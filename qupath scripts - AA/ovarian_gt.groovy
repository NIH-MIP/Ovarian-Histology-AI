def gson = GsonTools.getInstance(true)

def imageData = getCurrentImageData()
def name = GeneralTools.getNameWithoutExtension(imageData.getServer().getMetadata().getName())
print name


def json = new File("/project_path/all_jsons_noID_withMargin_noDuplicates/" + name + ".json").text


// Read the annotations
def type = new com.google.gson.reflect.TypeToken<List<qupath.lib.objects.PathObject>>() {}.getType()
def deserializedAnnotations = gson.fromJson(json, type)
addObjects(deserializedAnnotations)
