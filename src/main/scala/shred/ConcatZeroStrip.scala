//
// written by Kazutomo Yoshii <kazutomo.yoshii@gmail.com>
//
package shred

import chisel3._
import chisel3.util._

// This logic concatenates the non-zero contents of inA and inB.
// It takes two input buses (inA and inB), where each input
// bus has a vector of integer signals whose length is 'nelems', and
// generates one input bus whose length is 'nelems*2'.  The contents
// of each input bus are already sorted in the previous stage and
// non-zero-value elements are followed by zero-value elements.  When
// all elements of both inputs are non-zero, the output simply
// concatenates two N-element inputs to one (N*2)-element output.

class ConcatZeroStrip(val nelems:Int = 2, val bw:Int = 10) extends Module {
  require( bw <= 64)

  val nelems_out = nelems * 2 // for convenience

  val io = IO(new Bundle {
    val inA  = Input(Vec(nelems, UInt(bw.W))) // the contents are sorted
    val inB  = Input(Vec(nelems, UInt(bw.W)))
    val inAmask = Input(UInt(nelems.W))
    val inBmask = Input(UInt(nelems.W))
    val out  = Output(Vec(nelems_out,  UInt(bw.W))) // the contents are sorted
    val outmask = Output(UInt(nelems_out.W))
    // input and output masks retain the original location of non-zero elements
  })

  // linearize the twoinputs, combining A and B into a single vector
  // of wires.
  val inAB = Wire(Vec(nelems_out+1, UInt(bw.W)))
  for (i <- 0 until nelems) {
    inAB(i)        := io.inA(i)
    inAB(i+nelems) := io.inB(i)
  }
  inAB(nelems_out) := 0.U

  // pop count of mask bits, which can tell us the length of non-zero
  // elements, assuming the elements associated to this mask are sorted
  val popcA = PopCount(io.inAmask)
  val popcB = PopCount(io.inBmask)

  // create the output mask, simply concatenating two masks, repecting the order
  io.outmask := Cat(io.inBmask, io.inAmask)  // Cat(MSB, LSB)

  // The contents of inB is shifted only when inA is partially
  // occupied and inB has at least one non-zero elem. If all the
  // elements of inA is non-zero, we don't need to shift.
  when ( popcA =/= nelems.U && popcB =/= 0.U ) {

    // this function returns a list that includes the index of wire
    // for each condition for target muxid.
    def createMuxLookupList(muxid : Int) : List[Int]  = {
      List.tabulate(nelems+1) {j =>
        if (muxid < nelems) { if (j<(nelems-muxid)) muxid         else j+muxid }
        else                { if ((j+muxid) < nelems_out) j+muxid else nelems_out}
      }
    }

    val nshift = nelems.U - popcA

    for (i <- 0 until nelems_out)  {
      val lookuplist = createMuxLookupList(i)

      val lookups = lookuplist.zipWithIndex.map { case (wireidx, sel) => sel.U -> inAB(wireidx) }

      io.out(i) := MuxLookup(nshift, inAB(i), lookups)
    }

    // testing this mux lookup algorithm
    // List.tabulate(n*2) { i =>  List.tabulate(n+1) {j => if (i<n) { if (j<(n-i)) i else j+i } else { if ((j+i) < n*2) j+i else n*2 -1} } }

  } .otherwise {
    // no shift is required, simply wiring up. since shift 0 is direct connection, this is not needed?
    for (i <- 0 until nelems_out)  io.out(i) := inAB(i)
  }
}
