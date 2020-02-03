//
// Header module tester
//
// written by Kazutomo Yoshii <kazutomo.yoshii@gmail.com>
//
package hwcomp

import chisel3.iotesters
import chisel3.iotesters.{Driver, PeekPokeTester}

class HeaderUnitTester(c: Header) extends PeekPokeTester(c) {

  var r = new scala.util.Random(123)

  val nelem = 8

  for (i <- 0 to 10) {
    println("i="+i)

    val tmp = r.nextInt(1<<nelem)
    val tmpseq = Seq.tabulate(nelem)( x => tmp & (1 << x) )

    var idx = 0
    for (t <- tmpseq) {
      poke(c.io.in(idx), t)
      idx += 1
    }

    expect(c.io.out, tmp)

    def binstr(v : Int) : String = String.format("%8s", Integer.toBinaryString(v)).replace(' ', '0')

    val res = peek(c.io.out).toInt
    println("IN: " + binstr(tmp))
    println("OUT:" + binstr(res))

    step(1)
  }
}
