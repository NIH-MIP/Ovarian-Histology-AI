from pathlib import Path
import sys
import os
import openslide
import xml.etree.ElementTree as ET
import geojson
from shapely.geometry import Polygon, Point, MultiPoint, MultiPolygon, shape
import numpy as np
import argparse

def generic_regionAttrib():
    # ImageScope has this for all layers... we should initialize it but do not need to add info
    # add/create attributes and region attribute header in all layers
    reg = ET.Element('RegionAttributeHeader')
    attrib = ET.Element('AttributeHeader', attrib={'Id': '9999', 'Name': 'Region', 'ColumnWidth': '-1'})
    reg.append(attrib)
    attrib = ET.Element('AttributeHeader', attrib={'Id': '9997', 'Name': 'Length', 'ColumnWidth': '-1'})
    reg.append(attrib)
    attrib = ET.Element('AttributeHeader', attrib={'Id': '9996', 'Name': 'Area', 'ColumnWidth': '-1'})
    reg.append(attrib)
    attrib = ET.Element('AttributeHeader', attrib={'Id': '9998', 'Name': 'Text', 'ColumnWidth': '-1'})
    reg.append(attrib)
    attrib = ET.Element('AttributeHeader', attrib={'Id': '1', 'Name': 'Description', 'ColumnWidth': '-1'})
    reg.append(attrib)

    return reg

def get_imgProperties(wsipath):
    # img = openslide.OpenSlide(wsipath)
    # create base
    a = ET.Element('Annotations')
    # a.set("MicronsPerPixel", img.properties[openslide.PROPERTY_NAME_MPP_X])

    a.set("MicronsPerPixel", '0.2212') # Since I know MPP for images in my batches, I hard-code it to speed up the script

    return a

def read_geojson(jsonpath):
    with open(jsonpath) as f:
        allobjects = geojson.load(f)

    #allshapes = [shape(obj["geometry"]) for obj in allobjects]
    allshapes = [obj["geometry"]['coordinates'] for obj in allobjects]
    # allshapes = [[[list(map(int, lst)) for lst in shapes] for shapes in shapes1] for shapes1 in allshapes] # convert nested listed list items to int
    alllabels = [obj['properties'] for obj in allobjects]
    roilabels = list()
    for roi_num in range(0, len(alllabels)):
        roi_label = alllabels[roi_num]['classification']['name']
        roilabels.append(roi_label)
    
    return allshapes, roilabels

def build_layer(classname,classregions,classcolor,classnum):
    b = ET.Element('Annotation', attrib={'Id': str(classnum), 'Name': classname, 'ReadOnly': '0', 'NameReadOnly': '0',
                                               'LineColorReadOnly': '0', 'Incremental': '0', 'Type': '4',
                                               'LineColor': classcolor, 'Selected': '0', 'MarkupImagePath': '',
                                               'MacroName': ''})
    bb = ET.Element('Regions')
    b_reg = generic_regionAttrib()
    bb.append(b_reg)
    for _i in range(0,len(classregions)):
        regi = classregions[_i]
        b_regi = build_region(verts=regi[0],idx=_i+1)
        bb.append(b_regi)
    b.append(bb)
    return b
    
def build_region(verts,idx):
    # regions are listed by vertices
    e = ET.Element('Region', attrib={'Selected': '0', 'Type': '0', 'Id': str(idx), 'DisplayId': str(idx), 'Analyze': '1',
                                            'InputRegionId': '0', 'NegativeROA': '0', 'Text': ''})
    vreg = ET.Element('Vertices')
    for vpair in verts:
        #print(str(vpair[0])+','+str(vpair[1]))
        v = ET.Element('Vertex', attrib={'Z': '0', 'X': str(vpair[0]), 'Y': str(vpair[1])})
        vreg.append(v)
    e.append(vreg)
    return e


json_dir = Path('/Users/arlovaa2/Documents/ovarian project/galact_NIH/gal_NIH_merged_segm_bbox_jsons/')

img_dir = Path('/Volumes/AIR/')

save_dir = '/Users/arlovaa2/Documents/ovarian project/galact_NIH/gal_NIH_merged_segm_bbox_jsons/'

color_pal = {'Follicle':'0'}

filelist = [f.stem for f in json_dir.iterdir() if f.suffix == '.json']


# read json
for f in filelist:

    json_file = str(json_dir) + '/' + f + '.json'

    allshapes, roilabels = read_geojson(jsonpath=json_file)
    roilabels = ['Follicle' for f in roilabels]

    # we need the wsi to get the microns per pixel and initilize xml
    # slide = str(img_dir) + '/' + f[:-7] + '.svs'
    slide = str(img_dir) + '/' + f + '.svs'
    a = get_imgProperties(slide)

    # for each tumor type (based on geojson classification label) we build xml layer
    for roi_num in range(0,len(np.unique(roilabels))):
        roi_class = np.unique(roilabels)[roi_num]
        print(roi_class)
        class_color = color_pal[roi_class]
        classrois = [allshapes[roi] for roi in range(0,len(allshapes)) if roilabels[roi]==roi_class]
        class_layer = build_layer(classname=roi_class,classregions=classrois,classcolor=class_color,classnum=roi_num+1)
        a.append(class_layer)

    # define save_name for output file
    save_name = save_dir + f + '.xml'

    #dump it to xml file
    out = open(save_name, 'w')
    print(ET.tostring(a).decode(), file=out)
    out.close()