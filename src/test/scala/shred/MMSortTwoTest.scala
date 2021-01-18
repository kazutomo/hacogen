package shred

import chisel3.util._
import chisel3.iotesters
import chisel3.iotesters.{Driver, PeekPokeTester}
import testutil._
import testutil.TestUtil.{convLongToBinStr, convLongToHexStr}

class MMSortTwoUnitTester(c: MMSortTwo) extends PeekPokeTester(c) {

  val bitwidth = c.bw
  val maxval = (BigInt(1) << bitwidth) - BigInt(1)

  //val testpats = List((0,0), (0,3), (1,0), (1,3))
  val testpats : List[(BigInt, BigInt)]= List((0,0), (0,maxval), (maxval,0), (maxval,maxval))

  def refAB(a: BigInt, b: BigInt) : (BigInt, BigInt) = {
    if ( a == 0 )  (b, BigInt(0))
    else           (a, b)
  }
  def refMask(a: BigInt, b: BigInt) : Int = {
    (if(a>0) 1 else 0) + (if(b>0) 2 else 0)
  }

  for (tp <- testpats) {
    println()
    poke(c.io.inA, tp._1)
    poke(c.io.inB, tp._2)

    val outLow = peek(c.io.out(0)).toLong
    val outHigh = peek(c.io.out(1)).toLong
    val outMask = peek(c.io.outMask).toLong

    printf("B=" + convLongToHexStr(tp._2.toLong, bitwidth))
    printf(" A=" + convLongToHexStr(tp._1.toLong, bitwidth))

    println(" => " +
      convLongToHexStr(outHigh, bitwidth) + " " +
      convLongToHexStr(outLow,  bitwidth) + " Mask=" +
      convLongToBinStr(outMask, 2)
    )

    val (refA, refB) = refAB(tp._1, tp._2)
    expect(c.io.out(0), refA)
    expect(c.io.out(1), refB)
    expect(c.io.outMask, refMask(tp._1, tp._2))
  }

  printf("Note: mask MSB is inB and LSB is outA\n")
}

object MMSortTwoTest {
  def run(args: Array[String]) {
    val bitwidth = 64
    val dut = () => new MMSortTwo(bitwidth)
    val tester = c => new MMSortTwoUnitTester(c)
    TestUtil.driverhelper(args, dut, tester)
  }
}
