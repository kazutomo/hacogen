//package rawdata
1;5202;0c
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

  def writegray(image: Array[Array[Short]], fn: String, w: Int, h: Int) : Boolean = {
    val step = 4
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

object Main extends App {

  def printmemoryusage : Unit = {
    val r = Runtime.getRuntime
    println( "Free(MB) : " + (r.freeMemory >> 20) )
    println( "Total(MB): " + (r.totalMemory >> 20) )
  }

  printmemoryusage

  val fn = "pilatus_image_1679x1475x300_int32.raw"
  val w = 1679
  val h = 1475
  val xoff = 100
  val yoff = 200
  val xd = 8
  val yd = 8
  val nframes = 10 // 300
  val st = System.nanoTime()
  val images = RawIntImages.readimages(fn, w, h, nframes)
  val et = System.nanoTime() - st

  printmemoryusage

  println(et * 1e-9 + " sec")

  for (fno <- 0 until nframes) {
    var zcnt = 0
    for (y <- yoff until yoff+yd) {
      for (x <- xoff until xoff+xd) {
        if (images(fno)(y)(x) == 0) zcnt += 1
      }
    }
    println(fno + " " + zcnt + " " + (zcnt*100.0/(xd*yd)))
  }

  // write back to file
  RawIntImages.writegray(images(0), "a.gray", w, h)
}
