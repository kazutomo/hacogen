//
// Squeeze tester
//
// written by Kazutomo Yoshii <kazutomo.yoshii@gmail.com>
//
package hwcomp

import chisel3._
import chisel3.iotesters.PeekPokeTester

class SqueezeUnitTester(c: Squeeze) extends PeekPokeTester(c) {

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

    for (i <- 0 to 7) {
      val out = peek(c.io.out(i))
      print(s"$out  ")
    }
    val ndata = peek(c.io.ndata)
    print(s"n=$ndata\n")
    step(1) // step(1) due to flush print msgs
  }
}
