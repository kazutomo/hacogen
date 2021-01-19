package shred

import chisel3.util._
import chisel3.iotesters
import chisel3.iotesters.{Driver, PeekPokeTester}
import testutil._

class Comp128UnitTester(c: Comp128) extends PeekPokeTester(c) {
  val bitwidth = c.elemsize
  val nrows = c.nrows
  val nshifts = c.nshifts
  val nblocks = c.nblocks
  val bwblock = c.bwblock
  val uncompressedbits = bitwidth*nrows*nshifts
  val headerbits = nblocks

  def bitShuffle(pxs: List[BigInt], bitspx: Int) : List[BigInt] = {
    val npxblock = pxs.length
    val inp = pxs.toArray
    val zero = BigInt(0)
    val res = new Array[BigInt](bitspx)

    def mkNthbitH(n: Int) : BigInt = BigInt(1) << n
    def isNthbitH(v: BigInt, n: Int) : Boolean =  (v&mkNthbitH(n)) > BigInt(0)

    for (bpos <- 0 until bitspx) {
      res(bpos) =
        List.tabulate(npxblock) {i => if(isNthbitH(inp(i),bpos)) mkNthbitH(i) else zero} reduce (_|_)
    }
    res.toList
  }

  def ref(data: List[BigInt]) : (List[BigInt], BigInt) = {
    val blocks = data.sliding(bwblock, bwblock).toList

    val tmp = blocks map {b => bitShuffle(b, bitwidth)}
    val shuffled = tmp.flatten

    val mask = shuffled.zipWithIndex.map {case (v,i) => if (v>0) BigInt(1)<<i.toInt else BigInt(0)} reduce (_|_)

    val nz = shuffled filter (_ !=0)
    val z = shuffled filter (_ ==0)
    (nz:::z, mask)
  }

  def runtest(data: List[BigInt]) {
    data.zipWithIndex.map {case (v,i) => poke(c.io.in(i), v)}
    val (refout, refoutmask) = ref(data)

    expect(c.io.outmask, refoutmask)
    refout.zipWithIndex.map {case (v,i) => expect(c.io.out(i), v)}

    // debug output
    val out = List.tabulate(nblocks) {i => peek(c.io.out(i))}
    val outmask = peek(c.io.outmask)

    val nz = out.filter (_ != 0)
    val compressedbits = (nz.length * bwblock) + headerbits
    println("uncompressedbits/compressedbits: " + uncompressedbits + "/" + compressedbits + " => " + (uncompressedbits.toFloat/compressedbits.toFloat))

    println("dut.mask=" + TestUtil.convIntegerToBinStr(outmask, nblocks))
    println("ref.mask=" + TestUtil.convIntegerToBinStr(refoutmask, nblocks))
    // out foreach {v => print(TestUtil.convIntegerToHexStr(v, bwblock) + " ")}
    //println()
  }

  // simple inputs
  val n = nrows*nshifts
  runtest(List.fill(n){BigInt(0)})
  runtest(List.fill(n){BigInt(1)})
  runtest(List.fill(n){BigInt(1023)})
  runtest(List.tabulate(n){i => if ((i%8)==1) BigInt(1) else BigInt(0)})
}

object Comp128Test {

  def run(args: Array[String]) {

    val dut = () => new Comp128()
    val tester = c => new Comp128UnitTester(c)

    TestUtil.driverhelper(args, dut, tester)
  }
}
