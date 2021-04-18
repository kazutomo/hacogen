//
// ShuffleMerge
//
// written by Kazutomo Yoshii <kazutomo.yoshii@gmail.com>
//

package shred

import chisel3._
import chisel3.util._


class Comp128() extends Module {
  val elemsize:Int = 10 // bits
  val nshifts:Int = 8 // pixels per cycle
  val nrows:Int = 128 // pixels
  val nrowsgroup: Int = 8 // pixels per group
  val ngroups: Int = nrows/nrowsgroup // 16 rows per group
  val nelemspergroup: Int = nrowsgroup * nshifts // 64
  // these are the output params
  val nblocks:Int = elemsize*(nrows/nrowsgroup) // 160 blocks
  val bwblock:Int = nelemspergroup // 64 bits

  val io = IO(new Bundle {
    val in  = Input(Vec(nrows*nshifts,  UInt(elemsize.W)))
    val out = Output(Vec(nblocks, UInt(bwblock.W)))
    val outmask = Output(UInt(nblocks.W))
  })

  // feed into 16 ShuffleMerge
  val shmerge = Array.fill(ngroups) {Module(new ShuffleMerge(nelemspergroup, elemsize))}

  for (i <- 0 until nrows*nshifts by nelemspergroup) {
    for (j <- 0 until nelemspergroup) {
      val gid = i/nelemspergroup
      shmerge(gid).io.in(j) := io.in(i+j)
    }
  }

  // 1st stage: eight concat with two 10-block input
  val cat2in = Array.fill(8) {Module(new ConcatZeroStrip(10, bwblock))}
  for (i <- 0 until 8) {
    cat2in(i).io.inA     := shmerge(i*2  ).io.out
    cat2in(i).io.inB     := shmerge(i*2+1).io.out
    cat2in(i).io.inAmask := shmerge(i*2  ).io.outmask
    cat2in(i).io.inBmask := shmerge(i*2+1).io.outmask
  }

  // 2nd stage: four concat with two 20-block input
  val cat4in = Array.fill(4) {Module(new ConcatZeroStrip(20, bwblock))}
  for (i <- 0 until 4) {
    cat4in(i).io.inA     := cat2in(i*2  ).io.out
    cat4in(i).io.inB     := cat2in(i*2+1).io.out
    cat4in(i).io.inAmask := cat2in(i*2  ).io.outmask
    cat4in(i).io.inBmask := cat2in(i*2+1).io.outmask
  }

  // 3rd stage: two concat with two 40-block input
  val cat8in = Array.fill(2) {Module(new ConcatZeroStrip(40, bwblock))}
  for (i <- 0 until 2) {
    cat8in(i).io.inA     := cat4in(i*2  ).io.out
    cat8in(i).io.inB     := cat4in(i*2+1).io.out
    cat8in(i).io.inAmask := cat4in(i*2  ).io.outmask
    cat8in(i).io.inBmask := cat4in(i*2+1).io.outmask
  }

  // the last stage: one concat with two 80-block input
  val lastcat = Module(new ConcatZeroStrip(80, bwblock))
  lastcat.io.inA     := cat8in(0).io.out
  lastcat.io.inB     := cat8in(1).io.out
  lastcat.io.inAmask := cat8in(0).io.outmask
  lastcat.io.inBmask := cat8in(1).io.outmask


  val reg_out = RegInit(VecInit(Seq.fill(nblocks)(0.U(bwblock.W))))
  val reg_outmask = RegInit(0.U(nblocks.W))

  reg_out := lastcat.io.out
  reg_outmask := lastcat.io.outmask

  io.out := reg_out
  io.outmask := reg_outmask

  //io.out := lastcat.io.out
  //io.outmask := lastcat.io.outmask
}
