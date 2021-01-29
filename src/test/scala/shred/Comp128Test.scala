package shred

import chisel3.util._
import chisel3.iotesters
import chisel3.iotesters.{Driver, PeekPokeTester}
import testutil._

import java.io._
import refcomp._
import refcomp.Util._
import refcomp.RefComp._

object LocalParam {
  var fn : String = ""
  var w : Int = 0
  var h : Int = 0
  var sz : Int = 0
  var fno : Int = 0
}

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

  def runtest(data: List[BigInt]) : Float = {
    data.zipWithIndex.map {case (v,i) => poke(c.io.in(i), v)}
    step(1)
    val (refout, refoutmask) = ref(data)

    expect(c.io.outmask, refoutmask)
    refout.zipWithIndex.map {case (v,i) => expect(c.io.out(i), v)}

    // debug output
    val out = List.tabulate(nblocks) {i => peek(c.io.out(i))}
    val outmask = peek(c.io.outmask)

    val nz = out.filter (_ != BigInt(0))
    val compressedbits = (nz.length * bwblock) + headerbits

    //nz foreach {v => print(TestUtil.convIntegerToHexStr(v,64) + " ")}
    //println("nz.length=" + nz.length)
    //println("bwblock=" + bwblock)
    //println("headerbits=" + headerbits)

    val ratio = (uncompressedbits.toFloat/compressedbits.toFloat)

    println("uncompressedbits/compressedbits: " + uncompressedbits + "/" + compressedbits + " => " + ratio)

    println("dut.mask=" + TestUtil.convIntegerToBinStr(outmask, nblocks))
    println("ref.mask=" + TestUtil.convIntegerToBinStr(refoutmask, nblocks))
    // out foreach {v => print(TestUtil.convIntegerToHexStr(v, bwblock) + " ")}
    println("---------------------------------")
    println()

    ratio
  }

  // simple inputs
  if(true) {
    val n = nrows*nshifts
    runtest(List.fill(n){BigInt(0)})

    runtest(List.fill(n){BigInt(0)})
    runtest(List.fill(n){BigInt(1)})
    runtest(List.fill(n){BigInt(1023)})
    runtest(List.tabulate(n){i => if ((i%8)==1) BigInt(1) else BigInt(0)})
    runtest(List.tabulate(10){v => BigInt(v)} ::: List.fill(n-10){BigInt(0)} )
  }

  // load data from image: quick&dirty hard-coded version.
  // clean up and add options later
  if(LocalParam.fn.length>0 && LocalParam.sz> 0) {
    val filename = LocalParam.fn
    val width    = LocalParam.w
    val height   = LocalParam.h
    val psize    = LocalParam.sz
    val frameno  = LocalParam.fno
    val ycenter = height/2
    val windowheight = 128
    val ybegin = ycenter - windowheight/2
    //val nshifts = 8
    val npxblock = 8

    val in = new FileInputStream(filename)
    val rawimg = new RawImageTool(width, height)
    for (fno <- 0 until frameno) rawimg.skipImage(in, psize)
    if (frameno > 0) println(s"Skipped to ${frameno}")

    if (psize == 4) rawimg.readImageInt(in)
    else if (psize == 1) rawimg.readImageByte(in)

    def shiftpixel(x: Int) : Double = {
      println(s"* shift starting at x=$x")
      val b = List.tabulate(nrows) {y => rawimg.getpxs(x, y+ybegin, nshifts)}
      val b2 = b.flatten
      val b3 = b2 map { v => if (v>=1024) 1023 else v}

      runtest(b3.map {BigInt(_)})
    }

    val ratios = List.tabulate(width/nshifts) {p =>
      shiftpixel(p*nshifts)}

    val sum = ratios.reduce(_+_)
    val mean = sum / ratios.length
    println("AverageCR="+mean)
  }
}

object Comp128Test {

  def run(args: Array[String]) {
    val (argsrest, opt) = TestUtil.getopts(args,
      Map("fn" -> "", "w" -> "0", "h" -> "0",
        "sz" -> "0", "fno" -> "0") )

    LocalParam.fn = opt("fn")
    LocalParam.w = opt("w").toInt
    LocalParam.h = opt("h").toInt
    LocalParam.sz = opt("sz").toInt
    LocalParam.fno = opt("fno").toInt

    val dut = () => new Comp128()
    val tester = c => new Comp128UnitTester(c)

    TestUtil.driverhelper(argsrest, dut, tester)
  }
}
