import qupath.lib.images.servers.LabeledImageServer

def imageData = getCurrentImageData()

// Define output path (relative to project)
def name = GeneralTools.getNameWithoutExtension(imageData.getServer().getMetadata().getName())
print name
def pathOutput = buildFilePath(PROJECT_BASE_DIR, 'tiles_20x_nonorm', name)
mkdirs(pathOutput)

// Define output resolution
double requestedPixelSize = 0.5
//double requestedPixelSize = imageData.getServer().getPixelCalibration().getAveragedPixelSize()
print requestedPixelSize

// Convert to downsample
double downsample = requestedPixelSize / imageData.getServer().getPixelCalibration().getAveragedPixelSize()
print downsample

// Create an ImageServer where the pixels are derived from annotations
def labelServer = new LabeledImageServer.Builder(imageData)
    .backgroundLabel(0, ColorTools.BLACK) // Specify background label (usually 0 or 255)
    .downsample(downsample)    // Choose server resolution; this should match the resolution at which tiles are exported
//    .useInstanceLabels()
//    .addLabel('Ovulated Antral Follicle',1)// Choose output labels (the order matters!)
//    .addLabel('Antral',2)
//    .addLabel('Corpus Luteum', 3)
//    .addLabel('Large Antral', 4)
//    .addLabel('Intermediate Antral', 5)
//    .addLabel('Small Antral', 6)
//    .addLabel('Secondary', 7)
//    .addLabel('Secondary No Nuclei',8)
//    .addLabel('Multi-oocytic',9)
//    .addLabel('Multilayer No Oocyte', 10)
//    .addLabel('NOS',11)
//    .addLabel('Transforming Primary', 12)
//    .addLabel('Primary',13)
//    .addLabel('Transitional',14)
//    .addLabel('Primordial', 15)
    .addLabel('Margin', 0)
    .addLabel('Ovulatory Antral', 1)
    .addLabel('Dominance Antral',2)
    .addLabel('Ovulated Antral Follicle',3)// Choose output labels (the order matters!)
    .addLabel('Antral',4)
    .addLabel('Corpus Luteum', 5)
    .addLabel('Large Antral', 6)
    .addLabel('Selection Antral', 7)
    .addLabel('Pre-Selection Antral', 8)
    .addLabel('Intermediate Antral', 9)
    .addLabel('Small Antral', 10)
    .addLabel('Early Antral',11)
    .addLabel('Atretic Antral with NM',12)
    .addLabel('Atretic Antral with AM',13)
    .addLabel('Secondary', 14)
    .addLabel('Secondary No Nuclei',15)
    .addLabel('Multi-oocytic',16)
    .addLabel('Multilayer', 17)
    .addLabel('NOS',18)
    .addLabel('do not count',19)
    .addLabel('AMF-O', 20)
    .addLabel('AMF-G', 21)
    .addLabel('AMF-OG', 22)
    .addLabel('Transforming Primary', 23)
    .addLabel('Transitional Primary', 24)
    .addLabel('Primary',25)
    .addLabel('Transitional',26)
    .addLabel('Transitional Primordial',27)
    .addLabel('Primordial', 28)


    .multichannelOutput(false)  // If true, each label is a different channel (required for multiclass probability)
    .build()

// Create an exporter that requests corresponding tiles from the original & labeled image servers
new TileExporter(imageData)
    .downsample(downsample)     // Define export resolution
    .imageExtension('.jpg')     // Define file extension for original pixels (often .tif, .jpg, '.png' or '.ome.tif')
    .tileSize(500)              // Define size of each tile, in pixels
    .labeledServer(labelServer) // Define the labeled image server to use (i.e. the one we just built)
    
    .annotatedTilesOnly(true)  // If true, only export tiles if there is a (labeled) annotation present
    .overlap(50)                // Define overlap, in pixel units at the export resolution
    .writeTiles(pathOutput)     // Write tiles to the specified directory

print 'Done!'