//package rawdata

import java.io._
import java.nio.ByteBuffer
import Array._
import scala.collection.mutable.ListBuffer

import javax.imageio.ImageIO // png
import java.awt.image.BufferedImage // png

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
  var dump_vsample = false
  var vsamplexpos = 0
  // NOTE: assume width and height >= 256. for now, fixed
  val window_width  = 256
  val window_height = 256
  var xoff = 0
  var yoff = 0
  // compressor params. non-adjustable for now
  var npxbits = 9      // 9 bits per pixels
  val yd = 16  // the size input to the encoder

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
    fnostart = getIntVal(m, "fnostart")
    fnostop = getIntVal(m, "fnostop")
    dump_gray = getBoolVal(m, "gray")
    dump_png = getBoolVal(m, "png")

    m.get("vsample") match {
      case Some(v) => dump_vsample = true; vsamplexpos = v.toInt
      case None => 0
    }

    // FIX: add xoff,yoff later
    xoff = (width/2)  - (window_width/2)
    yoff = (height/2) - (window_width/2)
  }

  def printinfo() = {
    println("[Info]")
    println("dataset: " + filename)
    println("width:   " + width)
    println("height:  " + height)
    println("size:    " + psize)
    println("")
  }
}


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

  def skipImage(in: FileInputStream, step: Int) {
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
          val v2 = if (v < 0) {0} else {if (v>=clipval) clipval else v}
          // val v2 = v.abs
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
      val sval : Short = if (v < 0) 0 else v.toShort
      // val sval : Short = if (v <= 0) 0 else 1
      val tmp = ShortToBytes(sval)
      val idx = y*width + x

      buf(idx*2 + 0) = tmp(0)
      buf(idx*2 + 1) = tmp(1)
    }

    try {
      val out = new FileOutputStream(fn)
      out.write(buf)
      out.close()
    } catch {
      case e: FileNotFoundException => println("Not found:" + fn)
      case e: IOException => println("Failed to write")
    }
    true
  }

  // pixel value higher equal than threshold, assign the max brightness
  def writePng(fn: String, threshold: Int) : Unit = {
    val img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    def graytorgb(a: Int) = (a<<16) | (a<<8) | a

    for (x <- 0 until width) {
      for (y <- 0 until height) {
        val tmp = getpx(x,y)
        val tmp2 = if(tmp>=threshold) 255 else tmp
        img.setRGB(x, y,  graytorgb(tmp2))
      }
    }
    ImageIO.write(img, "png", new File(fn))
  }


  def getVerticalLineSample(x: Int, y1: Int, y2: Int) : List[Int] = {
    List.tabulate(y2-y1) {i => getpx(x, y1 + i) }
  }

  def shuffleVerticalLineSample(data: List[Int], stride: Int) : List[Int] = {
    val a = data.toArray
    List.tabulate(data.length) {i =>  a( ((i%stride)*stride) + (i/stride) )}
  }

  def bitshuffleVerticalLineSample(data: List[Int], npxblock: Int, bitspx: Int) : List[Int] = {
    var reslen = (data.length/npxblock) * bitspx
    var res = new Array[Int](reslen)
    var pidx = 0 // index in List
    val maxval = (1<<bitspx) - 1

    // convert data to bits array
    // each element hold particular bit in a pixel
    def bits2list(v: Int, b: Int) : List[Int] = { List.tabulate(b) { idx => if (((1<<idx)&v)>0) 1 else 0 } }
    var btmp = ListBuffer[Int]() // a bit wasting...
    for(p <- data) {
      val pclip = if (p>maxval) maxval else p
      bits2list(pclip, bitspx).foreach {v => btmp += v}
    }

    val bdata = btmp.toArray
    // println(s"bdata.length=$bdata.length npxblock=$npxblock bitspx=$bitspx")

    // constructing output, filling res
    for (idxbase <- 0 until bdata.length by (npxblock*bitspx)) {
      val residxbase = (idxbase / (npxblock*bitspx)) * bitspx

      // bidx is pixel pos in dst
      for(bidx <- 0 until bitspx) {
        // collect bidx'th bit's value from all pixels in a block
        val tmp = List.tabulate(npxblock)
        {i => bdata(idxbase + i*bitspx + bidx) << i }

        res(residxbase + bidx) = tmp.reduce(_|_)
      }
    }
    res.toList
  }

  def writeData2Text(fn: String, data: List[Int]) {
    try {
      val f = new File(fn)
      val out = new BufferedWriter(new FileWriter(f))
      data.foreach{v => out.write(f"$v\n")}
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

  var ap = new AppParams()

  ap.getopts(args)
  ap.printinfo()

  //

  var allratios28 = new ListBuffer[Float]()
  var allratios56 = new ListBuffer[Float]()
  var allzeroratios = new ListBuffer[Float]()

  // open a dataset
  val in = new FileInputStream(ap.filename)
  val rawimg = new RawImageDataSet(ap.width, ap.height)

  val nshifts = (ap.height/ap.yd) * ap.width

  val st = System.nanoTime()

  // need to skip frames
  for (fno <- 0 until ap.fnostart) {
    rawimg.skipImage(in, ap.psize)
    println(s"Skipping $fno")
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

    if(ap.dump_vsample) {
      val vsx = ap.vsamplexpos
      val vsy1 = ap.yoff
      val vsy2 = ap.yoff + ap.window_height
      val vs = rawimg.getVerticalLineSample(vsx, vsy1, vsy2)
      val vsfn = f"vsample-x${vsx}y${vsy1}until${vsy2}-fr${fno}.txt"
      println(s"Writing $vsfn")
      rawimg.writeData2Text(vsfn, vs)
/*
      val vss = rawimg.shuffleVerticalLineSample(vs, 16)
      val vssfn = f"vsample-shuffle16-x${vsx}y${vsy1}until${vsy2}-fr${fno}.txt"
      println(s"Writing $vssfn")
      rawimg.writeData2Text(vssfn, vss)
 */
      val vsbs = rawimg.bitshuffleVerticalLineSample(vs, 16, 9)
      val vsbsfn = f"vsample-9bitshuffle16-x${vsx}y${vsy1}until${vsy2}-fr${fno}.txt"
      println(s"Writing $vsbsfn")
      rawimg.writeData2Text(vsbsfn, vsbs)
    }

    // enclens is created for each frames
    var enclens = new ListBuffer[Int]()

    val hyd = ap.height - (ap.height % ap.yd) // to simplify, ignore the remaining

    // chunk up to rows whose height is yd
    for (yoff <- 0 until hyd by ap.yd) {
      // emulate pixel shift
      for (xoff <- 0 until ap.width) {
        // create a column chunk, which is an input to the compressor
        val dtmp = List.tabulate(ap.yd)(
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
          tmp_ratios += (ap.yd.toFloat / l.toFloat)
        }
      } else {
        for (l <- enclens) {
          if (npxs_used + l < noutpixs) {
            npxs_used += l
            input_cnt += ap.yd
          } else {
            tmp_ratios += (input_cnt.toFloat / noutpixs.toFloat)
            npxs_used = l
            input_cnt = ap.yd
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
