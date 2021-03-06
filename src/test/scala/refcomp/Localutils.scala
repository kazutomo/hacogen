package refcomp

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

  class StatT(val mean: Float=0.0.toFloat, val std: Float=0.0.toFloat, val min: Float=0.0.toFloat, val max: Float=0.0.toFloat)

  def calcStats(l: List[Float]) : StatT = {
    val mean = l.sum / l.size
    val std  = stddev(l)
    val minv = l.reduce((a,b) => (a min b))
    val maxv = l.reduce((a,b) => (a max b))

    new StatT(mean, std, minv, maxv)
  }

  def printStats(label: String, l: List[Float]) {
    val st = calcStats(l)
    println(f"$label%-12s :  mean=${st.mean}%.5f std=${st.std}%.5f min=${st.min}%.5f max=${st.max}%.5f")
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
