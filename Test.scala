//
// HACOGen test driver
//
// written by Kazutomo Yoshii <kazutomo.yoshii@gmail.com>
// 
package hwcomp

import chisel3.iotesters
import chisel3.iotesters.{Driver, PeekPokeTester}

object TestMain extends App {
  val nelems_src = 8
  val nelems_dst = 16

  // component target list.
  val targetlist = List(
    "header",  "selector",  "squeeze",  "stbuf", "comp"
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
          chisel3.Driver.execute(args, () => new Header(nelems_src, nelems_dst) )
        case _ =>
          iotesters.Driver.execute(args, () => new Header(nelems_src, nelems_dst) )  { c => new HeaderUnitTester(c) }
      }

    case "selector" =>
      mode match {
        case "verilog" =>
          chisel3.Driver.execute(args, () => new Selector(nelems_src, nelems_dst) )
        case _ =>
          iotesters.Driver.execute(args, () => new Selector(nelems_src, nelems_dst) )  { c => new SelectorUnitTester(c) }
      }

    case "stbuf" =>
      mode match {
        case "verilog" =>
          chisel3.Driver.execute(args, () => new STBuf(nelems_src, nelems_dst) )
        case _ =>
          iotesters.Driver.execute(args, () => new STBuf(nelems_src, nelems_dst) )  { c => new STBufUnitTester(c) }
      }

    case "squeeze" =>
      mode match {
        case "verilog" =>
          chisel3.Driver.execute(args, () => new Squeeze(nelems_src, nelems_dst) )
        case _ =>
          iotesters.Driver.execute(args, () => new Squeeze(nelems_src, nelems_dst) )  { c => new SqueezeUnitTester(c) }
      }

    case _ =>
      mode match {
        case "verilog" =>
          chisel3.Driver.execute(args, () => new Comp(nelems_src, nelems_dst) )
        case _ =>
          iotesters.Driver.execute(args, () => new Comp(nelems_src, nelems_dst) )  { c => new CompUnitTester(c) }
      }
  }
}
