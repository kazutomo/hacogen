//
// Header module tester
//
// written by Kazutomo Yoshii <kazutomo.yoshii@gmail.com>
//
package onerowold

import chisel3.iotesters
import chisel3.iotesters.{Driver, PeekPokeTester}
import testutil._

class HeaderUnitTester(c: Header) extends PeekPokeTester(c) {

  var r = new scala.util.Random(123)

  val nelem = c.nelems

  val ntests = 10

  for (i <- 0 until ntests) {
    println("i="+i)

    val tmp = r.nextInt(1<<nelem)
    val tmpseq = Seq.tabulate(nelem)( x => tmp & (1 << x) )

    var idx = 0
    for (t <- tmpseq) {
      poke(c.io.in(idx), t)
      idx += 1
    }

    expect(c.io.out(0), tmp)

    def binstr(v : Int) : String = String.format("%8s", Integer.toBinaryString(v)).replace(' ', '0')

    val res = peek(c.io.out(0)).toInt
    println("IN: " + binstr(tmp))
    println("OUT:" + binstr(res))

    step(1)
  }
}


object HeaderTest {
  def run(args: Array[String]) {
    val (argsrest, opt) = TestUtil.getopts(args,
      Map("n" -> "8", "bw" -> "9"))

    val dut = () => new Header(opt("n").toInt, opt("bw").toInt)
    val tester = c => new HeaderUnitTester(c)
    TestUtil.driverhelper(argsrest, dut, tester)
  }
}
