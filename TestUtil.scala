//
// utitilies for test codes
//
// written by Kazutomo Yoshii <kazutomo.yoshii@gmail.com>
// 
package testutil

import chisel3.iotesters
import chisel3.iotesters.{Driver, PeekPokeTester}
import chisel3.experimental._


object TestUtil {

  var verilogonly = false

  def launch(args: Array[String], targetmap: Map[String, (() => Unit, String)]) {

    val mode   = args(0)
    val target = args(1)
    val args2 = args.drop(2)

    def printlist() {
      println("*target list")
      for (t <- targetmap.keys) {
        printf("%-15s : %s\n", t, targetmap(t)._2)
      }
    }

    def checkfirstcharnocap(s: String, c: String) : Boolean = if (s.toLowerCase().substring(0,1) == c ) true else false

    // check see if only verilog output
    verilogonly = checkfirstcharnocap(mode, "v")

    if (checkfirstcharnocap(mode, "l")) {
      printlist()
      System.exit(0)
    }

    // find target module name match
    val matched = targetmap.keys.filter(
      _.toLowerCase.matches("^" + target.toLowerCase + ".*"))

    println()

    if (matched.size == 0) {
      println("No match found for " + target)
      printlist()
      println()

    } else if (matched.size > 1) {
      println("Multiple matches found for " + target)
      for (i <- matched) print(i + " ")
      println()
      printlist()
      println()
      System.exit(1)
    }

    val found = matched.toList(0)
    println(f"MODE=$mode TARGET=$found")

    // call the run() method of the found test object (e.g.,
    // FooTest.scal for Foo)
    targetmap(found)._1()
  }


  def getopts(args: Array[String], opts: Map[String, Int]) :
      (Array[String], Map[String, Int]) = {

    def nextopts(l: Array[String], m : Map[String, Int], res: Map[String, Int] ) :
        (Array[String], Map[String, Int], Map[String, Int]) = {
      /*
      print("debug nextopts()")
      print("l=")
      l foreach {v => print(v + " ")}
      println(" / m=" + m + " / res=" + res)
       */
      if (m.size > 0)  {
        val (k,v) = m.head
        if (l.length == 0) {
          nextopts(l, m.tail, res ++ Map(k -> v))
        } else {

          val pos = l.indexOf("-" + k)

          if (pos < 0 || pos+1 >= l.length) {
            nextopts(l, m.tail, res ++ Map(k -> v))
          } else {
            nextopts(l.patch(pos, Nil, 2), m.tail, res ++ Map(k -> l(pos+1).toInt))
          }
        }
      } else {
        (l, m, res)
      }
    }
    val (a, o, res) = nextopts(args, opts, Map[String, Int]())
    (a, res)
  }

  def test_getopts() {
    def runtest(args: Array[String], opts: Map[String,Int]) {
      println("[Input]")
      print("args: ")
      args foreach {v => print(v + " ")}
      println("\nopts: " + opts)

      val (a, res) = getopts(args, opts)
      println("[Output]")
      print("args: ")
      a foreach {v => print(v + " ")}
      println("\nopts: " + res)
      println()
    }

    runtest(Array(), Map())
    runtest(Array("rest"), Map())
    runtest(Array("rest"), Map("n" -> 3, "bw" -> 20))
    runtest(Array("-n", "12", "rest"), Map("n" -> 3, "bw" -> 20))
    runtest(Array("-z", "12", "rest"), Map("n" -> 3, "bw" -> 20))
    runtest(Array("-n", "12", "-bw", "80"), Map("n" -> 3, "bw" -> 20))
    runtest(Array("-n", "12", "-bw", "80", "rest"), Map("a" -> 11, "n" -> 3, "bw" -> 20))
  }

  def driverhelper[T <: MultiIOModule](args: Array[String], dutgen : () => T, testergen: T => PeekPokeTester[T]) {
    // Note: is chisel3.Driver.execute a right way to generate
    // verilog, or better to use (new ChiselStage).emitVerilog()?
    if (verilogonly) chisel3.Driver.execute(args, dutgen)
    else           iotesters.Driver.execute(args, dutgen) {testergen}
  }

  // convenient functions

  def convIntegerToBinStr(v : BigInt,  nbits: Int) = {
    val s = v.toString(2)
    val l = s.length
    val leadingzeros = "0" * (if (nbits > l) nbits-l else 0)
    leadingzeros + s
  }

  def convIntegerToHexStr(v : BigInt,  nbits: Int) = {
    val s = v.toString(16)
    val l = s.length
    val maxnhexd = nbits/4 + (if ((nbits%4)>0) 1 else 0)
    val leadingzeros = "0" * (if (maxnhexd > l) maxnhexd-l else 0)

    leadingzeros + s
  }

  // below are going to be obsolete
  def intToBinStr(v : Int,  nbits: Int) = f"%%0${nbits}d".format(v.toBinaryString.toInt)

  def convLongToBinStr(v : Long,  nbits: Int) = {
    val s = v.toBinaryString
    val l = s.length
    val leadingzeros = "0" * (if (nbits > l) nbits-l else 0)

    leadingzeros + s
  }

  def convLongToHexStr(v : Long,  nbits: Int) = {
    val s = v.toHexString
    val l = s.length
    val maxnhexd = nbits/4 + (if ((nbits%4)>0) 1 else 0)
    val leadingzeros = "0" * (if (maxnhexd > l) maxnhexd-l else 0)

    leadingzeros + s
  }

  // This function search an option that is a combination of an option
  // string and an integer value in args (e.g., -len 10). opt excludes
  // '-'.  dval is the default value. A calling example is
  // getoptint(args, "len", 16). If '-len' is found, the returned args
  // is the same as the input args and optval is dval. If found, the
  // returned args does not include '-len INT' and optval is INT.
  def getoptint(args: Array[String], opt: String, dval: Int) :
      (Array[String], Int) = {

    val hopt = "-" + opt
    val pos = args.indexOf(hopt)

    if(pos < 0 || pos+1 >= args.length) return (args, dval)

    return (args.patch(pos, Nil, 2),  args(pos+1).toInt)
  }
}
