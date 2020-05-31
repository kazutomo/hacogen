//
// HACOGen top-level module
//
// written by Kazutomo Yoshii <kazutomo.yoshii@gmail.com>
//
package hacogen

import chisel3._
import chisel3.util.log2Ceil

class SHComp(
  val sh_nelems_src:Int = 16,
  val sh_elemsize:Int = 9,
  // below are params for COMP
  val nelems_src:Int = 9, val nelems_dst:Int = 28,
  val elemsize:Int = 16) extends Module {
  val io = IO(new Bundle {
    val in  = Input(Vec(sh_nelems_src, UInt(sh_elemsize.W)))
    val out = Output(Vec(nelems_dst, UInt(elemsize.W)))

    // val flush = Input(Bool()) // to tell STbuf to flush data

    // ndata is not needed
    val ndata = Output(UInt((log2Ceil(nelems_src)+1).W))

    // below are debuginfo from Selector
    val bufsel = Output(UInt(1.W))
    val bufpos = Output(UInt(log2Ceil(nelems_dst).W))
    val flushed = Output(Bool())
    val flushedbuflen = Output(UInt(log2Ceil(nelems_dst+1).W))
  })

  val sfl   = Module(new BitShufflePerChannel(sh_nelems_src, sh_elemsize))

  val sqz   = Module(new Squeeze(nelems_src, elemsize))
  val hdr   = Module(new Header(nelems_src, elemsize))
  val sel   = Module(new Selector(nelems_src, nelems_dst, elemsize))
  val stbuf = Module(new STBuf(nelems_src, nelems_dst, elemsize))

  sfl.io.in := io.in

  //sqz.io.in := io.in
  //hdr.io.in := io.in
  sqz.io.in := sfl.io.out
  hdr.io.in := sfl.io.out


  // data includes header plus compressed data
  val data = Wire(Vec(nelems_src+1, UInt(elemsize.W)))

  data(0) := hdr.io.out(0) // XXX: fix this to support multiple headers
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
