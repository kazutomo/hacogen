package hwcomp

import chisel3._
import chisel3.util.log2Ceil

// NOTE:
// a signal to flush an incomplete buf (e.g., the end of the streaming)
//

class Selector(val nelems_src:Int = 8, val nelems_dst:Int = 16, elemsize:Int = 16) extends Module {
  val io = IO(new Bundle {
    // the number of valid elements in src is variable (0..nsrc)
    //val src  = Input(Vec(nelems_src, UInt(elemsize.W)))
    val nsrc = Input(UInt((log2Ceil(nelems_src)+1).W))
    //
    //val dst  = Output(Vec(nelems_dst, UInt(elemsize.W)))
    val bufcursel = Output(UInt(1.W))
    val bufcurpos = Output(UInt(log2Ceil(nelems_dst).W))
    val bufsel = Output(UInt(1.W)) // use when flush
    val bufpos = Output(UInt(log2Ceil(nelems_dst).W)) // XXX: probably not needed
    val flushed = Output(Bool())
    val flushedbuflen = Output(UInt(log2Ceil(nelems_dst).W))
  })

  // the current status
  val bufsel_reg = RegInit(0.U(1.W))
  // note (log2Ceil(nelems_dst)+1) to make the following comparison work.
  // without +1, flush never becomes true
  val bufpos_reg = RegInit(0.U((log2Ceil(nelems_dst)+1).W))

  io.bufcursel := bufsel_reg
  io.bufcurpos := bufpos_reg

  val flushed = ((bufpos_reg + io.nsrc) >= nelems_dst.U)

  when (flushed) {
    io.bufsel := bufsel_reg + 1.U
    io.bufpos := 0.U
    io.flushedbuflen := bufpos_reg
  } .otherwise {
    io.bufsel := bufsel_reg
    io.bufpos := bufpos_reg + io.nsrc
    io.flushedbuflen := 0.U
  }

  bufsel_reg := io.bufsel
  bufpos_reg := io.bufpos

  io.flushed := flushed
}
