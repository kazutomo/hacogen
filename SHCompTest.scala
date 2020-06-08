//
// HACOGen tester
//
// written by Kazutomo Yoshii <kazutomo.yoshii@gmail.com>
//
package hacogen

import chisel3.iotesters
import chisel3.iotesters.{Driver, PeekPokeTester}
import scala.collection.mutable.ListBuffer

class SHCompUnitTester(c: SHComp) extends PeekPokeTester(c) {
  // SHComp
  // val sh_nelems_src:Int = 16
  // val sh_elemsize:Int = 9
  // val nelems_dst:Int = 28

  println(f"sh_nelems_src=${c.sh_nelems_src}")
  println(f"sh_elemsize=${c.elemsize}")
  println(f"nelems_dst=${c.nelems_dst}")

  println("")
  println("implement this later")
  // test is empty now
}
