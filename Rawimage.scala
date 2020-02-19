//package rawdata

import java.io._
import java.nio.ByteBuffer
import Array._

object RawIntImages {

  def BytesToInt(buf: Array[Byte]) : Int = {
    (buf(3)<<24) | (buf(2)<<16) | (buf(1)<<8) | buf(0)
  }

  def readimages(fn: String, w: Int, h: Int, nframes: Int) : Array[Array[Array[Int]]] = {
    var images = ofDim[Int](nframes, w, h)
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
            images(fno)(x)(y) = v
          }
        }
      }
    } // add exception handing later

    images
  }
}

object Main extends App {

  val fn = "pilatus_image_1679x1475x300_int32.raw"
  val w = 1679
  val h = 1475
  val nframes = 10 // 300
  val st = System.nanoTime()
  val images = RawIntImages.readimages(fn, w, h, nframes)
  val et = System.nanoTime() - st
  println(et * 1e-9 + " sec")

  for (fno <- 0 until nframes) {
    var zcnt = 0
    for (y <- 0 until h; x <- 0 until w) {
      if (images(fno)(x)(y) == 0) zcnt += 1
    }
    println(fno + " " + zcnt)
  }
}
