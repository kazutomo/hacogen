package shred

import chisel3.util._
import chisel3.iotesters
import chisel3.iotesters.{Driver, PeekPokeTester}
import testutil._

class ConcatZeroStripUnitTester(c: ConcatZeroStrip) extends PeekPokeTester(c) {

  val nelems_a = c.nelems
  val nelems_b = nelems_a
  val nelems_out = nelems_a + nelems_b
  val bw = c.bw
  val maxval = (BigInt(1) << bw) - BigInt(1)

  type Data = List[BigInt]
  type Mask = BigInt
  class DataMask(val d: Data, val m: Mask)

  // the mask can be more than 64-bit, so BigInt is needed here
  def mkmask(p: Data) : Mask = p.zipWithIndex.map {case (v, i) => if (v>0) BigInt(1)<<i.toInt else BigInt(0)} reduce (_|_)

  // generate a test data in List[BigInt]. l is the length of the
  // List. The value 'v' is fill for the first n elements and 0 is
  // filled for the rest.
  def mktestdat(l: Int, v: BigInt, n: Int) : Data = List.tabulate(l) {i => if (i<n) v else BigInt(0)}

  def gentestDataMask(l: Int, v: BigInt, n: Int) : DataMask = {
    val d = mktestdat(l, v, n)
    val m = mkmask(d)
    new DataMask(d, m)
  }

  def concat(a: DataMask, b: DataMask) : DataMask = {
    val ad_nz = a.d filter (_ != 0)
    val ad_z  = a.d filter (_ == 0)
    val d = ad_nz ::: b.d ::: ad_z
    val l = a.d.length
    val m = (b.m << l) | a.m

    new DataMask(d, m)
  }

  def runtest(a: DataMask, b: DataMask) {

    // fill the input data
    for (i <- 0 until nelems_a) poke(c.io.inA(i), a.d(i))
    for (i <- 0 until nelems_b) poke(c.io.inB(i), b.d(i))
    poke(c.io.inAmask, a.m)
    poke(c.io.inBmask, b.m)

    step(1)
    // compare with reference
    val ref = concat(a, b)
    expect(c.io.outmask, ref.m)
    for (i <- 0 until nelems_out ) expect(c.io.out(i), ref.d(i))

    // debugout
    val outmask = peek(c.io.outmask).toLong
    print("dut.mask=" + TestUtil.convIntegerToBinStr(outmask, nelems_out))
    println(" ref.mask=" + TestUtil.convIntegerToBinStr(ref.m, nelems_out))

    for (i <- 0 until nelems_out ) {
      print("dut=" + TestUtil.convIntegerToHexStr(peek(c.io.out(i)).toLong, nelems_out))
      println(" ref=" + TestUtil.convIntegerToHexStr(ref.d(i), nelems_out))
    }
    println()
  }
  //
  runtest(gentestDataMask(nelems_a, 0, 0),   gentestDataMask(nelems_b, 0, 0))
  runtest(gentestDataMask(nelems_a, 10, nelems_a), gentestDataMask(nelems_b, 0, 0))
  runtest(gentestDataMask(nelems_a, 0,  0),  gentestDataMask(nelems_b, 20, nelems_b))
  runtest(gentestDataMask(nelems_a, 10, nelems_a), gentestDataMask(nelems_b, 20, nelems_b))
}

object ConcatZeroStripTest {

  def run(args: Array[String]) {
    val (argsrest, opt) = TestUtil.getopts(args,
      Map("n" -> 2, "bw" -> 64))

    val dut = () => new ConcatZeroStrip(opt("n"), opt("bw"))
    val tester = c => new ConcatZeroStripUnitTester(c)
    TestUtil.driverhelper(argsrest, dut, tester)
  }
}
