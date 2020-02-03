package hwcomp

import chisel3._
import chisel3.util.log2Ceil

class Comp(val nelems_src:Int = 8, val nelems_dst:Int = 16, val elemsize:Int = 16) extends Module {
  val io = IO(new Bundle {
    val in  = Input(Vec(nelems_src, UInt(elemsize.W)))
    val out = Output(Vec(nelems_dst, UInt(elemsize.W)))

    // val flush = Input(Bool()) // to tell STbuf to flush data

    // ndata is not needed
    val ndata = Output(UInt((log2Ceil(nelems_src)+1).W))

    // below are debuginfo from Selector
    val bufsel = Output(UInt(1.W))
    val bufpos = Output(UInt(log2Ceil(nelems_dst).W))
    val flushed = Output(Bool())
    val flushedbuflen = Output(UInt(log2Ceil(nelems_dst).W))
  })

  val sqz   = Module(new Squeeze(nelems_src, elemsize))
  val hdr   = Module(new Header(nelems_src, elemsize))
  val sel   = Module(new Selector(nelems_src, nelems_dst, elemsize))
  val stbuf = Module(new STBuf(nelems_src, nelems_dst, elemsize))

  sqz.io.in := io.in
  hdr.io.in := io.in

  // data includes header plus compressed data
  val data = Wire(Vec(nelems_src+1, UInt(elemsize.W)))

  data(0) := hdr.io.out
  for (i <- 1 to nelems_src) {
    data(i) := sqz.io.out(i-1)
  }
  val ndata = Wire(UInt((log2Ceil(nelems_src)+1).W))
  ndata := sqz.io.ndata + 1.U

  io.ndata := ndata

  sel.io.nsrc := ndata // input to sel

  io.bufsel := sel.io.bufcursel
  io.bufpos := sel.io.bufcurpos // assign for test purpose
  io.flushed := sel.io.flushed   // flush now
  io.flushedbuflen := sel.io.flushedbuflen
  //
  stbuf.io.src := data  // sqz.io.out
  stbuf.io.pos := sel.io.bufcurpos
  stbuf.io.len := sel.io.nsrc
  stbuf.io.flushed := sel.io.flushed

  io.out := stbuf.io.dst
}
