package hwcomp

import chisel3.iotesters
import chisel3.iotesters.{Driver, PeekPokeTester}
import pxgen.generator._
import java.io._
import scala.collection.mutable.ListBuffer

class CompUnitTester(c: Comp) extends PeekPokeTester(c) {

  val pw = new PrintWriter(new File("comp-output.txt" ))

  def fillbits(n: Int) = (1<<n) - 1

  def update(rpx: List[Int])  : List[Int] = {
    var idx = 0
    pw.write("IN : ")
    for (r <- rpx) {
      pw.write(r + " ")
      poke(c.io.in(idx), fillbits(r))
      idx += 1
    }
    pw.write("\n")

    val npxs = 16 // XXX: use the param

    val ndata = peek(c.io.ndata)
    val bufsel = peek(c.io.bufsel)
    val bufpos = peek(c.io.bufpos)
    val flushed = peek(c.io.flushed)
    val flushedbuflen = peek(c.io.flushedbuflen)
    pw.write("OUT: ")
    for (i <- 0 until npxs) {
      val out = peek(c.io.out(i))
      pw.write(s"$out ")
    }
    pw.write(s"n=$ndata sel=$bufsel pos=$bufpos fl=$flushed flblen=$flushedbuflen\n")

    return List.tabulate(npxs+1)(i => if (i==0) flushedbuflen.toInt else peek(c.io.out(i-1)).toInt )
  }

  println("==CompUnitTester==")

  val fs = parsePixelStat("./pixelstat.txt")
  val seed = 1234;
  val rn = new scala.util.Random(seed)
  var norig = 0
  var ncompressed = 0
  var nframes = 3  // fs.length
  var generated_rpxs = new ListBuffer[List[Int]]()

  for (i <- 0 until nframes) { // generates N frames of 8x8 data
    val fno = i
    // for (cno <- 7 to 0 by -1 ) { // emulate shift
    for (cno <- 0 until 8 ) { // emulate shift
      val rpx = List.tabulate(8)(rno => pick_nbit(rn.nextDouble, fs(fno).get(cno, rno)))
      generated_rpxs += rpx

      norig += 8
      val cdata = update(rpx)
      val fblen = cdata(0)

      if (fblen > 0 ) {
        ncompressed += fblen
        val cr = norig.toDouble / ncompressed.toDouble
        println(f"$ncompressed%4d $norig%4d $cr%4.1f")
      }
      step(1)

    }
  }
  pw.close

}
