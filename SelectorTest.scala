//
// Selector tester
//
// written by Kazutomo Yoshii <kazutomo.yoshii@gmail.com>
//
package hacogen

import chisel3._
import chisel3.iotesters.PeekPokeTester
import testutil._

class SelectorSW(val nelems_src:Int = 8, val nelems_dst:Int = 16, val elemsize:Int = 16) {
  private var dpos : Int = 0
  private var qpos : Int = 0

  // return (pos, flushed, flushedlen)
  def input(n: Int) : (Int, Int, Int) = {
    var flushedlen : Int = 0
    var tmppos : Int = 0
    var retpos : Int = 0
    val flushed = if ((qpos+n) > nelems_dst) 1 else 0

    if (flushed == 1) {
      tmppos = n
      flushedlen = qpos
      retpos = 0
    } else {
      tmppos = qpos + n
      flushedlen = 0
      retpos = qpos
    }

    dpos = tmppos

    (retpos, flushed, flushedlen)
  }

  def step() { qpos = dpos }
}

class SelectorUnitTester(c: Selector) extends PeekPokeTester(c) {

  // A software implementation of Selector
  var swsel = new SelectorSW(c.nelems_src, c.nelems_dst, c.elemsize)

  val seed = 100
  val r = new scala.util.Random(seed)

  // nelems_src is the number of input pixels
  // elemsize is the number of bits per pixel
  val headerlen = (c.nelems_src/c.elemsize).toInt + 1

  val ntests = 20
  val maxval = 4
  val nn = List.tabulate(ntests)(x => r.nextInt(maxval) + headerlen)

  for(nsrc <- nn) {

    // input to both hardware and software implementation
    poke(c.io.nsrc, nsrc)
    val (swpos, swflushed, swflushedlen) = swsel.input(nsrc)
    // println(f"sw: pos=$swpos flushed=$swflushed flushedlen=$swflushedlen")
    // val bufsel = peek(c.io.bufcursel) // bufsel is unused
    val bufpos = peek(c.io.bufcurpos)
    val flushed   = peek(c.io.flushed)
    val flushedbuflen  = peek(c.io.flushedbuflen)

    print(s"nsrc=$nsrc => pos=$bufpos flushed=$flushed fblen=$flushedbuflen\n")

    expect(c.io.bufcurpos, swpos)
    expect(c.io.flushed, swflushed)
    expect(c.io.flushedbuflen, swflushedlen)

    step(1)
    swsel.step()
  }
}

object SelectorTest {
  def run(args: Array[String]) {
    val (argsrest, opt) = TestUtil.getopts(args,
      Map("n" -> 8, "ndst" -> 28, "bw" -> 9))

    val dut = () => new Selector(opt("n"), opt("ndst"), opt("bw"))
    val tester = c => new SelectorUnitTester(c)
    TestUtil.driverhelper(argsrest, dut, tester)
  }
}
