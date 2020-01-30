package pxgen

import scala.io.Source
import scala.util.control._
import scala.util.matching.Regex

object generator
{
  class Frame (val width: Int = 8, val height: Int = 8) {
    val px = new Array[Array[Double]](width * height)

    def put(x: Int, y: Int, a: Array[Double]) : Unit = {
      val pos = x + y * width
      px(pos) = a
    }

    def get(x: Int, y: Int ) : Array[Double] = {
      val pos = x + y * width
      return px(pos)
    }
  }

  def parsePixelStat(fn: String) : Array[Frame] = {
    //val fn = "pixelstat.txt"
    val re_ino = new Regex("imageno=([0-9]+)")
    val lines = Source.fromFile(fn).getLines.toList
    val nimgs = lines.foldLeft(0)( (acc, l) => if (l contains "imageno") acc + 1 else acc)
    val fs = new Array[Frame](nimgs)
    var fidx = 0

    for (l <- lines) {
      if (l contains "imageno") {
        val tmp = re_ino.findAllIn(l).matchData
        fidx = tmp.next.group(1).toInt
        fs(fidx) = new Frame()
      } else {
        val t = l.split(" +")
        val x = t(0).toInt
        val y = t(1).toInt
        val dist = t.slice(2, t.length).map(x => x.toFloat)
        val accdist = new Array[Double](dist.length)
        var acc = 0.0
        var idx = 0

        // XXX: let's clean up the code below later. make it functional
        for (r <- dist) {
          acc = acc + r
          accdist(idx) = acc / 100.0
          idx = idx + 1
        }
        accdist(idx - 1) = 1.0

        fs(fidx).put(x, y, accdist)
      }
    }

    return fs
  }

  def pick_nbit(dice: Double, dist: Array[Double]) : Int = {
    // pick an integer value [0, 16] randomly with a given distribution 'rd'
    // 0 means no bit is occupied, 1 means 1 bit occupied (e.g., 0b1)
    // 2 means 2 bits occupied (e.g., 0b11)

    var prev = 0.0
    var idx = 0
    val inner = new Breaks
    inner.breakable {
      for (d <- dist) {
        if (prev <= dice && dice < d ) inner.break
        idx += 1
        prev = d
      }
    }
    //for (d <- dist)  print(f"$d%.5f" + " ")
    //print(" : ")
    //println(f"$dice%.5f" + " => " + idx)
    idx
  }
}
