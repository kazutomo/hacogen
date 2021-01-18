//
// Compression ratio estimator
//
// written by Kazutomo Yoshii <kazutomo.yoshii@gmail.com>
//
// scala -classpath classes EstimatorMain
//
// Note: The previous estimator is moved to EstimatorPrev.scala hope
// it is still compatible. scala -classpath classes EstimatorMainV1.
// If you want to use the exact same code, use 9b9b7022e for the
// previous version (used for the JINST paper). Usage examples
// (analyze.sh) in my local repo.
//

package refcomp

import Array._
import scala.collection.mutable.ListBuffer
import java.io._

// local claases
import refcomp.Util._
import refcomp.RefComp._

//import rawimagetool._
//import localutil.Util._
//import refcomp.RefComp._
//import estimator._ // AppParams

// without postfixOps, a warning msg will be shown for the following line
// rowidx => rawimg.getpxs(x, y + rowidx, ap.ncolshifts) toList
import scala.language.postfixOps

//object EstimatorMain extends App {
object Estimator {

  // NOTE: indent inside run() later
  def run(args: Array[String]) {
    args foreach {println(_)}

  var ap = EstimatorAppParams
  ap.getopts(args)
  ap.printinfo()

  // open dataset
  val in = new FileInputStream(ap.filename)
  val rawimg = new RawImageTool(ap.width, ap.height)

  for (fno <- 0 until ap.fnostart) rawimg.skipImage(in, ap.psize)
  if (ap.fnostart > 0) println(s"Skipped to ${ap.fnostart}")

  // cut remaing pixels
  val height = ap.height - (ap.height % ap.nrowbundles)
  val width  = ap.width  - (ap.width  % ap.ncolshifts)
  val nyblocks = height / ap.nrowbundles
  val nxblocks = width / ap.ncolshifts


  def compressFrameWithBundleBitshuffle() : Array[Array[Float]] = {
    val pixelsinblock =  ap.nrowbundles * ap.ncolshifts
    val uncompressedbits = pixelsinblock * ap.bitspx

    val cr = Array.ofDim[Float](nyblocks, nxblocks) // Array is mutable
    for (x <- 0 until width by ap.ncolshifts) {
      for (y <- 0 until height by ap.nrowbundles) {
        // get a block that contains (ap.ncolshifts * ap.nrowbundles) pixels
        val blockLL = List.tabulate(ap.nrowbundles) {
          rowidx => rawimg.getpxs(x, y + rowidx, ap.ncolshifts) toList}
        val block = blockLL.flatten map (_.toLong)

        val bs : List[Long] = bitShuffleLong(block, ap.bitspx)
        val nonzero = bs.filter(x => x > 0)
        cr(y/ap.nrowbundles)(x/ap.ncolshifts) = uncompressedbits.toFloat / (nonzero.length*pixelsinblock + ap.bitspx)
      }
    }
    cr
  }

  for (fno <- ap.fnostart to ap.fnostop) {
    if (ap.psize == 4) rawimg.readImageInt(in)
    else if (ap.psize == 1) rawimg.readImageByte(in)

    val cr_bbs = compressFrameWithBundleBitshuffle()

    printStats("BundleBitShuffle", cr_bbs.flatten.toList)

  }
  }
}
