//
// Header module that generates a meta data
//
// written by Kazutomo Yoshii <kazutomo.yoshii@gmail.com>
//
package hacogen

import chisel3._
import chisel3.util.log2Ceil

class Header(val nelems:Int = 8, val elemsize:Int = 16) extends Module {
    val io = IO(new Bundle {
      val in  = Input(Vec(nelems, UInt(elemsize.W)))
      val out = Output(UInt(elemsize.W))
    })

  val metadata = VecInit.tabulate(nelems)(i => (io.in(i) =/= 0.U).asUInt << i)

  /*
  for(i <- 0 to nelems -1) {
    printf("%d: in=%d %d\n", i.U, io.in(i), metadata(i))
  }  */

  io.out := metadata.reduce(_ | _) // + (1.U << (elemsize-1).U)
}
