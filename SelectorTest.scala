package hwcomp

import chisel3._
import chisel3.iotesters.PeekPokeTester

class SelectorUnitTester(c: Selector) extends PeekPokeTester(c) {

  //val r = new scala.util.Random(100)

  val nn = List(4,4,4,4,4)
  //val nn = List(8,6,6,6)

//  for (i <- 0 to 10) {
//    val nsrc = r.nextInt(10)
  for(nsrc <- nn) {
    poke(c.io.nsrc, nsrc)
    val bufsel = peek(c.io.bufsel)
    val bufpos = peek(c.io.bufpos)
    val flushed   = peek(c.io.flushed)
    val flushedbuflen  = peek(c.io.flushedbuflen)
    if (flushed > 0 )
      print(s"nsrc=$nsrc => sel=$bufsel pos=$bufpos flushedbuflen=$flushedbuflen\n")
    else
      print(s"nsrc=$nsrc => sel=$bufsel pos=$bufpos\n")

    step(1)
  }
}
