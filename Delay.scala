package hwcomp

import chisel3._

class Delay extends Module {
  val io = IO(new Bundle {
    val in  = Input(UInt(8.W))
    val out = Output(UInt(8.W))
  })
  val r0 = RegNext(io.in)
  val r1 = RegNext(r0)
  io.out := r1
}


