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
import scala.collection.parallel.mutable.ParArray

import java.io._
import java.nio.file.Path
import java.nio.file.Paths

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

    def optionalprocessing(fno: Int) {
      // write back to file.
      // To display an image, display -size $Wx$H -depth 16  imagefile
      if(ap.dump_gray)  {
        val grayfn = f"fr${fno}.gray"
        println(s"Writing $grayfn")
        rawimg.writeImageShort(grayfn)
      }
      if(ap.dump_png) {
        val pngfn = f"fr${fno}.png"
        println(s"Writing $pngfn")
        //rawimg.writePng(pngfn, threshold = 10, logscale = true, step = 25)
        rawimg.writePng(pngfn, threshold = 255)
      }
      if(ap.dump_pngregion) {
        val pngfn = f"fr${fno}-${ap.window_width}x${ap.window_height}+${ap.xoff}+${ap.yoff}.png"
        println(s"Writing $pngfn")
        rawimg.writePngRegion(pngfn, 1, ap.xoff, ap.yoff,
          ap.window_width, ap.window_height);
      }

      if(ap.dump_vsample) {
        val fn = f"vsample${fno}-x${ap.vsamplexpos}.txt"
        val vs = rawimg.getVerticalLineSample(ap.vsamplexpos, 0, ap.height)
        writeList2File(fn, vs)
        sys.exit(0)
      }
    }

    // cut remaing pixels
    val height = ap.height - (ap.height % ap.nrowbundles)
    val width  = ap.width  - (ap.width  % ap.ncolshifts)
    val maxval = (1<<ap.bitspx)-1
    val windowsize = 128
    val xstart = 0
    val xend = width
    //val xstart = (ap.width/2) - windowsize/2
    //val xend = xstart + windowsize
    val ystart = (ap.height/2) - windowsize/2
    val yend = ystart + windowsize
    val nyblocks = windowsize / ap.nrowbundles
    val nxblocks = windowsize / ap.ncolshifts
    val pixelsinblock =  ap.nrowbundles * ap.ncolshifts
    val rawbits = windowsize * ap.ncolshifts * ap.bitspx

    def compressFrameWithBundleBitshuffle() : List[Int] = {

      val cr = new ListBuffer[Int]()
      val shcr = new ListBuffer[Int]()

      for (x <- xstart until xend by ap.ncolshifts) {
        var nzlen = 0
        for (y <- ystart until yend by ap.nrowbundles) {
          // get a block that contains (ap.ncolshifts * ap.nrowbundles) pixels
          val blockLL = List.tabulate(ap.nrowbundles) {
            rowidx => rawimg.getpxs(x, y + rowidx, ap.ncolshifts) toList}
          val block = blockLL.flatten map {v => if (v>maxval) BigInt(maxval) else BigInt(v)}

          val bs : List[BigInt] = bitShuffleBigInt(block, ap.bitspx)
          val nonzero = bs.filter {x => x > BigInt(0)}
          shcr += nonzero.length
          nzlen += nonzero.length

          if (true) { // dump shuffled output with input for longer cases
            if (nonzero.length>2) {
              val fn = "tmp-shuffled-in-out.txt" // ad-hoc
              try {
                val out = new FileOutputStream(fn, true)
                out.write((block.mkString(" ")+"\n").getBytes)
                out.write((bs.mkString(" ")+"\n\n").getBytes)
                out.close()
              } catch {
                case e: FileNotFoundException => println("Not found:" + fn)
                case e: IOException => println("Failed to write")
              }
            }
          }
          /* validation
          val decoded = bitShuffleBigInt(bs, pixelsinblock)

          (block zip decoded).zipWithIndex map {
            case ((a,b),idx) => {
              if ((a-b) != 0) {
                println(f"Error: mismatch at $idx x=$x y=$y ($a != $b)")
                println("i:" + block)
                println("o:" + decoded)
                System.exit(0)
              }
            }
          }
           */
        }

        cr += nzlen
      }

      //
      if (false) {
        val nzlengrps = shcr.toList.groupBy {x => x}
        val nzlengrpss = nzlengrps.toSeq.sortBy(_._1)
        print("* Histogram: ")
        nzlengrpss foreach { case (k,v) => print(f"${k}b=${v.length} ") }
        println("")
      }

      cr.toList
    }


    def analyzeShredCRs() {

      val stpa = Array.fill(ap.fnostop-ap.fnostart+1) (new StatT())
      val wmstpa = Array.fill(ap.fnostop-ap.fnostart+1) (new StatT())
      val unusedbitsstpa = Array.fill(ap.fnostop-ap.fnostart+1) (new StatT())
      val headersize = ap.bitspx*(yend-ystart)/ap.nrowbundles

      val st = System.nanoTime()

      for (fno <- (ap.fnostart to ap.fnostop) ) {
        if (ap.psize == 4) rawimg.readImageInt(in)
        else if (ap.psize == 1) rawimg.readImageByte(in)

        optionalprocessing(fno)

        val nzlenlist = compressFrameWithBundleBitshuffle()
        val compbitlenlist = nzlenlist map {v => v*pixelsinblock + headersize}
        val watermarklist = compbitlenlist map {v => math.ceil(v.toFloat/1024.0).toFloat}
        val unusedbitslist = compbitlenlist map {v => (v%1024).toFloat} // toFloat due to calcStats requires List[Float]
        val crlist = compbitlenlist map {v => rawbits.toFloat/v.toFloat}

        // ad-hoc water mark dump
        if (ap.dump_watermark) {
          val datanametmp = Paths.get(ap.filename).getFileName
          val dataname = datanametmp.toString.slice(0,4)
          val fn = s"watermark-$dataname-fno$fno.txt"
          try {
            val out = new FileOutputStream(fn)
            watermarklist foreach {v => out.write(s"$v\n".getBytes)}
            out.close()
          } catch {
            case e: FileNotFoundException => println("Unable to open " + fn)
            case e: IOException => println("Failed to write")
          }
        }

        val idx = fno-ap.fnostart
        val st = calcStats(crlist)
        //println(f"$fno ${st.mean}%.5f ${st.std}%.5f ${st.min}%.5f ${st.max}%.5f")
        stpa(idx) = st
        wmstpa(idx) = calcStats(watermarklist)
        unusedbitsstpa(idx) = calcStats(unusedbitslist)

        print(".") // what's is the best way to show the progress
      } // for fno

      val et = (System.nanoTime() - st)*1e-9
      println(f"\nElapsed Time[s]: $et")

      // summarize results
      val dummy = 0.toFloat
      val dummyst = new StatT(dummy, dummy, dummy, dummy)
      class WMFno(var wm: Int, var fno: Int, var st: StatT)
      val minwm = new WMFno(100, 0, dummyst)
      val maxwm = new WMFno(0  , 0, dummyst)

      // need to be serialized in this way
      // write reduction version later
      for (fno <- (ap.fnostart to ap.fnostop) ) {
        val idx = fno-ap.fnostart

        val unusedbitsst = unusedbitsstpa(idx)
        val st = stpa(idx)
        val wmst = wmstpa(idx)
        val wmark = wmst.max.toInt
        if (wmark > maxwm.wm)  {
          maxwm.wm = wmark
          maxwm.fno = fno
          maxwm.st = st
        }

        if (wmark < minwm.wm)  {
          if (st.mean < 64.0) {
            minwm.wm = wmark
            minwm.fno = fno
            minwm.st = st
          }
        }
        println(f"$fno ${st.mean}%.3f ${st.std}%.3f ${st.min}%.3f ${st.max}%.5f   ${unusedbitsst.mean}%.2f ${unusedbitsst.std}%.2f ${unusedbitsst.min}%.2f ${unusedbitsst.max}%.2f ${wmst.mean}%.2f ${wmst.std}%.2f ${wmst.min}%.2f ${wmst.max}%.2f")

        //printStats("Shred WM: ", watermarklist)
        //println()
      }
      println(f"minwm=${minwm.wm}@${minwm.fno} stats=(${minwm.st.mean}%.5f,${minwm.st.std}%.5f,${minwm.st.min}%.5f,${minwm.st.max}%.5f)")
      println(f"maxwm=${maxwm.wm}@${maxwm.fno} stats=(${maxwm.st.mean}%.5f,${maxwm.st.std}%.5f,${maxwm.st.min}%.5f,${maxwm.st.max}%.5f)")
    }
    analyzeShredCRs()
  }
}
