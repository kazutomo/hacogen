//
// HACOGen tester
//
// written by Kazutomo Yoshii <kazutomo.yoshii@gmail.com>
//
package hacogen

import scala.collection.mutable.ListBuffer
import localutil.Util._
import refcomp.RefComp._

import java.io._
import chisel3.iotesters
import chisel3.iotesters.{Driver, PeekPokeTester}

import testutil._

class SHCompUnitTester(c: SHComp) extends PeekPokeTester(c) {
  // SHComp
  // val sh_nelems_src:Int = 16
  // val sh_elemsize:Int = 9
  // val nelems_dst:Int = 28

  val nelems_src   = c.sh_nelems_src
  val elemsize_src = c.sh_elemsize
  val nelems_dst   = c.nelems_dst
  val elemsize_dst = c.sh_nelems_src

  println(f"nelems_src   = ${nelems_src}")
  println(f"elemsize_src = ${elemsize_src}")
  println(f"nelems_dst   = ${nelems_dst}")
  println(f"elemsize_dst = ${elemsize_dst}")

  val pw = new PrintWriter(new File("shcomp-output.txt" ))

  // TODO: fill an actual test here after CompTest gets updated
  //

  // SHComp:
  //
  // val in  = Input(Vec(sh_nelems_src, UInt(sh_elemsize.W)))
  // val out = Output(Vec(nelems_dst, UInt(elemsize.W)))
  //
  // below are debuginfo
  // val bufsel = Output(UInt(1.W))
  // val bufpos = Output(UInt(log2Ceil(nelems_dst).W))
  // val flushed = Output(Bool())
  // val flushedbuflen = Output(UInt(log2Ceil(nelems_dst+1).W))
  //

  val seed = 100
  val r = new scala.util.Random(seed)
  val maxnzpxs = 4

  // TODO: use Estimator's code to generate test patterns from actual image
  def geninputpxs() : List[Int] = {

    // randomly generate a test pattern with the lengtn nelems_src, where the total number of non-zero pixels is npxs.
    val npxs = r.nextInt(maxnzpxs)
    // fill a list than includes the randomly-selected locations of non-zero pixels
    val pxidxs = List.tabulate(npxs) { _ => r.nextInt(nelems_src)}
    val tmp = List.tabulate(nelems_src) { i => if (pxidxs contains i) r.nextInt(7) +1 else 0}
    tmp
  }

  var refoutput = ListBuffer[Int]()
  val ninputs = 50
  var inputbits = 0
  var outputbits = 0

  for (i <- 0 until ninputs) {
    val testp = geninputpxs()
    val ref = shzsEncode(testp, elemsize_src)
    refoutput ++= ref

    inputbits += nelems_src * elemsize_src

    pw.write(f"step=$i\n")
    pw.write("testp: ")
    for(e <- testp) pw.write(f"$e%02x ")
    pw.write("\nref: ")
    for(e <- ref) pw.write(f"$e%02x ")
    pw.write("\n")

    // fill an input data to the SHComp module with testp
    for (j <- 0 until nelems_src)  poke(c.io.in(j), testp(j))

    step(1)
    val bufpos = peek(c.io.bufpos)
    val flushed = peek(c.io.flushed)
    val flushedbuflen = peek(c.io.flushedbuflen)
    if (flushed != 0) {
      pw.write(f"len=$flushedbuflen%d p=$bufpos%d\nhw: ")
      for (k <- 0 until flushedbuflen.toInt) {
        val tmphw = peek(c.io.out(k))
        pw.write(f"${tmphw}%02x ")
        pw.flush()
        val tmpref = refoutput(k)
        expect(c.io.out(k), tmpref)
      }
      pw.write("\n")

      refoutput.clear()
      if (refoutput.length > nelems_dst) {
        refoutput ++= ref
      }

      outputbits += nelems_dst * elemsize_dst
    }
  }
  pw.close()

  val crratio = inputbits.toDouble / outputbits
  println(f"crratio=$crratio%.3f")

  println("done")
}

object SHCompTest {
  def run(args: Array[String]) {
    val (argsrest, opt) = TestUtil.getopts(args,
      Map("n" -> 16, "bw" -> 9, "ndst" -> 28))

    val dut = () => new SHComp(opt("n"), opt("bw"), opt("ndst"))
    val tester = c => new SHCompUnitTester(c)
    TestUtil.driverhelper(argsrest, dut, tester)
  }
}
