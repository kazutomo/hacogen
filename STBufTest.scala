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

  val tests = Array(
    (0, 0, List(11, 0, 0, 0, 0, 0, 0, 0)),
    (0, 1, List(1, 11, 0, 0, 0, 0, 0, 0)),
    (1, 2, List(2, 3, 11, 0, 0, 0, 0, 0)),
    (3, 3, List(4, 5, 6, 11, 0, 0, 0, 0))
  )

  for (t <- tests) {
    poke(c.io.pos, t._1)
    poke(c.io.len, t._2)
    println(f"pos=${t._1} len=${t._2}")
    println("IN:")
    var idx = 0
    for (e <- t._3) {
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
