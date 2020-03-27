//package rawdata

import java.io._
import java.nio.ByteBuffer
import Array._
import scala.collection.mutable.ListBuffer

// assume an image is consist of 32-bit (signed) integer pixels.
object RawIntImages {

  def BytesToInt(buf: Array[Byte]) : Int = {
    (buf(3)<<24) | (buf(2)<<16) | (buf(1)<<8) | buf(0)
  }

  def ShortToBytes(v: Short): Array[Byte] = {
    val tmp = new Array[Byte](2)

    tmp(0) = v.toByte
    tmp(1) = (v >> 8).toByte

    tmp
  }

  def readframe(in: FileInputStream, w: Int, h: Int) :
      (Array[Array[Short]], Short, Int) = {
    var frame = ofDim[Short](h, w)
    val step = 4
    var maxval : Short = 0
    var zcnt : Int = 0

    try {
      val buf = new Array[Byte]( (w*h)*step)

      in.read(buf)
      // convert byte buf to image. not efficient
      for (y <- 0 until h) {
        for (x <- 0 until w) {
          var idx = y * w + x
          val v = BytesToInt(buf.slice(idx*step, idx*step+4))
          val v2 : Short = if (v < 0) {0} else {v.toShort}
          if (v2 == 0) zcnt += 1
          if (v2 > maxval) { maxval = v2 }
          frame(y)(x) = v2
        }
      }
    } catch {
      case e: IOException => println("Failed to read")
    }
    (frame, maxval, zcnt)
  }


  def writegray(image: Array[Array[Short]], fn: String, w: Int, h: Int) : Boolean = {
    val step = 2
    var buf = new Array[Byte]( (w*h) * step )

    for (y <- 0 until h; x <- 0 until w ) {
      val sval : Short = if (image(y)(x) < 0) 0 else image(y)(x).toShort
      val tmp = ShortToBytes(sval)
      val idx = y*w + x

      buf(idx*2 + 0) = tmp(0)
      buf(idx*2 + 1) = tmp(1)
    }

    try {
      val out = new FileOutputStream(fn)
      out.write(buf)
    } catch {
      case e: FileNotFoundException => println("Not found:" + fn)
      case e: IOException => println("Failed to write")
    }

    true
  }

  // software-based encoding for validation
  def encoding(px: List[Short]) : List[Short] = {
    val headerlist = List.tabulate(px.length)(i => if (px(i) == 0) 0 else 1<<i)
    val header = headerlist.reduce(_ + _) // | (1 << (c.elemsize-1))
    val nonzero = px.filter(x => x > 0)
    return List.tabulate(nonzero.length+1)(i => if(i==0) header.toShort else nonzero(i-1).toShort )
  }
}


object RawimageAnalyzerMain extends App {

  def printmemoryusage : Unit = {
    val r = Runtime.getRuntime
    println( "Free(MB) : " + (r.freeMemory >> 20) )
    println( "Total(MB): " + (r.totalMemory >> 20) )
  }

  var fn = ""
  var h = 0
  var w = 0
  var fnostart = 0
  var fnostop = 0
  var dumpflag = false
  // x y steps
  val xd = 8
  val yd = 8

  if (args.length < 6) {
    println("Usage: RawimageAnalyzerMain rawimgfilename width height fnostart fnostop dump")
    println("")
    println("This command reads a simple raw image format that contains a number of glay scala images with 32-bit integer pixel value (its dynamic range is possible smaller). Width and Height are specified via command line. The image framenumber start and stop are also specified via command line.")
    System.exit(1)
  }

  fn = args(0)
  h = args(1).toInt
  w = args(2).toInt
  fnostart = args(3).toInt
  fnostop = args(4).toInt
  dumpflag = args(5).toBoolean

  var allratios = new ListBuffer[Float]()


  val in = new FileInputStream(fn)

  val st = System.nanoTime()
  for (fno <- fnostart to fnostop) {
    val (fr, maxval, zcnt) = RawIntImages.readframe(in, w, h)
    val zeroratio = zcnt.toFloat / (w*h).toFloat * 100.0
    println(f"zero=$zeroratio%.2f")

    // write back to file.
    // To display an image, display -size $Wx$H -depth 16  imagefile
    if (dumpflag)   RawIntImages.writegray(fr, f"fr$fno.gray", w, h)

    var ratios = new ListBuffer[Float]()

    for (yoff <- 0 until h-yd by yd) {
      for (xoff <- 0 until w-xd  by xd) {

        for (x <- 0 until xd) {
          val dtmp = List.tabulate(yd)(rno => fr(rno + yoff)(x + xoff))
          val enctmp = RawIntImages.encoding(dtmp)

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
