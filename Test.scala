//
// HACOGen test driver
//
// written by Kazutomo Yoshii <kazutomo.yoshii@gmail.com>
// 
package hwcomp

import chisel3.iotesters
import chisel3.iotesters.{Driver, PeekPokeTester}

object TestMain extends App {

  val modelist = List(
    "header", "selector", "squeeze", "stbuf", "default"
  )

  val mode = if (args.length > 0) args(0) else "default"

  val nelems_src = 8
  val nelems_dst = 16

  println("MODE=" + mode)

  mode match {
    case "list" =>
      println("*mode list")
      for (m <- modelist) {
        println(m)
      }
    // unit test
    case "header" =>
      println("@Header Test")
      iotesters.Driver.execute(args, () => new Header(nelems_src, nelems_dst) )  { c => new HeaderUnitTester(c) }
      //chisel3.Driver.execute(args, () => new Header(nelems_src, nelems_dst) ) // no randomization
    case "selector" =>
      println("@Selector Test")
      iotesters.Driver.execute(args, () => new Selector(nelems_src, nelems_dst) )  { c => new SelectorUnitTester(c) }
    case "stbuf" =>
      println("@STBuf Test")
      iotesters.Driver.execute(args, () => new STBuf(nelems_src, nelems_dst) )  { c => new STBufUnitTester(c) }
    case "squeeze" =>
      println("@Squeeze Test")
      iotesters.Driver.execute(args, () => new Squeeze(nelems_src, nelems_dst) )  { c => new SqueezeUnitTester(c) }
    // delay is just a simple Chisel example, not part of the compressor
    case "delay" =>
      println("@Delay Test")
      iotesters.Driver.execute(args, () => new Delay() )  { c => new DelayUnitTester(c) }
    // all compressor components integrated
    case _ =>
      println("@Main Comp Test")
      iotesters.Driver.execute(args, () => new Comp(nelems_src, nelems_dst) )  { c => new CompUnitTester(c) }
  }
}
