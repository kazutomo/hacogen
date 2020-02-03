//
// HACOGen tester
//
// written by Kazutomo Yoshii <kazutomo.yoshii@gmail.com>
//
package hacogen

import chisel3.iotesters
import chisel3.iotesters.{Driver, PeekPokeTester}
import pxgen.generator._
import java.io._
import scala.collection.mutable.ListBuffer

class CompUnitTester(c: Comp) extends PeekPokeTester(c) {

  val pw = new PrintWriter(new File("comp-output.txt" ))

  def fillbits(n: Int) = (1<<n) - 1

  def compressor(px: List[Int]) : List[Int] = {
    val headerlist = List.tabulate(px.length)(i => if (px(i) == 0) 0 else 1<<i)
    val header = headerlist.reduce(_ + _) | (1 << (c.elemsize-1))
    val nonzero = px.filter(x => x > 0)
    return List.tabulate(nonzero.length+1)(i => if(i==0) header else nonzero(i-1))
  }

  def update(px: List[Int])  : List[Int] = {
    var idx = 0
    for (r <- px) {
      poke(c.io.in(idx), r)
      idx += 1
    }
    val npxs = c.nelems_dst

    val ndata = peek(c.io.ndata)
    val bufsel = peek(c.io.bufsel)
    val bufpos = peek(c.io.bufpos)
    val flushed = peek(c.io.flushed)
    val flushedbuflen = peek(c.io.flushedbuflen)
    for (i <- 0 until npxs) {
      val out = peek(c.io.out(i))
      pw.write(f"$out%02x ")
    }
    pw.write(f"n=$ndata s=$bufsel p=$bufpos%x fl=$flushed/len=$flushedbuflen\n")

    return List.tabulate(npxs+1)(i => if (i==0) flushedbuflen.toInt else peek(c.io.out(i-1)).toInt )
  }

  println("==CompUnitTester==")

  val fs = parsePixelStat("./pixelstat.txt")
  val seed = 123456;
  val rn = new scala.util.Random(seed)
  var norig = 0
  var ncompressed = 0
  var nframes = fs.length  // fs.length for max
  var generated_rpxs = new ListBuffer[List[Int]]()
  var npixels = c.nelems_src
  var compressedchunks = new ListBuffer[List[Int]]()
  var failed = 0

  for (i <- 0 until nframes) { // generates N frames of 8x8 data
    val fno = i
    for (cno <- 0 until npixels ) {  // emulate shift
      val pxtmp = List.tabulate(npixels)(rno => pick_nbit(rn.nextDouble, fs(fno).get(cno, rno)))
      val px = pxtmp.map(x => fillbits(x))

      generated_rpxs += px

      norig += c.nelems_src

      for (p <- px ) pw.write(f"$p%02x ")
      pw.write(" => ")

      val cdata = update(px)
      val fblen = cdata(0)

      val zt = compressor(px)
      if (fblen == 0) {
        compressedchunks += zt
      }
      for (z <- zt ) pw.write(f"$z%02x ")
      pw.write("\n")

      if (fblen > 0 ) {
        ncompressed += fblen
        val cr = norig.toDouble / ncompressed.toDouble
        println(f"$ncompressed%4d $norig%4d $cr%4.1f")

        for (i <- 0 until c.nelems_dst) {
          if (i < compressedchunks.flatten.length ) {
            val cchunks = compressedchunks.flatten
            if (cdata(i+1) != cchunks(i)) {
              pw.write(f"Failed to validate: cdata(${i+1}) = ${cdata(i+1)} cchunks(${i}) = ${cchunks(i)}\n")
              failed += 1
            }
          } else {
            if (cdata(i+1) != 0) {
              pw.write(f"Failed to validate: cdata(${i+1}) = ${cdata(i+1)}\n")
              failed += 1
            }
          }
        }

        compressedchunks.clear
        compressedchunks += zt
      }
      step(1)
    }
  }
  pw.close

  if (failed == 0) println("Validation passed!!!!")

}
