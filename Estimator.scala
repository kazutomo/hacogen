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

import Array._
import scala.collection.mutable.ListBuffer

// local claases
import rawimagetool._
import localutil.Util._
import refcomp.RefComp._
import estimator._ // AppParams

object EstimatorMain extends App {

  var ap = EstimatorAppParams
  ap.getopts(args)
  ap.printinfo()

}
