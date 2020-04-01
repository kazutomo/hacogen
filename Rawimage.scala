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
    (buf(3)<<24) | (buf(2)<<16) | (buf(1)<<8) | buf(0)
  }

  def ShortToBytes(v: Short): Array[Byte] = {
    val tmp = new Array[Byte](2)

    tmp(0) = v.toByte
    tmp(1) = (v >> 8).toByte

    tmp
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
          val v2 = if (v < 0) {0} else v
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
}


object RawimageAnalyzerMain extends App {

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


  if (args.length < 6) {
    println("Usage: RawimageAnalyzerMain rawimgfilename width height fnostart fnostop dump")
    println("")
    println("This command reads a simple raw image format that contains a number of glay scala images with 32-bit integer pixel value (its dynamic range is possible smaller). Width and Height are specified via command line. The image framenumber start and stop are also specified via command line.")
    System.exit(1)
  }

  val fn = args(0)
  val w = args(1).toInt
  val h = args(2).toInt
  val sz = args(3).toInt // 4 is 4-byte int, 1 is 1-byte int
  val fnostart = args(4).toInt
  val fnostop = args(5).toInt
  val dumpflag = args(6).toBoolean
  // x y steps
  val xd = 8
  val yd = 8

  var allratios = new ListBuffer[Float]()

  // open a dataset
  val in = new FileInputStream(fn)

  val rawimg = new RawImageDataSet(w, h)

  println("[Info]")
  println("width:  " + rawimg.width)
  println("height: " + rawimg.height)
  println("size:   " + sz)
  println("")

  if (! Seq(1,4).contains(sz)) {
    println("Error: invalid size: " + sz)
    System.exit(1)
  }

  val st = System.nanoTime()
  for (fno <- fnostart to fnostop) {
    if (sz == 4) rawimg.readImageInt(in)
    else if (sz == 1) rawimg.readImageByte(in)

    val zeroratio = rawimg.zerocnt.toFloat / (w*h).toFloat * 100.0
    println(f"zero=$zeroratio%.2f")

    // write back to file.
    // To display an image, display -size $Wx$H -depth 16  imagefile
    if (dumpflag)   rawimg.writeImageShort(f"fr$fno.gray")

    var ratios = new ListBuffer[Float]()

    val hyd = h - (h % yd)
    val wxd = w - (w % xd)

    // chunk up to blocks whose width is xd and height is yd
    for (yoff <- 0 until hyd by yd) {
      for (xoff <- 0 until wxd by xd) {

        for (x <- 0 until xd) {
          val dtmp = List.tabulate(yd)(
            rno =>
            rawimg.getpx(x + xoff, rno + yoff) )
          val enctmp = encoding(dtmp)

          val cr =  (xd.toFloat / enctmp.length)
          ratios += cr
          /*
          if ( dtmp.map(_.toInt).reduce( (a,b) => (a + b)) >= maxval ) {
            println(f"($xoff,$yoff) cr=$cr%.1f :" + " " + dtmp.mkString(",") + " >> " + enctmp.mkString(",") )
          }
           */
        }
      }
    }

    if (ratios.length > 0) {
      val rtmp = ratios.toList
      val rmean = (rtmp.reduce( (a,b) => (a + b) )) / rtmp.length.toFloat
      val rmin = rtmp.reduce( (a,b) => (a min b) )
      val rmax = rtmp.reduce( (a,b) => (a max b) )

      allratios += rmean

      //println(f"fno=$fno%4d mean=$rmean%.2f min=$rmin%.2f rmax=$rmax%.2f")
      //println(f"$rmean%.3f") // just compression ratio
    }
  }
  val et = System.nanoTime()
  val psec = (et-st)*1e-9

  val allmean = allratios.reduce((a,b) => (a+b)) / allratios.length.toFloat
  println(f"Compression Ratio Mean = $allmean%.3f")

  println(f"Processing Time[Sec] = $psec%.3f")
}
