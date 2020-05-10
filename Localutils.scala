package localutil

import java.io._

object Util {
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

  def printstats(label: String, l: List[Float]) {
    val mean = l.sum / l.size
    val std  = stddev(l)
    val maxv = l.reduce((a,b) => (a max b))
    val minv = l.reduce((a,b) => (a min b))
    println(f"$label%-10s :  mean=$mean%.3f std=$std%.3f min=$minv%.3f max=$maxv%.3f")
  }

  def writeList2File(fn: String, data: List[Int]) {
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
