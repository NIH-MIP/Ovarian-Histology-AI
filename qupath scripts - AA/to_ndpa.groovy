import groovy.xml.MarkupBuilder
import qupath.lib.scripting.QP
import qupath.lib.gui.tools.ColorToolsFX
import qupath.lib.geom.Point2
import qupath.lib.images.servers.ImageServer
import qupath.lib.common.GeneralTools
import qupath.lib.objects.PathAnnotationObject
import qupath.lib.objects.classes.PathClassFactory
import qupath.lib.roi.*

def write_ndpa() {
    def server = QP.getCurrentImageData().getServer()

    // We need the pixel size
    def cal = server.getPixelCalibration()
    if (!cal.hasPixelSizeMicrons()) {
        Dialogs.showMessageDialog("Metadata check", "No pixel information for this image!");
        return
    }

    // Here we get the pixel size
    def md = server.getMetadata()
    def pixelsPerMicron_X = 1 / cal.getPixelWidthMicrons() //md["pixelWidthMicrons"]
    def pixelsPerMicron_Y = 1 / cal.getPixelHeightMicrons() //md["pixelHeightMicrons"]

    //Aperio Image Scope displays images in a different orientation
    //TODO Is this in the metadatata? Is this likely to be a problem?
    //print(server.dumpMetadata())
    def rotated = false

    def h = server.getHeight()
    def w = server.getWidth()
    def ImageCenter_X = (w/2)*1000/pixelsPerMicron_X
    def ImageCenter_Y = (h/2)*1000/pixelsPerMicron_Y 

    // need to add annotations to hierarchy so qupath sees them
    def hierarchy = QP.getCurrentHierarchy()
        
    //Get X Reference from OPENSLIDE data
    //The Open slide numbers are actually offset from IMAGE center (not physical slide center). 
    //This is annoying, but you can calculate the value you need -- Offset from top left in Nanometers. 

    def map = getCurrentImageData().getServer().osr.getProperties()
    map.each { k, v ->
        if(k.equals("hamamatsu.XOffsetFromSlideCentre")){
            OffSet_From_Image_Center_X = v
            //print OffSet_From_Image_Center_X
            //print ImageCenter_X
            OffSet_From_Top_Left_X = ImageCenter_X.toDouble() - OffSet_From_Image_Center_X.toDouble()
            X_Reference =  OffSet_From_Top_Left_X
            //print X_Reference
            }
        if(k.equals("hamamatsu.YOffsetFromSlideCentre")){
            OffSet_From_Image_Center_Y = v
            //print    OffSet_From_Image_Center_Y
            //print ImageCenter_Y

            OffSet_From_Top_Left_Y = ImageCenter_Y.toDouble() - OffSet_From_Image_Center_Y.toDouble() 
            Y_Reference =  OffSet_From_Top_Left_Y
            //print Y_Reference
            }
    }

    def writer = new StringWriter()
    def xml = new MarkupBuilder(writer)
    def pathObjects = hierarchy.getAnnotationObjects()

    xml.mkp.xmlDeclaration(version: "1.0", encoding: "utf-8", standalone: "yes")
    xml.annotations {
        pathObjects.eachWithIndex { pathObject, index ->
            ndpviewstate('id' : index+1) {
                title(pathObject.getName())
                details(pathObject.getPathClass())
                coordformat('nanometers')
                lens(0.5)
                x(ImageCenter_X.toInteger())
                y(ImageCenter_Y.toInteger())
                z(0) //FIXME should be link to a z parameter
                showtitle(0)
                showhistogram(0)
                showlineprofile(0)

                //Annotation object
                annotation(type:'freehand',  //FIXME here we should use switch/case to associate the right variables here
                        displayname:'AnnotatedFreehand', 
                        color:'#' + Integer.toHexString(ColorToolsFX.getDisplayedColorARGB(pathObject)).substring(2)) {
                    measuretype(0)
                    closed(1) //FIXME polygon vs polyline
                    pointlist {
                        pathObject.getROI().getAllPoints().each { pt ->
                            point {
                                x(((pt.getX() - X_Reference / 1000 * pixelsPerMicron_X) * 1000 / pixelsPerMicron_X).toInteger())
                                y(((pt.getY() - Y_Reference / 1000 * pixelsPerMicron_Y) * 1000 / pixelsPerMicron_Y).toInteger())
                            }
                        }
                    }
                }
            }
        }
    }

    //an NDPA file simply adds '.ndpa' ti the NDPI filename
    def path = GeneralTools.toPath(server.getURIs()[0]).toString()+".ndpa";
    def NDPAfile = new File(path)
    NDPAfile.write(writer.toString())
}

write_ndpa()