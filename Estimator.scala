//package rawdata

import java.io._
import java.nio.ByteBuffer
import Array._
import scala.collection.mutable.ListBuffer

// A class for manipulating a simple raw image data set that does not
// include metadata information such as width, height, pixel data
// type, the number of image, etc. This class provides methods that
// loads and stores the raw image and methods that analyzes data
// (maximum value, zero pixel count, etc).
//

class RawImageDataSet(val width: Int, val height: Int)
{
  // image pixel data
  private var pixels = ofDim[Int](height, width) // index order; y -> x
  // helper function
  def setpx(x: Int, y: Int, v: Int) : Unit = pixels(y)(x) = v
  def getpx(x: Int, y: Int) : Int = pixels(y)(x)

  // statistical info
  var maxval = 0
  var zerocnt = 0

  def resetpixels(v: Int = 0) : Unit = {
    for(y <- 0 until height; x <- 0 until width) setpx(x, y, v)
    maxval = 0
    zerocnt = 0
  }

  def BytesToInt(buf: Array[Byte]) : Int = {
    val ret = ByteBuffer.wrap(buf.reverse).getInt
    /*
    if(buf(3) != 0 ||buf(2) != 0 ||buf(1) != 0) {
      println(f"${buf(3)}:${buf(2)}:${buf(1)}:${buf(0)} => $ret")
    }
     */
    ret
  }

  def ShortToBytes(v: Short): Array[Byte] = {
    val tmp = new Array[Byte](2)

    tmp(0) = v.toByte
    tmp(1) = (v >> 8).toByte

    tmp
  }

  def skipImageInt(in: FileInputStream) {
    val step = 4
    in.skip((width*height)*step)
  }
  def skipImageByte(in: FileInputStream) {
    val step = 1
    in.skip((width*height)*step)
  }


  // read an image with 4-byte integer pixels (4 * w * h bytes) and
  // store it to pixels.  maxval and zerocnt are updated
  def readImageInt(in: FileInputStream) {
    resetpixels()

    val step = 4
    try {
      val buf = new Array[Byte]( (width*height)*step )

      in.read(buf)

      for (y <- 0 until height) {
        for (x <- 0 until width) {
          var idx = y * width + x
          val v = BytesToInt(buf.slice(idx*step, idx*step+4))


          // NOTE: take only positive values
          val clipval=(1<<16)-1
          //val v2 = if (v < 0) {0} else {if (v>=clipval) clipval else v}
          val v2 = v.abs

          if (v2 == 0) zerocnt += 1
          if (v2 > maxval) { maxval = v2 }
          setpx(x, y, v2)
        }
      }
    } catch {
      case e: IOException => println("Failed to read")
    }
  }

  // read an image with 1-byte integer pixels (w * h bytes) and
  // store it to pixels.  maxval and zerocnt are updated
  def readImageByte(in: FileInputStream) {
    resetpixels()

    try {
      val buf = new Array[Byte](width*height)
      in.read(buf)
      for (y <- 0 until height) {
        for (x <- 0 until width) {
          var idx = y * width + x
          val v = buf(idx).toInt
          if (v == 0) zerocnt += 1
          if (v > maxval) { maxval = v }
          setpx(x, y, v)
        }
      }
    } catch {
      case e: IOException => println("Failed to read")
    }
  }


  // implement this later when needed
  // def writeImageInt(fn: String, w: Int, h: Int) : Boolean

  // write the current image into a file for a debug purpose
  def writeImageShort(fn: String) : Boolean = {
    val step = 2
    var buf = new Array[Byte]((width*height) * step)

    for (y <- 0 until height; x <- 0 until width ) {
      val v = getpx(x, y)
      //val sval : Short = if (v < 0) 0 else v.toShort
      val sval : Short = if (v <= 0) 0 else 1
      val tmp = ShortToBytes(sval)
      val idx = y*width + x

      buf(idx*2 + 0) = tmp(0)
      buf(idx*2 + 1) = tmp(1)
    }

    try {
      val out = new FileOutputStream(fn)
      out.write(buf)
      out.close()
      println("Stored to " + fn)
    } catch {
      case e: FileNotFoundException => println("Not found:" + fn)
      case e: IOException => println("Failed to write")
    }

    true
  }

  // ad-hoc dump utility
  def writeVerticalLineSamples(fn: String, x: Int) {
    try {
      val f = new File(fn)
      val out = new BufferedWriter(new FileWriter(f))

      println("Stored samples to " + fn)
      for (y <- 0 until height) {
        val v = getpx(x, y)
        out.write(f"$v\n")
      }
      out.close()
    } catch {
      case e: FileNotFoundException => println("Not found:" + fn)
      case e: IOException => println("Failed to write")
    }
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

  def printmemoryusage : Unit = {
    val r = Runtime.getRuntime
    println( "Free(MB) : " + (r.freeMemory >> 20) )
    println( "Total(MB): " + (r.totalMemory >> 20) )
  }

  def stddev(x: List[Float]) : Float = {
    val m = x.sum / x.size
    val sq = x map(v => (v-m)*(v-m))
    math.sqrt( (sq.sum / x.size).toDouble ).toFloat
  }

  class Params {
    var width = 256
    var height = 256
    var fnostart = 0
    var fnostop = 0
    var dump_gray = false
    var dump_png = false
    var dump_vsample = false
    var vsamplepos = 0
    // NOTE: assume width and height >= 256. for now, fixed
    val window_width  = 256
    val window_height = 256
    var xoff = 0
    var yoff = 0
    // compressor params. non-adjustable for now
    var npxbits = 9      // 9 bits per pixels

    def autoxyoff() = {
      xoff = (width/2)  - (window_width/2)
      yoff = (height/2) - (window_width/2)
    }
  }

  var p = new Params()

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
    println("-vsample xpos : dump vertical sample at xpos (default width/2)")
    println("")
  }

  type AppOpts = Map[String, String]

  // command line option handling
  def nextopts(l: List[String], m: AppOpts) : AppOpts = {
    l match {
      case Nil => m
      case "-h" :: tail => usage() ; sys.exit(1)
      case "-gray" :: istr :: tail => nextopts(tail, m ++ Map("gray" -> istr ))
      case "-png" :: istr :: tail => nextopts(tail, m ++ Map("png" -> istr ))
      case "-vsample" :: istr :: tail => nextopts(tail, m ++ Map("vsample" -> istr ))
      case "-width" :: istr :: tail => nextopts(tail, m ++ Map("width" -> istr ))
      case "-height" :: istr :: tail => nextopts(tail, m ++ Map("height" -> istr ))
      case "-psize" :: istr :: tail => nextopts(tail, m ++ Map("psize" -> istr ))
      case "-fnostart" :: istr :: tail => nextopts(tail, m ++ Map("fnostart" -> istr ))
      case "-fnostop" :: istr :: tail => nextopts(tail, m ++ Map("fnostop" -> istr ))
      case str :: Nil => m ++ Map("filename" -> str)
      case unknown => {
        println("Unknown: " + unknown)
        sys.exit(1)
      }
    }
  }

  def getIntVal(m: AppOpts, k: String) = m.get(k) match {case Some(v) => v.toInt ; case None => println("No value found: " + k); sys.exit(1)}
  def getStrVal(m: AppOpts, k: String) = m.get(k) match {case Some(v) => v ; case None => println("No value found: " + k); sys.exit(1)}
  def getBoolVal(m: AppOpts, k: String) = m.get(k) match {case Some(v) => v.toBoolean ; case None => println("No value found: " + k); sys.exit(1)}

  val m = nextopts(args.toList, Map())

  val fn = getStrVal(m, "filename")
  val w = getIntVal(m, "width")
  val h = getIntVal(m, "height")
  val sz = getIntVal(m, "psize")
  val fnostart = getIntVal(m, "fnostart")
  val fnostop = getIntVal(m, "fnostop")
  val dumpflag = getBoolVal(m, "gray")

  val yd = 16  // the size input to the encoder

  //var allratios0  = new ListBuffer[Float]()
  //var allratios16 = new ListBuffer[Float]()
  //var allratios18 = new ListBuffer[Float]()
  var allratios28 = new ListBuffer[Float]()
  var allratios56 = new ListBuffer[Float]()
  var allzeroratios = new ListBuffer[Float]()

  // open a dataset
  val in = new FileInputStream(fn)
  val rawimg = new RawImageDataSet(w, h)

  println("[Info]")
  println("dataset: " + fn)
  println("width:   " + rawimg.width)
  println("height:  " + rawimg.height)
  println("size:    " + sz)
  println("")

  if (! Seq(1,4).contains(sz)) {
    println("Error: invalid size: " + sz)
    System.exit(1)
  }

  val nshifts = (h/yd) * w

  val st = System.nanoTime()
  // need to skip frames
  for (fno <- 0 until fnostart) {
    if (sz == 4) rawimg.skipImageInt(in)
    else if (sz == 1) rawimg.skipImageByte(in)
    println(s"Skipping $fno")
  }

  for (fno <- fnostart to fnostop) {
    if (sz == 4) rawimg.readImageInt(in)
    else if (sz == 1) rawimg.readImageByte(in)

    val zeroratio = rawimg.zerocnt.toFloat / (w*h).toFloat
    allzeroratios += zeroratio

    val maxval = rawimg.maxval
    println(f"$fno%04d: zeroratio=$zeroratio%.3f maxval=$maxval")

    // write back to file.
    // To display an image, display -size $Wx$H -depth 16  imagefile
    if (dumpflag)   {
      rawimg.writeImageShort(f"fr$fno.gray")
      val sampleatx=(w/2)+1
      rawimg.writeVerticalLineSamples(f"vsample-x$sampleatx-fr$fno.txt", sampleatx)
    }

    // enclens is created for each frames
    var enclens = new ListBuffer[Int]()

    val hyd = h - (h % yd) // to simplify, ignore the remaining

    // chunk up to rows whose height is yd
    for (yoff <- 0 until hyd by yd) {
      // emulate pixel shift
      for (xoff <- 0 until w) {
        // create a column chunk, which is an input to the compressor
        val dtmp = List.tabulate(yd)(
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
          tmp_ratios += (yd.toFloat / l.toFloat)
        }
      } else {
        for (l <- enclens) {
          if (npxs_used + l < noutpixs) {
            npxs_used += l
            input_cnt += yd
          } else {
            tmp_ratios += (input_cnt.toFloat / noutpixs.toFloat)
            npxs_used = l
            input_cnt = yd
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
      val ncperc = noncompressedcnt.toFloat * 100.0 / (w*h).toFloat

      println(f"$fno%04d: mean=$rmean%.2f max=$rmax%.2f min=$rmin%.2f non=$noncompressedcnt/$nshifts ($noutpixs%2d I/O pixels)")

      rmean
    }

    //allratios0  += estimate_ratios(0) // ideal case
    //allratios16 += estimate_ratios(16)
    //allratios18 += estimate_ratios(18)
    allratios28 += estimate_ratios(28)
    allratios56 += estimate_ratios(56)
  }

  val et = System.nanoTime()
  val psec = (et-st)*1e-9


  def printstats(label: String, l: List[Float]) {
    val mean = l.sum / l.size
    val std  = stddev(l)
    val maxv = l.reduce((a,b) => (a max b))
    val minv = l.reduce((a,b) => (a min b))

    println(f"$label%-10s :  mean=$mean%.3f std=$std%.3f min=$minv%.3f max=$maxv%.3f")
  }
  println()
  println("-" * 60)
  printstats("zeroratio", allzeroratios.toList)
  printstats("cr28",      allratios28.toList)
  printstats("cr56",      allratios56.toList)
  println("-" * 60)
  println()

  println(f"Processing Time[Sec] = $psec%.3f")
}
