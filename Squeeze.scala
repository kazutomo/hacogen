package hwcomp

import chisel3._
import chisel3.util.log2Ceil

// to covert a sparse data into a dense data one by one
// when if in(pos) has zero, shift up the rest of data; remove the in(pos)
class ShiftUp(val nelems:Int = 8, elemsize:Int = 16) extends Module {
  val io = IO(new Bundle {
    val pos    = Input(UInt((log2Ceil(nelems)+1).W))
    val in     = Input(Vec(nelems, UInt(elemsize.W)))
    val posout = Output(UInt((log2Ceil(nelems)+1).W))
    val out    = Output(Vec(nelems, UInt(elemsize.W)))
  })

  val sel = (io.in(io.pos) === 0.U)   // === returns Chisel Bool

  when (sel) {
    io.posout := io.pos
  } .otherwise {
    io.posout := io.pos + 1.U
  }

  //printf("pos=%d => %d\n", io.pos, io.posout)

  for (i <- 0 to nelems - 2) {
    when( (i.U >= io.pos) & sel ) {
      io.out(i) := io.in(i+1)
    } .otherwise {
      io.out(i) := io.in(i)
    }
  }

  io.out(nelems-1) := Mux(sel, 0.U, io.in(nelems-1))
}

class Squeeze(val nelems:Int = 8, elemsize:Int = 16) extends Module {
    val io = IO(new Bundle {
      val in  = Input(Vec(nelems, UInt(elemsize.W)))
      val out = Output(Vec(nelems, UInt(elemsize.W)))
      val ndata = Output(UInt((log2Ceil(nelems)+1).W))
    })

  val pos = Wire(UInt((log2Ceil(nelems)+1).W))
  pos := 0.U

  val s = Array.fill(nelems) { Module(new ShiftUp(nelems, elemsize)) }

  s(0).io.pos := pos
  s(0).io.in  := io.in
  for (i <- 1 to (nelems-1)) {
    s(i).io.pos := s(i-1).io.posout
    s(i).io.in  := s(i-1).io.out
  }
  io.ndata := s(nelems-1).io.posout
  io.out   := s(nelems-1).io.out
}
