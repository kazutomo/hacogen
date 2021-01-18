//
// ShuffleMerge module tester
//
// written by Kazutomo Yoshii <kazutomo.yoshii@gmail.com>
//
package shred

import chisel3.iotesters
import chisel3.iotesters.{Driver, PeekPokeTester}
import testutil._

class ShuffleMergeUnitTester(c: ShuffleMerge) extends PeekPokeTester(c) {
  // input
  val nelems = c.nelems
  val elemsize = c.elemsize
  // output
  val nblocks = elemsize // the number of blocks after BitShuffle
  val bwblock = nelems   // the bitwidth of each block after BitShuffle

  for (i <- 0 until nelems ) poke(c.io.in(i), 1)

  println("* output")
  val outmask = peek(c.io.outmask).toLong
  println( "mask: " + TestUtil.convLongToBinStr(outmask, nblocks))
  for (i <- 0 until nblocks ) {
    val v = peek(c.io.out(i)).toLong
    print(TestUtil.convLongToHexStr(v, bwblock) + " ")

  }
  println()
}


object ShuffleMergeTest {

  def run(args: Array[String]) {

    val (args2, nelems) = TestUtil.getoptint(args, "nelem", 64)
    val (args3, bw) = TestUtil.getoptint(args2, "bw", 10)

    val dut = () => new ShuffleMerge(nelems, bw)
    val tester = c => new ShuffleMergeUnitTester(c)

    TestUtil.driverhelper(args3, dut, tester)
  }
}
