//
// Selector tester
//
// written by Kazutomo Yoshii <kazutomo.yoshii@gmail.com>
//
package hacogen

import chisel3._
import chisel3.iotesters.PeekPokeTester

class SelectorUnitTester(c: Selector) extends PeekPokeTester(c) {

  val r = new scala.util.Random(100)

  //  val nn = List(4,4,4,4,4)
  val ndata = 32
  val maxval = 4
  val nn = List.tabulate(ndata)(x => r.nextInt(maxval) + 1)

  for(nsrc <- nn) {
    poke(c.io.nsrc, nsrc)
    val bufsel = peek(c.io.bufcursel)
    val bufpos = peek(c.io.bufcurpos)
    val flushed   = peek(c.io.flushed)
    val flushedbuflen  = peek(c.io.flushedbuflen)
    if (flushed > 0 )
      print(s"nsrc=$nsrc => sel=$bufsel pos=$bufpos fblen=$flushedbuflen\n")
    else
      print(s"nsrc=$nsrc => sel=$bufsel pos=$bufpos\n")

    step(1)
  }
}
