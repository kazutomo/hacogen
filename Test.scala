//
// HACOGen test driver
//
// written by Kazutomo Yoshii <kazutomo.yoshii@gmail.com>
// 
package hacogen

import chisel3.iotesters
import chisel3.iotesters.{Driver, PeekPokeTester}

object HacoGen extends App {
  // these params for shuffling input
  val shuffle_elemsize = 9
  val shuffle_nelems = 16

  // these params after shuffling
  /*
  val nelems_src = 9
  val nelems_dst = 28
  val elemsize = 16
   */
  val nelems_src = 8
  val nelems_dst = 28
  val elemsize = 9

  // component target list.
  val targetlist = List(
    "shuffle", "header",  "selector",  "squeeze",  "stbuf", "comp", "shcomp"
  )

  val a = if (args.length > 0) args(0) else "comp"
  val tmp = a.split(":")

  val target = tmp(0)
  val mode = if (tmp.length > 1) tmp(1) else "test"

  mode match {
    case "test" => {}
    case "verilog" => {}
    case _ => println(f"Warning: $mode is not a valid mode")
  }

  println(f"CONFIGS: nelems_src=$nelems_src  nelems_dst=$nelems_dst")
  println(f"MODE=$mode TARGET=$target")

  target match {

    case "list" =>
      println("*target list")
      for (t <- targetlist)  println(t)

    case "header" =>
      mode match {
        case "verilog" =>
          chisel3.Driver.execute(args, () => new Header(nelems_src, elemsize) )
        case _ =>
          iotesters.Driver.execute(args, () => new Header(nelems_src, elemsize) )  { c => new HeaderUnitTester(c) }
      }

    case "selector" =>
      mode match {
        case "verilog" =>
          chisel3.Driver.execute(args, () => new Selector(nelems_src, nelems_dst, elemsize) )
        case _ =>
          iotesters.Driver.execute(args, () => new Selector(nelems_src, nelems_dst, elemsize) )  { c => new SelectorUnitTester(c) }
      }

    case "stbuf" =>
      mode match {
        case "verilog" =>
          chisel3.Driver.execute(args, () => new STBuf(nelems_src, nelems_dst, elemsize) )
        case _ =>
          iotesters.Driver.execute(args, () => new STBuf(nelems_src, nelems_dst, elemsize) )  { c => new STBufUnitTester(c) }
      }

    case "squeeze" =>
      mode match {
        case "verilog" =>
          chisel3.Driver.execute(args, () => new Squeeze(nelems_src, elemsize) )
        case _ =>
          iotesters.Driver.execute(args, () => new Squeeze(nelems_src, elemsize) )  { c => new SqueezeUnitTester(c) }
      }

    case "shuffle" =>
      mode match {
        case "verilog" =>
          chisel3.Driver.execute(args, () =>   new BitShufflePerChannel(shuffle_nelems, shuffle_elemsize))
        case _ =>
          iotesters.Driver.execute(args, () => new BitShufflePerChannel(shuffle_nelems, shuffle_elemsize))  { c => new BitShufflePerChannelUnitTester(c) }
      }

    case "shcomp" =>
      mode match {
        case "verilog" =>
          chisel3.Driver.execute(args, () =>   new SHComp(shuffle_nelems, shuffle_elemsize, nelems_dst))
      }


    case _ =>
      mode match {
        case "verilog" =>
          chisel3.Driver.execute(args, () => new Comp(nelems_src, nelems_dst, elemsize) )
        case _ =>
          iotesters.Driver.execute(args, () => new Comp(nelems_src, nelems_dst, elemsize) )  { c => new CompUnitTester(c) }
      }
  }
}
