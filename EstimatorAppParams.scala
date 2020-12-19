package estimator

object EstimatorAppParams {
  def usage() {
    println("Usage: scala EstimatorMain [options] rawimgfilename")
    println("")
    println("This command reads a simple raw image format that contains a number of glay scala images with 32-bit integer pixel value (its dynamic range is possible smaller). Width and Height are specified via command line. The image framenumber start and stop are also specified via command line.")
    println("")
    println("[Options]")
    println("")
    println("-width int  : width of each image frame")
    println("-height int : height of each image frame")
    println("-psize int  : bytes per pixel: 1 or 4")
    println(s"-fnostart int : start frame number (default: $fnostart)")
    println(s"-fnostop int  : stop frame number (default: $fnostop)")
    println("* compressor configs")
    println("-nrowbundles int : the number of the row bundles")
    println("-ncolshifts int : the number of the pixels per readout")
    println("-ninpxs int : the number of input pixels (depreciated)")
    println("-nbufpxs int : the number of output buffer pixels")
    println("* dump options")
    println("-gray: dump gray images")
    println("-png: dump png images")
    println("-pngregion: dump png images")
    println("-vsample xpos : dump vertical sample at xpos (default width/2)")
    println("")
  }

  var filename = ""
  var width = 256
  var height = 256
  var psize = 4
  var fnostart = 0
  var fnostop = 0
  var dump_gray = false
  var dump_png = false
  var dump_pngregion = false
  var dump_vsample = false
  var vsamplexpos = 0
  var bitspx = 10
  // NOTE: assume width and height >= 256. for now, fixed
  val window_width  = 128
  val window_height = 128
  var xoff = 0
  var yoff = 0
  // compressor params. non-adjustable for now
  var nrowbundles = 8 // only works with V2(the current version)
  var ncolshifts = 8 // only works with V2

  var ninpxs = 16  // the input size to encoders, which is # of elements  (deprecated)
  var nbufpxs = 28  // (deprecated)


  type AppOpts = Map[String, String]

  // command line option handling
  def nextopts(l: List[String], m: AppOpts) : AppOpts = {
    l match {
      case Nil => m
      case "-h" :: tail => usage() ; sys.exit(1)
      case "-gray" :: istr :: tail => nextopts(tail, m ++ Map("gray" -> istr ))
      case "-png" :: istr :: tail => nextopts(tail, m ++ Map("png" -> istr ))
      case "-pngregion" :: istr :: tail => nextopts(tail, m ++ Map("pngregion" -> istr ))
      case "-vsample" :: istr :: tail => nextopts(tail, m ++ Map("vsample" -> istr ))
      case "-width" :: istr :: tail => nextopts(tail, m ++ Map("width" -> istr ))
      case "-height" :: istr :: tail => nextopts(tail, m ++ Map("height" -> istr ))
      case "-psize" :: istr :: tail => nextopts(tail, m ++ Map("psize" -> istr ))
      case "-fnostart" :: istr :: tail => nextopts(tail, m ++ Map("fnostart" -> istr ))
      case "-fnostop" :: istr :: tail => nextopts(tail, m ++ Map("fnostop" -> istr ))
      case "-xoff" :: istr :: tail => nextopts(tail, m ++ Map("xoff" -> istr ))
      case "-yoff" :: istr :: tail => nextopts(tail, m ++ Map("yoff" -> istr ))
      case "-nrowbundles" :: istr :: tail => nextopts(tail, m ++ Map("nrowbundles" -> istr ))
      case "-mcolshifts" :: istr :: tail => nextopts(tail, m ++ Map("ncolshifts" -> istr ))
      case "-ninpxs" :: istr :: tail => nextopts(tail, m ++ Map("ninpxs" -> istr )) // deprecated
      case "-nbufpxs" :: istr :: tail => nextopts(tail, m ++ Map("nbufpxs" -> istr )) // deprecated
      case str :: Nil => m ++ Map("filename" -> str)
      case unknown => {
        println("Unknown: " + unknown)
        sys.exit(1)
      }
    }
  }

  def getopts(a: Array[String]) {
    val m = nextopts(a.toList, Map())

    filename = m.get("filename") match {
      case Some(v) => v
      case None => println("No filename found"); usage(); sys.exit(1)
    }
    def getIntVal(m: AppOpts, k: String) = m.get(k) match {case Some(v) => v.toInt ; case None => println("No value found: " + k); sys.exit(1)}

    def getBoolVal(m: AppOpts, k: String) = m.get(k) match {case Some(v) => v.toBoolean ; case None => println("No value found: " + k); sys.exit(1)}

    width = getIntVal(m, "width")
    height = getIntVal(m, "height")
    psize = getIntVal(m, "psize")

    // the following lines need to be fixed. not elegant...
    if (m contains "fnostart") fnostart = getIntVal(m, "fnostart")
    if (m contains "fnostop")  fnostop = getIntVal(m, "fnostop")
    if (m contains "xoff")     xoff = getIntVal(m, "xoff")
    if (m contains "yoff")     yoff = getIntVal(m, "yoff")
    if (m contains "ninpxs")   ninpxs = getIntVal(m, "ninpxs")
    if (m contains "nbufpxs")  nbufpxs = getIntVal(m, "nbufpxs")
    if (m contains "gray")     dump_gray = getBoolVal(m, "gray")
    if (m contains "png")      dump_png = getBoolVal(m, "png")
    if (m contains "pngregion")   dump_pngregion = getBoolVal(m, "pngregion")

    m.get("vsample") match {
      case Some(v) => dump_vsample = true; vsamplexpos = v.toInt
      case None => println("vsample needs value")
    }
  }

  def printinfo() = {
    println("[Info]")
    println("dataset:     " + filename)
    println("width:       " + width)
    println("height:      " + height)
    println("size:        " + psize)
    println("nrowbundles: " + nrowbundles)
    //println("ncolshifts:  " + ncolshifts) // we don't need to change this
    println(s"offset:      x=$xoff, y=$yoff")
    //println("ninpxs:  " + ninpxs + " (deprecated)")
    println("")
  }
}
