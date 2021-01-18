//
// HACOGen test driver
//
// written by Kazutomo Yoshii <kazutomo.yoshii@gmail.com>
// 
package launcher

import onerowold._ // old one-row design. initial design when the shift and output strategy were not clear
import cprim._  // common primitves
import shred._  // multi-row shuffle-reduce design

import testutil._
import refcomp._ // reference and estimator

import chisel3.iotesters
import chisel3.iotesters.{Driver, PeekPokeTester}


object Main extends App {

  if (args.length < 2) {
    println("Usage: testmain.Main command target [options]")
    println("")
    System.exit(1)
  }

  val args2 = args.drop(2) // drop the command and target

  if (TestUtil.checkfirstcharnocap(args(0), "e")) {
    println("calling estimator")
    args.drop(1) foreach {println(_)}
    Estimator.run(args.drop(1))
    // EstimatorPrev.run(args.drop(1)) // to invoke the prev version
    System.exit(0)
  }

  // default params and component target list
  // key is the name of target module
  // value contains the run method function and the description
  val targetmap = Map(
    "BitShuffle"      -> (() => BitShuffleTest.run(args2), "cprim"),
    "MMSortTwo"       -> (() => MMSortTwoTest.run(args2), "shred"),
    "ConcatZeroStrip" -> (() => ConcatZeroStripTest.run(args2), "shred"),
    "ShuffleMerge"    -> (() => ShuffleMergeTest.run(args2), "shred"),
    "Comp128"         -> (() => Comp128Test.run(args2), "shred"),
    "Header"          -> (() => HeaderTest.run(args2), "onerowold"),
    "Selector"        -> (() => SelectorTest.run(args2), "onerowold"),
    "STBuf"           -> (() => STBufTest.run(args2), "onerowold"),
    "Squeeze"         -> (() => SqueezeTest.run(args2), "onerowold"),
    "SHComp"          -> (() => SHCompTest.run(args2), "onerowold"),
    "Comp"            -> (() => CompTest.run(args2), "onerowold")
  )

  TestUtil.launch(args, targetmap)
}
