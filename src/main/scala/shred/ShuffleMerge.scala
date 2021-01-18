//
// ShuffleMerge
//
// written by Kazutomo Yoshii <kazutomo.yoshii@gmail.com>
//
package shred

import chisel3._
import chisel3.util._

import cprim._ // BitShuffle

class Reduction8(val nelems:Int = 64) extends Module {

  val elemsize = 8
  require(nelems >= elemsize && nelems <= 64) // upper limit is 64.

  val nblocks = elemsize // the number of blocks after BitShuffle
  val bwblock = nelems   // the bitwidth of each block after BitShuffle

  val io = IO(new Bundle {
    val in  = Input(Vec(nblocks,  UInt(bwblock.W)))
    val out = Output(Vec(nblocks, UInt(bwblock.W)))
    val outmask = Output(UInt(elemsize.W))
  })

  val msort = Array.fill(4) { Module(new MMSortTwo(bwblock)) }
  val concat2in = Array.fill(2) { Module(new ConcatZeroStrip(2, bwblock)) }
  val concat4in = Module(new ConcatZeroStrip(4, bwblock))

  for (i <- 0 until nblocks by 2) {
    val bidx = i/2
    msort(bidx).io.inA := io.in(i)
    msort(bidx).io.inB := io.in(i+1)
  }

  concat2in(0).io.inA     := msort(0).io.out
  concat2in(0).io.inAmask := msort(0).io.outMask
  concat2in(0).io.inB     := msort(1).io.out
  concat2in(0).io.inBmask := msort(1).io.outMask

  concat2in(1).io.inA     := msort(2).io.out
  concat2in(1).io.inAmask := msort(2).io.outMask
  concat2in(1).io.inB     := msort(3).io.out
  concat2in(1).io.inBmask := msort(3).io.outMask

  concat4in.io.inA     := concat2in(0).io.out
  concat4in.io.inAmask := concat2in(0).io.outmask
  concat4in.io.inB     := concat2in(1).io.out
  concat4in.io.inBmask := concat2in(1).io.outmask

  io.out := concat4in.io.out
  io.outmask := concat4in.io.outmask
}

class ShuffleMerge(val nelems:Int = 64, val elemsize:Int = 10) extends Module {
  val nblocks = elemsize // the number of blocks after BitShuffle
  val bwblock = nelems   // the bitwidth of each block after BitShuffle

  require(elemsize == 8 || elemsize == 10) // also support 10 later
  require(nelems > elemsize && nelems <= 64)

  val io = IO(new Bundle {
    val in  = Input( Vec(nelems,  UInt(elemsize.W)))
    val out = Output(Vec(nblocks, UInt(bwblock.W)))
    val outmask = Output(UInt(elemsize.W))
  })

  val sh = Module(new BitShuffle(nelems, elemsize))

  val red8 = Module(new Reduction8(nelems))

  sh.io.in := io.in

  if (nblocks == 8 ) {
    red8.io.in := sh.io.out
    io.out := red8.io.out
    io.outmask := red8.io.outmask
  } else { // nblock is 10

    // tentatively use this. zero padding to B
    val concat8in = Module(new ConcatZeroStrip(8, bwblock))
    val msort = Module(new MMSortTwo(bwblock))

    for (i <- 0 until 8) {
      red8.io.in(i) := sh.io.out(i)
      concat8in.io.inA(i) := red8.io.out(i)
      concat8in.io.inAmask := red8.io.outmask
    }
    msort.io.inA := sh.io.out(8)
    msort.io.inB := sh.io.out(9)

    concat8in.io.inB(0) := msort.io.out(0)
    concat8in.io.inB(1) := msort.io.out(1)

    val zeroblock = WireInit(0.U(bwblock.W))
    for (i <- 2 until 8) concat8in.io.inB(i) := zeroblock

    concat8in.io.inBmask := Cat(0.U(6.W), msort.io.outMask)

    // only 10 blocks are connected
    for (i <- 0 until 10)  io.out(i) := concat8in.io.out(i)
    io.outmask := concat8in.io.outmask(9, 0)
  }
}
