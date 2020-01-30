package hwcomp

import chisel3.iotesters
import chisel3.iotesters.{Driver, PeekPokeTester}

class DelayUnitTester(c: Delay) extends PeekPokeTester(c) {

  for (t <- 0 to 5) {
    poke(c.io.in, t)
    step(1)
    val r = peek(c.io.out)
    println(f"r=${r}")
  }
}
