//
// STBuf tester
//
// written by Kazutomo Yoshii <kazutomo.yoshii@gmail.com>
//
package onerowold

import chisel3.iotesters
import chisel3.iotesters.{Driver, PeekPokeTester}
import scala.collection.mutable.ListBuffer
import testutil._

class STBufSW(val nelems_src:Int = 8, val nelems_dst:Int = 16,
  val elemsize:Int = 16)
{
  private var dbuf = new Array[Int](nelems_dst)
  private var qbuf = new Array[Int](nelems_dst)

  for(i <- 0 until nelems_dst)  qbuf(i) = 0x55

  def insert(src: Array[Int], pos: Int, len : Int, flushed : Int) {
    for(i <- 0 until len) dbuf(pos+i) = src(i)

    for(i <- pos+len until nelems_dst)  dbuf(i) = 0x55
  }

  def getqbuf() : Array[Int] = qbuf

  def step() {
    qbuf = dbuf map(identity)
  }

  def printbuf() {
    if (true) {
      print("D  : ")
      dbuf.foreach(e => print(f"$e%2d "))
      println()
    }
    print("Q  : ")
    qbuf.foreach(e => print(f"$e%2d "))
    println()
  }
}


class STBufUnitTester(c: STBuf) extends PeekPokeTester(c) {

  var swstb = new STBufSW(c.nelems_src, c.nelems_dst, c.elemsize)

  // 99 here is a guard. should not be in the output
  // Note: tests array generation needs to be implemented for different input sizes
  // input data type
  class inT(val pos:Int, val len:Int, val flushed:Int, val data: List[Int])

  val rndtestgen = true

  var testinputs = List[inT]()


  if (rndtestgen) {
    var swsel = new SelectorSW(c.nelems_src, c.nelems_dst, c.elemsize)

    val seed = 100
    val r = new scala.util.Random(seed)

    // nelems_src is the number of input pixels
    // elemsize is the number of bits per pixel
    val headerlen = (c.nelems_src/c.elemsize).toInt + 1

    val ntests = 5 // the number of tests
    val maxval = 7 // maximum data length
    val nn = List.tabulate(ntests)(x => r.nextInt(maxval) + headerlen)

    var tmptestinputs = ListBuffer[inT]()

    for(nsrc <- nn) {
      val (swpos, swflushed, swflushedlen) = swsel.input(nsrc)
      val tmpdata = List.tabulate(nsrc)(x => r.nextInt(10)+1)
      tmptestinputs += (new inT(swpos, nsrc, swflushed, tmpdata))
      swsel.step()
    }
    tmptestinputs += (new inT(0, 0, 1, List(99)))
    testinputs = tmptestinputs.toList
  } else {
    /* for manual test inputs */
    testinputs = List(
    (new inT(0, 0, 0, List(99, 0, 0, 0, 0, 0, 0, 0))),
    (new inT(0, 1, 0, List(1, 99, 0, 0, 0, 0, 0, 0))),
    (new inT(1, 2, 0, List(2, 3, 99, 0, 0, 0, 0, 0))),
    (new inT(3, 3, 0, List(4, 5, 6, 99, 0, 0, 0, 0))),
    (new inT(0, 3, 1, List(7, 8, 9, 99, 0, 0, 0, 0))),
    (new inT(0, 1, 0, List(1, 99, 0, 0, 0, 0, 0, 0))),
    (new inT(1, 3, 0, List(2, 3, 4, 99, 0, 0, 0, 0))) )
  }

  def checkoutput(flushed: Int) {
    var idx = 0
    val swqbuf = swstb.getqbuf()
    print("OUT: ")
    for (i <- 0 until c.nelems_dst)  {
      if (flushed == 1)
        expect(c.io.dst(idx), swqbuf(i))
      val v = peek(c.io.dst(idx))
      //print(v + " ")
      print(f"$v%2d ")
      idx += 1
    }
    println()
    swstb.printbuf()
  }

  var flushedlen = 0

  for (t <- testinputs) {
    // input to both hardware and software implementation

    poke(c.io.pos, t.pos)
    poke(c.io.len, t.len)
    poke(c.io.flushed, t.flushed)
    println(f"pos=${t.pos} len=${t.len} flushed=${t.flushed}")
    print("IN : ")
    var idx = 0
    for (e <- t.data) {
      //print(e + " ")
      print(f"$e%2d")
      poke(c.io.src(idx), e)
      idx += 1
    }
    println()
    swstb.insert(t.data.toArray, t.pos, t.len, t.flushed)

    checkoutput(t.flushed)

    step(1)
    swstb.step()
  }
}

object STBufTest {
  def run(args: Array[String]) {
    val (argsrest, opt) = TestUtil.getopts(args,
      Map("n" -> "8", "ndst" -> "28", "bw" -> "9"))

    val dut = () => new STBuf(opt("n").toInt, opt("ndst").toInt, opt("bw").toInt)
    val tester = c => new STBufUnitTester(c)
    TestUtil.driverhelper(argsrest, dut, tester)
  }
}
