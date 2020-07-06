//
// HACOGen tester
//
// written by Kazutomo Yoshii <kazutomo.yoshii@gmail.com>
//
package hacogen

import chisel3.iotesters
import chisel3.iotesters.{Driver, PeekPokeTester}
import scala.collection.mutable.ListBuffer

class SHCompUnitTester(c: SHComp) extends PeekPokeTester(c) {
  // SHComp
  // val sh_nelems_src:Int = 16
  // val sh_elemsize:Int = 9
  // val nelems_dst:Int = 28

  val nelems_src   = c.sh_nelems_src
  val elemsize_src = c.sh_elemsize
  val nelems_dst   = c.nelems_dst
  val elemsize_dst = c.sh_nelems_src

  println(f"nelems_src   = ${nelems_src}")
  println(f"elemsize_src = ${elemsize_src}")
  println(f"nelems_dst   = ${nelems_dst}")
  println(f"elemsize_dst = ${elemsize_dst}")

  // TODO: fill an actual test here after CompTest gets updated
  //

  // SHComp:
  //
  // val in  = Input(Vec(sh_nelems_src, UInt(sh_elemsize.W)))
  // val out = Output(Vec(nelems_dst, UInt(elemsize.W)))
  //
  // below are debuginfo
  // val bufsel = Output(UInt(1.W))
  // val bufpos = Output(UInt(log2Ceil(nelems_dst).W))
  // val flushed = Output(Bool())
  // val flushedbuflen = Output(UInt(log2Ceil(nelems_dst+1).W))
  //

  for (i <- 0 until 10) {
    // fill input
    for (j <- 0 until nelems_src) {
      poke(c.io.in(j), if (j == 0) 1 else 0)
    }
    step(1)
    for (k <- 0 until nelems_dst) {
      val tmp = peek(c.io.out(k))
      print(f"$tmp%02x ")
    }
    val bufpos = peek(c.io.bufpos)
    val flushed = peek(c.io.flushed)
    val flushedbuflen = peek(c.io.flushedbuflen)
    println(f"  p=$bufpos%x fl=$flushed/len=$flushedbuflen%x\n")
  }

  println("")
  println("Currently no test is implemented...")
}