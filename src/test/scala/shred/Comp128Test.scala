package shred

import chisel3.util._
import chisel3.iotesters
import chisel3.iotesters.{Driver, PeekPokeTester}
import testutil._

class Comp128UnitTester(c: Comp128) extends PeekPokeTester(c) {
}

object Comp128Test {

  def run(args: Array[String]) {

    val dut = () => new Comp128()
    val tester = c => new Comp128UnitTester(c)

    TestUtil.driverhelper(args, dut, tester)
  }
}
