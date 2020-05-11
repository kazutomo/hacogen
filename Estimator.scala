import Array._
import java.io._
import java.nio.ByteBuffer
import scala.collection.mutable.ListBuffer
import javax.imageio.ImageIO // png
import java.awt.image.BufferedImage // png

// local claases
import rawimagetool._
import localutil.Util._
import refcomp.RefComp._

class AppParams {
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
    println("-gray: dump gray images.")
    println("-png: dump png images.")
    println("-pngregion: dump png images.")
    println("-vsample xpos : dump vertical sample at xpos (default width/2)")
    println("* compressor configs")
    println("-ninpxs int : the number of input pixels")
    println("-nbufpxs int : the number of output buffer pixels")
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
  var bitspx = 9
  // NOTE: assume width and height >= 256. for now, fixed
  val window_width  = 256
  val window_height = 256
  var xoff = 0
  var yoff = 0
  // compressor params. non-adjustable for now
  var ninpxs = 16  // the input size to encoders, which is # of elements
  var nbufpxs = 28

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
      case "-ninpxs" :: istr :: tail => nextopts(tail, m ++ Map("ninpxs" -> istr ))
      case "-nbufpxs" :: istr :: tail => nextopts(tail, m ++ Map("nbufpxs" -> istr ))
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
    println("dataset: " + filename)
    println("width:   " + width)
    println("height:  " + height)
    println("size:    " + psize)
    //println(s"offset:  x=$xoff, y=$yoff")
    //println("")
    //println("ninpxs:  " + ninpxs)
    // println("nbufpxs: " + nbufpxs)
    //println("")
  }
}

object EstimatorMain extends App {


  var ap = new AppParams()

  ap.getopts(args)
  ap.printinfo()


  // open a dataset
  val in = new FileInputStream(ap.filename)
  val rawimg = new RawImageTool(ap.width, ap.height)

  val nshifts = (ap.height/ap.ninpxs) * ap.width

  val st = System.nanoTime()

  // need to skip frames

  for (fno <- 0 until ap.fnostart) rawimg.skipImage(in, ap.psize)
  if (ap.fnostart > 0) println(s"Skipped to ${ap.fnostart}")

  def optionalprocessing(fno: Int) {
    // write back to file.
    // To display an image, display -size $Wx$H -depth 16  imagefile
    if(ap.dump_gray)  {
      val grayfn = f"fr${fno}.gray"
      println(s"Writing $grayfn")
      rawimg.writeImageShort(grayfn)
    }
    if(ap.dump_png) {
      val pngfn = f"fr${fno}.png"
      println(s"Writing $pngfn")
      rawimg.writePng(pngfn, 1)
    }
    if(ap.dump_pngregion) {
      val pngfn = f"fr${fno}-${ap.window_width}x${ap.window_height}+${ap.xoff}+${ap.yoff}.png"
      println(s"Writing $pngfn")
      rawimg.writePngRegion(pngfn, 1, ap.xoff, ap.yoff,
        ap.window_width, ap.window_height);
    }

    if(ap.dump_vsample) {
      val vs = rawimg.getVerticalLineSample(ap.vsamplexpos, 0, ap.height)
      sys.exit(0)
    }
  }


  val hyd = ap.height - (ap.height % ap.ninpxs) // to simplify, ignore the remaining
  val total_inpxs = ap.width * hyd
  val total_shuffled_inpxs = ap.width * (hyd/ap.ninpxs*ap.bitspx)



  def analyzeframe() : Map[String, Float] = {
    // per-frame stats
    // enclens is created for each encode scheme
    // rl   : N-input run-length
    // zs   : N-input zero suppression
    // shzs : shuffled N-input zero suppression
    var enclens_rl   = new ListBuffer[Int]()
    var enclens_zs   = new ListBuffer[Int]()
    var enclens_shzs = new ListBuffer[Int]()

    // chunk up to rows whose height is ap.ninpxs
    for (yoff <- 0 until hyd by ap.ninpxs) {
      // each column shift (every cycle in HW)
      for (xoff <- 0 until ap.width) {
        // create a column chunk, which is an input to the compressor
        val indata = List.tabulate(ap.ninpxs)(
          rno =>
          rawimg.getpx(xoff, rno + yoff))

        // only check the number of pixel output
        enclens_rl   += rlEncode(indata).length
        enclens_zs   += zsEncode(indata, ap.bitspx).length
        if (ap.ninpxs > ap.bitspx)
          enclens_shzs += shzsEncode(indata, ap.bitspx).length
      }
    }

    val nrl = enclens_rl reduce(_+_)
    val nzs = enclens_zs reduce(_+_)

    val nshzs =
      if (ap.ninpxs > ap.bitspx) enclens_shzs reduce(_+_)
      else 1

    val ti = total_inpxs.toFloat
    val tsi = total_shuffled_inpxs.toFloat

    if (false) {
      printStats("RL", enclens_rl.toList.map(_.toFloat) )
      printStats("ZS", enclens_zs.toList.map(_.toFloat) )
      if (ap.ninpxs > ap.bitspx)
        printStats("SHZS", enclens_shzs.toList.map(_.toFloat) )
      println(f"RL  : ${total_inpxs}/${nrl} => ${ti/nrl}")
      println(f"ZS  : ${total_inpxs}/${nzs} => ${ti/nzs}")
      if (ap.ninpxs > ap.bitspx)
        println(f"SHZS: ${total_shuffled_inpxs}/${nshzs} => ${tsi/nshzs}")
    }

    // fix this hard-coded values later
    val b256nzs = calcNBufferedPixels(enclens_zs.toList, 28)
    val b256nshzs =
      if (ap.ninpxs > ap.bitspx)  calcNBufferedPixels(enclens_shzs.toList, 16)
      else 1
    val b512nzs = calcNBufferedPixels(enclens_zs.toList, 56)
    val b512nshzs =
      if (ap.ninpxs > ap.bitspx) calcNBufferedPixels(enclens_shzs.toList, 32)
      else 1

    if (false) {
      //println(f"B256RL  : ${total_inpxs}/${b256nrl} => ${ti/b256nrl}")
      println(f"B256ZS  : ${total_inpxs}/${b256nzs} => ${ti/b256nzs}")
      if (ap.ninpxs > ap.bitspx)
        println(f"B256SHZS: ${total_shuffled_inpxs}/${b256nshzs} => ${tsi/b256nshzs}")
      //println(f"B512RL  : ${total_inpxs}/${b512nrl} => ${ti/b512nrl}")
      println(f"B512ZS  : ${total_inpxs}/${b512nzs} => ${ti/b512nzs}")
      if (ap.ninpxs > ap.bitspx)
        println(f"B512SHZS: ${total_shuffled_inpxs}/${b512nshzs} => ${tsi/b512nshzs}")
    }

    val ret = Map(
      "RL" -> ti/nrl,
      "ZS" -> ti/nzs, "ZS256" -> ti/b256nzs,  "ZS512" -> ti/b512nzs
    )

    if (ap.ninpxs > ap.bitspx)
      ret ++ Map("SHZS" -> tsi/nshzs,
        "SHZS256" -> tsi/b256nshzs,
        "SHZS512" -> tsi/b512nshzs)
    else
      ret
  }


  // stats for entire dataset
  var allzeroratios = new ListBuffer[Float]()
  var allRLs = new ListBuffer[Float]()
  var allZSs = new ListBuffer[Float]()
  var allSHZSs = new ListBuffer[Float]()
  var allZS256s = new ListBuffer[Float]()
  var allSHZS256s = new ListBuffer[Float]()
  var allZS512s = new ListBuffer[Float]()
  var allSHZS512s = new ListBuffer[Float]()

  // per frame compression ratio
  var cr_rl = new ListBuffer[Int]()
  var maxzeroratio = 0.0.toFloat
  var minzeroratio = 1.0.toFloat
  var fno_maxzeroratio = 0 // skip 1.0 zeroratio frame
  var fno_minzeroratio = 0

  for (fno <- ap.fnostart to ap.fnostop) {
    if (ap.psize == 4) rawimg.readImageInt(in)
    else if (ap.psize == 1) rawimg.readImageByte(in)

    optionalprocessing(fno)

    val zeroratio = rawimg.zerocnt.toFloat / (ap.width*ap.height).toFloat
    if (zeroratio < 1.0.toFloat) {
      allzeroratios += zeroratio

      if (zeroratio > maxzeroratio) {
        fno_maxzeroratio = fno
        maxzeroratio = zeroratio
      }
      if (zeroratio < minzeroratio) {
        fno_minzeroratio = fno
        minzeroratio = zeroratio
      }
      val maxval = rawimg.maxval
      println(f"$fno%04d: zeroratio=$zeroratio%.3f maxval=$maxval")
      val crmap = analyzeframe()

      allRLs += crmap("RL")
      allZSs += crmap("ZS")
      if (ap.ninpxs > ap.bitspx)
        allSHZSs += crmap("SHZS")
      //allRL256s += crmap("RL256")
      allZS256s += crmap("ZS256")
      if (ap.ninpxs > ap.bitspx)
        allSHZS256s += crmap("SHZS256")
      //allRL512s += crmap("RL512")
      allZS512s += crmap("ZS512")
      if (ap.ninpxs > ap.bitspx)
        allSHZS512s += crmap("SHZS512")
    } else
      println(f"skip fno${fno} because all pixels are zero")
  }
  val et = System.nanoTime()
  val psec = (et-st)*1e-9

  println()
  printStats("zeroratio", allzeroratios.toList)
  println(f"maxzeroratio=$maxzeroratio@$fno_maxzeroratio")
  println(f"minzeroratio=$minzeroratio@$fno_minzeroratio")
  println()

  val npxs = ap.ninpxs
  printStats(f"RL$npxs",   allRLs.toList)
  printStats(f"ZS$npxs",   allZSs.toList)
  if (ap.ninpxs > ap.bitspx)
    printStats(f"SHZS$npxs", allSHZSs.toList)
  println()
  //printStats(f"RL$npxs-256b",   allRL256s.toList)
  printStats(f"ZS$npxs-256b",   allZS256s.toList)
  if (ap.ninpxs > ap.bitspx)
    printStats(f"SHZS$npxs-256b", allSHZS256s.toList)
  println()
  //printStats(f"RL$npxs-512b",   allRL512s.toList)
  printStats(f"ZS$npxs-512b",   allZS512s.toList)
  if (ap.ninpxs > ap.bitspx)
    printStats(f"SHZS$npxs-512b", allSHZS512s.toList)
  println()
  println()
  val nframes = ap.fnostop - ap.fnostart + 1
  println(f"Processing Time[Sec] = $psec%.3f total / ${psec/nframes.toFloat}%.3f per frame")
}
