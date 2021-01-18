//
// Squeeze tester
//
// written by Kazutomo Yoshii <kazutomo.yoshii@gmail.com>
//
package onerowold

import chisel3._
import chisel3.util._
import chisel3.iotesters._
import testutil._

class SqueezeUnitTester(c: Squeeze) extends PeekPokeTester(c) {

  def squeezed(l : List[Int]) : List[Int] = {
    var ret = List[Int]()
    for (e <- l if e != 0) ret = List(e) ::: ret
    ret.reverse ::: List.tabulate(c.nelems - ret.length)(dummy => 0)
  }

  val testpats = List(
    List(0, 0, 0, 0, 0, 0, 0, 1),
    List(0, 1, 0, 2, 3, 0, 0, 4),
    List(1, 2, 3, 4, 5, 6, 7, 8),
    List(0, 0, 0, 1, 0, 0, 2, 3),
    List(1, 0, 2, 0, 3, 0, 4, 0),
    List(0, 0, 0, 0, 0, 0, 0, 0)
  )

  for (l <- testpats) {
    var idx = 0

    print(s"in: ");
    for (a <- l) {
      print(s"$a  ")
      poke(c.io.in(idx), a)
      idx += 1
    }
    print(s"     out: ");

    val r = squeezed(l)
    for (i <- 0 until c.nelems) {
      val out = peek(c.io.out(i))
      print(s"$out  ")
      expect(c.io.out(i), r(i))
    }
    val ndata = peek(c.io.ndata)
    print(s"n=$ndata\n")

    step(1) // step(1) due to flush print msgs
  }
}


object SqueezeTest {
  def run(args: Array[String]) {
    val (argsrest, opt) = TestUtil.getopts(args,
      Map("n" -> 8, "bw" -> 9))

    val dut = () => new Squeeze(opt("n"), opt("bw"))
    val tester = c => new SqueezeUnitTester(c)
    TestUtil.driverhelper(argsrest, dut, tester)
  }
}
