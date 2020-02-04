//
// Selector module that updates buffer position and emits a buffer flush signal
//
// written by Kazutomo Yoshii <kazutomo.yoshii@gmail.com>
//
package hacogen

import chisel3._
import chisel3.util.log2Ceil

// NOTE:
// a signal to flush an incomplete buf (e.g., the end of the streaming)
//

class Selector(val nelems_src:Int = 8, val nelems_dst:Int = 16, val elemsize:Int = 16) extends Module {
  val io = IO(new Bundle {
    // the number of valid elements in src is variable (0..nsrc)
    //val src  = Input(Vec(nelems_src, UInt(elemsize.W)))
    val nsrc = Input(UInt((log2Ceil(nelems_src)+1).W))
    //
    //val dst  = Output(Vec(nelems_dst, UInt(elemsize.W)))
    val bufcursel = Output(UInt(1.W))
    val bufcurpos = Output(UInt((log2Ceil(nelems_dst)+1).W))
    val flushed = Output(Bool())
    val flushedbuflen = Output(UInt((log2Ceil(nelems_dst)+1).W))
  })

  // the current status
  val bufsel_reg = RegInit(0.U(1.W))
  // note (log2Ceil(nelems_dst)+1) to make the following comparison work.
  // without +1, flush never becomes true
  val bufpos_reg = RegInit(0.U((log2Ceil(nelems_dst)+1).W))

  val tmpbufsel = Wire(UInt(1.W))
  val tmpbufpos = Wire(UInt((log2Ceil(nelems_dst)+1).W))


  val posupdated = Wire(UInt((log2Ceil(nelems_dst)+1).W))

  posupdated := io.nsrc + bufpos_reg

  val flushed = (posupdated > nelems_dst.U)

  //printf("posupdated=%d flused=%d %d\n", posupdated, flushed, bufpos_reg)

  when (flushed) {
    tmpbufsel := bufsel_reg + 1.U
    tmpbufpos := io.nsrc

    io.flushedbuflen := bufpos_reg
    io.bufcursel := bufsel_reg
    io.bufcurpos := 0.U
  } .otherwise {
    tmpbufsel := bufsel_reg
    tmpbufpos := bufpos_reg + io.nsrc
    io.flushedbuflen := 0.U
    io.bufcursel := bufsel_reg
    io.bufcurpos := bufpos_reg
  }

  bufsel_reg := tmpbufsel
  bufpos_reg := tmpbufpos

  io.flushed := flushed
}
