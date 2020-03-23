//package rawdata

import java.io._
import java.nio.ByteBuffer
import Array._

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

  def readimages(fn: String, w: Int, h: Int, nframes: Int) : Array[Array[Array[Short]]] = {
    var images = ofDim[Short](nframes, h, w)
    val step = 4

    try {
      val in = new FileInputStream(fn)
      val buf = new Array[Byte]( (w*h)*step)

      for (fno <- 0 until nframes) {
        in.read(buf)
        // convert byte buf to image. not efficient
        for (y <- 0 until h) {
          for (x <- 0 until w) {
            var idx = y * w + x
            val v = BytesToInt(buf.slice(idx*step, idx*step+4))
            val v2 : Short = if (v < 0) {0} else {v.toShort}
            images(fno)(y)(x) = v2
          }
        }
      }
    } // add exception handing later

    images
  }

  def readframe(in: FileInputStream, w: Int, h: Int) :
      Array[Array[Short]] = {
    var frame = ofDim[Short](h, w)
    val step = 4

    try {
      val buf = new Array[Byte]( (w*h)*step)

      in.read(buf)
      // convert byte buf to image. not efficient
      for (y <- 0 until h) {
        for (x <- 0 until w) {
          var idx = y * w + x
          val v = BytesToInt(buf.slice(idx*step, idx*step+4))
          val v2 : Short = if (v < 0) {0} else {v.toShort}
          frame(y)(x) = v2
        }
      }
    } // add exception handing later
    frame
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
    }

    true
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

  //val st = System.nanoTime()
  //val images = RawIntImages.readimages(fn, w, h, nframes)
  //val et = System.nanoTime() - st
  // printmemoryusage

  val in = new FileInputStream(fn)

  for (fno <- fnostart to fnostop) {
    val fr = RawIntImages.readframe(in, w, h)

    // write back to file
    if (dumpflag)   RawIntImages.writegray(fr, f"fr$fno.gray", w, h)

    for (xoff <- 0 until w-xd  by xd) {
      for (yoff <- 0 until h-yd by yd) {
        var zcnt = 0
        for (y <- yoff until yoff+yd) {
          for (x <- xoff until xoff+xd) {
            if (fr(y)(x) == 0) zcnt += 1
          }
        }
        println(fno + " " + xoff + "," + yoff + " => " + (zcnt*100.0/(xd*yd)))
      }
    }
  }
}
