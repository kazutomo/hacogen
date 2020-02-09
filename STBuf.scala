//
// STBuf module
//
// written by Kazutomo Yoshii <kazutomo.yoshii@gmail.com>
//
package hacogen

import chisel3._
import chisel3.util.log2Ceil

class ShiftElems(val nelems:Int = 16, val elemsize:Int = 16) extends Module {
  val io = IO(new Bundle {
    val nshift = Input(UInt((log2Ceil(nelems)+1).W))
    val src = Input(Vec(nelems, UInt(elemsize.W)))
    val dst = Output(Vec(nelems, UInt(elemsize.W)))
  })

  for (i <- 0 to (nelems-1)) {
    when(i.U < io.nshift) {
      io.dst(i) := 0.U 
    } .otherwise {
      io.dst(i) := io.src(i.U - io.nshift)
    }
  }
}

// staging buffer
class STBuf(val nelems_src:Int = 8, val nelems_dst:Int = 16, val elemsize:Int = 16) extends Module {
  val io = IO(new Bundle {
    val src = Input(Vec(nelems_src+1, UInt(elemsize.W)))
    val pos = Input(UInt(log2Ceil(nelems_dst).W))
    val len = Input(UInt(log2Ceil(nelems_dst).W))
    val flushed = Input(Bool())
    val dst  = Output(Vec(nelems_dst, UInt(elemsize.W)))
  })

  val buf = RegInit(VecInit(Seq.fill(nelems_dst)(0x55.U(elemsize.W))))

  val sh = Module(new ShiftElems(nelems_dst, elemsize))

  val datapos = Wire(UInt((log2Ceil(nelems_dst)+1).W))
  val datalen = Wire(UInt((log2Ceil(nelems_dst)+1).W))
  datapos := io.pos
  datalen := io.len

  sh.io.nshift := datapos

  // expand src to nelems_dst, filling 0
  for (i <- 0 to ((nelems_src+1) - 1)) {
    sh.io.src(i) := io.src(i)
  }
  for (i <- (nelems_src+1) to (nelems_dst - 1) ) {
    sh.io.src(i) := 0.U
  }

  // only update dst[pos, pos+len)
  for (i <- 0 to (nelems_dst - 1) ) {
    when ( (i.U >= datapos) && (i.U < (datapos + datalen)) ) {
      buf(i) := sh.io.dst(i)
    } .otherwise {
      when (io.flushed) {
        when (i.U >= (datapos + datalen)) {
          buf(i) := 0x55.U  // 0x55 is tentative
        }
      } .otherwise {
        buf(i) := buf(i)
      }
    }
  }

  io.dst := buf
}

