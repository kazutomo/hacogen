package hwcomp

import chisel3.iotesters
import chisel3.iotesters.{Driver, PeekPokeTester}
import pxgen.generator._
import java.io._

class CompUnitTester(c: Comp) extends PeekPokeTester(c) {

  val pw = new PrintWriter(new File("comp-output.txt" ))

  def fillbits(n: Int) = (1<<n) - 1

  def update(rpx: List[Int])  : Int = {
    var idx = 0
    pw.write("IN : ")
    for (r <- rpx) {
      pw.write(r + " ")
      poke(c.io.in(idx), fillbits(r))
      idx += 1
    }
    pw.write("\n")

    val ndata = peek(c.io.ndata)
    val bufsel = peek(c.io.bufsel)
    val bufpos = peek(c.io.bufpos)
    val flushed = peek(c.io.flushed)
    val flushedbuflen = peek(c.io.flushedbuflen)
    pw.write("OUT: ")
    for (i <- 0 to 15) { // XXX: use the param
      val out = peek(c.io.out(i))
      pw.write(s"$out ")
    }
    pw.write(s"n=$ndata sel=$bufsel pos=$bufpos fl=$flushed flblen=$flushedbuflen\n")
    step(1)

    return flushedbuflen.toInt
  }

  println("==CompUnitTester==")

  val fs = parsePixelStat("./pixelstat.txt")
  val seed = 1234;
  val rn = new scala.util.Random(seed)
  var norig = 0
  var ncompressed = 0

  for (i <- 0 to 10) {
    val fno = 34 + i
    for (cno <- 7 to 0 by -1 ) {
      val rpx = List.tabulate(8)(rno => pick_nbit(rn.nextDouble, fs(fno).get(cno, rno)))

      norig += 8
      val fblen = update(rpx)
      if (fblen > 0 ) {
        ncompressed += fblen
        val cr = norig.toDouble / ncompressed.toDouble
        println(f"$ncompressed%4d $norig%4d $cr%4.1f")
      }
    }
  }

  pw.close
}
