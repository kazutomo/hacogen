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
  // NOTE: assume width and height >= 256. for now, fixed
  val window_width  = 256
  val window_height = 256
  var xoff = 0
  var yoff = 0
  // compressor params. non-adjustable for now
  var npxbits = 9  // 9 bits per pixels
  val ninpxs = 16  // the input size to encoders, which is # of elements

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
    // fix the followings later!. not elegant
    if (m contains "fnostart") fnostart = getIntVal(m, "fnostart")
    if (m contains "fnostop")  fnostop = getIntVal(m, "fnostop")
    if (m contains "xoff") xoff = getIntVal(m, "xoff")
    if (m contains "yoff") yoff = getIntVal(m, "yoff")
    if (m contains "gray") dump_gray = getBoolVal(m, "gray")
    if (m contains "png") dump_png = getBoolVal(m, "png")
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
    println(s"offset:  x=$xoff, y=$yoff")
    println("")
  }
}

object EstimatorMain extends App {

  // software-based encoding for validation
  def encoding(px: List[Int]) : List[Int] = {
    val headerlist = List.tabulate(px.length)(i => if (px(i) == 0) 0 else 1<<i)
    val header = headerlist.reduce(_ + _) // | (1 << (c.elemsize-1))
    val nonzero = px.filter(x => x > 0)
    return List.tabulate(nonzero.length+1)(i => if(i==0) header else nonzero(i-1) )
  }

  var ap = new AppParams()

  ap.getopts(args)
  ap.printinfo()

  //
  var allratios28 = new ListBuffer[Float]()
  var allratios56 = new ListBuffer[Float]()
  var allzeroratios = new ListBuffer[Float]()

  // open a dataset
  val in = new FileInputStream(ap.filename)
  val rawimg = new RawImageTool(ap.width, ap.height)

  val nshifts = (ap.height/ap.ninpxs) * ap.width

  val st = System.nanoTime()

  // need to skip frames
  println(s"Skipping to ${ap.fnostart}")
  for (fno <- 0 until ap.fnostart) {
    rawimg.skipImage(in, ap.psize)
  }

  for (fno <- ap.fnostart to ap.fnostop) {
    if (ap.psize == 4) rawimg.readImageInt(in)
    else if (ap.psize == 1) rawimg.readImageByte(in)

    val zeroratio = rawimg.zerocnt.toFloat / (ap.width*ap.height).toFloat
    allzeroratios += zeroratio

    val maxval = rawimg.maxval
    println(f"$fno%04d: zeroratio=$zeroratio%.3f maxval=$maxval")

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
      val dumptext = false
      val bitspx = 9
      val npxblock = 16

      // for compression ratio stats
      var rlcrs = new ListBuffer[Float]()
      var zscrs = new ListBuffer[Float]()
      var zs2crs = new ListBuffer[Float]()

      for (vsx <- ap.xoff until ap.xoff + ap.window_width) {
        val vsy1 = ap.yoff
        val vsy2 = ap.yoff + ap.window_height
        val vs0 = rawimg.getVerticalLineSample(vsx, vsy1, vsy2)

        //val vs = rawimg.clipSample(vs0, bitspx)
        val vs = vs0
        if(dumptext) {
          val vsfn = f"vsample-x${vsx}y${vsy1}until${vsy2}-fr${fno}.txt"
          println(s"Writing $vsfn")
          writeList2File(vsfn, vs)
        }
        val rl = runlengthEncoding(vs)
        val zs = zsEncoding(vs, npxblock, 2)
        val rlcr = vs.length.toFloat/rl.length
        val zscr = vs.length.toFloat/zs.length
        rlcrs += rlcr
        zscrs += zscr

        //println(s"RL: ${rl.length} => ${rlcr}x")
        //println(s"ZS: ${zs.length} => ${zscr}x")

        val vsbs = bitshuffleVerticalLineSample(vs, npxblock, bitspx)
        val vsbsfn = f"vsample-${bitspx}bitshuffle${npxblock}-x${vsx}y${vsy1}until${vsy2}-fr${fno}.txt"
        val zs2 = zsEncoding(vsbs, npxblock, 1)
        val zs2cr = (vs.length.toFloat/npxblock*bitspx) /zs2.length
        zs2crs += zs2cr
        //println(s"ZSS: ${zs2.length} => ${zs2cr}x")
        if(dumptext) {
          println(s"Writing $vsbsfn")
          writeList2File(vsbsfn, vsbs)
        }
      }

      printstats("RL ", rlcrs.toList)
      printstats("ZS ", zscrs.toList)
      printstats("ZS2", zs2crs.toList)

      sys.exit(0)
    }

    // enclens is created for each frames
    var enclens = new ListBuffer[Int]()

    val hyd = ap.height - (ap.height % ap.ninpxs) // to simplify, ignore the remaining

    // chunk up to rows whose height is yd
    for (yoff <- 0 until hyd by ap.ninpxs) {
      // emulate pixel shift
      for (xoff <- 0 until ap.width) {
        // create a column chunk, which is an input to the compressor
        val dtmp = List.tabulate(ap.ninpxs)(
          rno =>
          rawimg.getpx(xoff, rno + yoff))

        val enctmp = encoding(dtmp)
        enclens += enctmp.length
      }
    }

    def estimate_ratios(noutpixs: Int) : Float = {
      var tmp_ratios = new ListBuffer[Float]()
      var npxs_used = 0
      var input_cnt = 0

      if (noutpixs == 0) {
        for (l <- enclens) {
          tmp_ratios += (ap.ninpxs.toFloat / l.toFloat)
        }
      } else {
        for (l <- enclens) {
          if (npxs_used + l < noutpixs) {
            npxs_used += l
            input_cnt += ap.ninpxs
          } else {
            tmp_ratios += (input_cnt.toFloat / noutpixs.toFloat)
            npxs_used = l
            input_cnt = ap.ninpxs
          }
        }
        tmp_ratios += (input_cnt.toFloat / noutpixs.toFloat)
      }

      val rtmp = tmp_ratios.toList
      if (rtmp.length < 1) return 0.0.toFloat

      val rmean = (rtmp.reduce( (a,b) => (a + b) )) / rtmp.length.toFloat
      val rmax = rtmp.reduce( (a,b) => (a max b) )
      val rmin = rtmp.reduce( (a,b) => (a min b) )
      var noncompressedcnt = 0
      rtmp.foreach( e => if(e<1.0) noncompressedcnt += 1)
      val ncperc = noncompressedcnt.toFloat * 100.0 / (ap.width*ap.height).toFloat

      println(f"$fno%04d: mean=$rmean%.2f max=$rmax%.2f min=$rmin%.2f non=$noncompressedcnt/$nshifts ($noutpixs%2d I/O pixels)")

      rmean
    }
    allratios28 += estimate_ratios(28)
    allratios56 += estimate_ratios(56)
  }

  val et = System.nanoTime()
  val psec = (et-st)*1e-9

  println()
  println("-" * 60)
  printstats("zeroratio", allzeroratios.toList)
  printstats("cr28",      allratios28.toList)
  printstats("cr56",      allratios56.toList)
  println("-" * 60)
  println()

  println(f"Processing Time[Sec] = $psec%.3f")
}
