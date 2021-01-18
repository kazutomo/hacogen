//
// HACOGen test driver
//
// written by Kazutomo Yoshii <kazutomo.yoshii@gmail.com>
// 
package testmain

import onerowold._ // old one-row design. initial design when the shift and output strategy were not clear
import cprim._  // common primitves

import chisel3.iotesters
import chisel3.iotesters.{Driver, PeekPokeTester}

import testutil._

object Main extends App {

  if (args.length < 2) {
    println("Usage: testmain.Main command target [options]")
    println("")
    System.exit(1)
  }

  val args2 = args.drop(2) // drop the command and target

  cprim.BitShuffleTest.run(args2)

  // default params and component target list
  // key is the name of target module
  // value contains the run method function and the description
  val targetmap = Map(
    "BitShuffle"       -> (() => BitShuffleTest.run(args2), "cprim"),
    "Header"           -> (() => HeaderTest.run(args2), "old"),
    "Selector"         -> (() => SelectorTest.run(args2), "old"),
    "STBuf"            -> (() => STBufTest.run(args2), "old"),
    "Squeeze"          -> (() => SqueezeTest.run(args2), "old"),
    "SHComp"           -> (() => SHCompTest.run(args2), "old"),
    "Comp"             -> (() => CompTest.run(args2), "old")
    //"ConcatZeroStrip"  -> (() => ConcatZeroStripTest.run(args2), "concat zero strip"),
  )

  TestUtil.launch(args, targetmap)
}
