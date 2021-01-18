//
// written by Kazutomo Yoshii <kazutomo.yoshii@gmail.com>
//
package shred

import chisel3._
import chisel3.util._ // to use Cat()

// MMSortTwo: masked merge sort of two inputs. This module is a
// two-input (merge) sorting logic with respect to the zeroness of the
// signal value of each bus. This module converts two input buses into
// one input bus, where the bitwidth of each input bus is 'bw' bits
// while the output bus is '2*bw' bits.  It also generates a two-bit
// mask whose bit tells whether the signal values of each bus are all
// zero or not: '0' means all zeros and '1' for other cases. The low
// bit is associated with inA while the high bit is associated with
// inB.

class MMSortTwo(val bw:Int = 10) extends Module {
  val io = IO(new Bundle {
    val inA  = Input(UInt(bw.W))
    val inB  = Input(UInt(bw.W))
    val out  = Output(Vec(2,UInt(bw.W)))
    val outMask = Output(UInt(2.W)) // PopCount(outMask) to get len
  })

  val mask = Cat(io.inB.orR, io.inA.orR) // Cat(MSB, LSB)

  io.outMask := mask

  when ( mask === 2.U ) {
    io.out(0) := io.inB
    io.out(1) := 0.U(bw.W)
  } .otherwise {
    io.out(0) := io.inA
    io.out(1) := io.inB
  }
}
