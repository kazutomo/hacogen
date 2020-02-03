//
// STBuf tester
//
// written by Kazutomo Yoshii <kazutomo.yoshii@gmail.com>
//
package hwcomp

import chisel3.iotesters
import chisel3.iotesters.{Driver, PeekPokeTester}

class STBufUnitTester(c: STBuf) extends PeekPokeTester(c) {

  def printdst(c: STBuf) {
    var idx = 0
    for (i <- 0 to 7)  {
      val v = peek(c.io.dst(idx))
      print(v + " ")
      idx += 1
    }
    println()
  }

  // _1: insert position
  // _2: the number of elements
  // _3: flushed
  // 99 here is a guard. should not be in the output
  val tests = Array(
    (0, 0, 0, List(99, 0, 0, 0, 0, 0, 0, 0)),
    (0, 1, 0, List(1, 99, 0, 0, 0, 0, 0, 0)),
    (1, 2, 0, List(2, 3, 99, 0, 0, 0, 0, 0)),
    (3, 3, 0, List(4, 5, 6, 99, 0, 0, 0, 0)),
    (0, 3, 1, List(7, 8, 9, 99, 0, 0, 0, 0)),
    (0, 0, 0, List(0, 0, 0, 0,  0, 0, 0, 0))
  )

  for (t <- tests) {
    poke(c.io.pos, t._1)
    poke(c.io.len, t._2)
    poke(c.io.flushed, t._3)
    println(f"pos=${t._1} len=${t._2} flushed=${t._3}")
    println("IN:")
    var idx = 0
    for (e <- t._4) {
      print(e + " ")
      poke(c.io.src(idx), e)
      idx += 1
    }
    println("OUT:")
    printdst(c)
    step(1)
  }
  println("OUT:")
  printdst(c)
}
