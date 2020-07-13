//
// HACOGen tester
//
// written by Kazutomo Yoshii <kazutomo.yoshii@gmail.com>
//
package hacogen

import chisel3.iotesters
import chisel3.iotesters.{Driver, PeekPokeTester}
import scala.collection.mutable.ListBuffer
import localutil.Util._
import refcomp.RefComp._

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

  val seed = 100
  val r = new scala.util.Random(seed)
  val maxnzpxs = 4

  def geninputpxs() : List[Int] = {
    val npxs = r.nextInt(maxnzpxs)
    val pxidxs = List.tabulate(npxs) { _ => r.nextInt(nelems_src)}

    val tmp = List.tabulate(nelems_src) { i => if (pxidxs contains i) r.nextInt(7) +1 else 0}
    tmp
  }



  for (i <- 0 until 10) {
    // fill input

//    val testp = List.tabulate(nelems_src) (i => if(i==0) 1 else 0)
    val testp = geninputpxs()
    for(e <- testp) print(e + " ")
    println()

    for (j <- 0 until nelems_src)  poke(c.io.in(j), testp(j))

    step(1)
    for (k <- 0 until nelems_dst) {
      val tmp = peek(c.io.out(k))
      print(f"$tmp%02x ")
    }
    val bufpos = peek(c.io.bufpos)
    val flushed = peek(c.io.flushed)
    val flushedbuflen = peek(c.io.flushedbuflen)
    println(f"  p=$bufpos%x fl=$flushed/len=$flushedbuflen%x")
    val ref = shzsEncode(testp, elemsize_src)
    print("ref: ")
    for(e <- ref) print(f"$e%02x ")
    println("")
    println("")

  }

  println("")
  println("Currently no test is implemented...")
}
