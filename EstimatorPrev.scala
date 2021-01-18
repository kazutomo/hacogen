package refcomp

import Array._
import java.io._
import java.nio.ByteBuffer
import scala.collection.mutable.ListBuffer
import javax.imageio.ImageIO // png
import java.awt.image.BufferedImage // png

// local claases
import refcomp.Util._
import refcomp.RefComp._

//import rawimagetool._
//import localutil.Util._
//import refcomp.RefComp._
//import estimator._ // AppParams

object EstimatorPrev  {

  // NOTE: indent inside run() later
  def run(args: Array[String]) {

  var ap = EstimatorAppParams

  ap.getopts(args)
  ap.printinfo()

  // open a dataset
  val in = new FileInputStream(ap.filename)
  val rawimg = new RawImageTool(ap.width, ap.height)

  val nshifts = (ap.height/ap.ninpxs) * ap.width

  val st = System.nanoTime()

  // need to skip frames

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
      rawimg.writePng(pngfn, 1)
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

  val hyd = ap.height - (ap.height % ap.ninpxs) // to simplify, ignore the remaining
  val total_inpxs = ap.width * hyd
  val total_shuffled_inpxs = ap.width * (hyd/ap.ninpxs*ap.bitspx)


  def analyzeframe() : Map[String, Float] = {
    // per-frame stats
    // enclens is created for each encode scheme
    // rl   : N-input run-length
    // zs   : N-input zero suppression
    // shzs : shuffled N-input zero suppression
    //var enclens_rl   = new ListBuffer[Int]()
    //var enclens_shrl = new ListBuffer[Int]()
    var enclens_zs   = new ListBuffer[Int]()
    var enclens_shzs = new ListBuffer[Int]()

    var watermark = 0

    // chunk up to rows whose height is ap.ninpxs
    for (yoff <- 0 until hyd by ap.ninpxs) {
      // each column shift (every cycle in HW)
      watermark = 0
      for (xoff <- 0 until ap.width) {
        // create a column chunk, which is an input to the compressor
        val indata = List.tabulate(ap.ninpxs)(
          rno =>
          rawimg.getpx(xoff, rno + yoff))

        // only check the number of pixel output
        //enclens_rl   += rlEncode(indata).length
        enclens_zs   += zsEncode(indata, ap.bitspx).length
        if (ap.ninpxs > ap.bitspx) {

          if (false) {
            val tmpa = shzsEncode(indata, ap.bitspx)
            //val tmpb = shrlEncode(indata, ap.bitspx)

            print("shzs: ")
            for (e <- tmpa) print(e + " ")
            println()
            //print("shrl: ")
            //for (e <- tmpb) print(e + " ")
            //println()
          }

          val shzslen = shzsEncode(indata, ap.bitspx).length
          enclens_shzs += shzslen
          //enclens_shrl += shrlEncode(indata, ap.bitspx).length

          watermark += shzslen - 4
          if (watermark < 0) watermark = 0
          //print(watermark + " ")
        }
      }
      // HZM quick hack
      //enclens_zs +=1
      //enclens_shzs += 1

      //println()
    }



    //val nrl = enclens_rl reduce(_+_)
    val nzs = enclens_zs reduce(_+_)

    val nshzs =
      if (ap.ninpxs > ap.bitspx) enclens_shzs reduce(_+_)
      else 1

    //val nshrl =
    //if (ap.ninpxs > ap.bitspx) enclens_shrl reduce(_+_)
      //else 1

    val ti = total_inpxs.toFloat
    val tsi = total_shuffled_inpxs.toFloat

    if (false) {
      //printStats("RL", enclens_rl.toList.map(_.toFloat) )
      printStats("ZS", enclens_zs.toList.map(_.toFloat) )
      if (ap.ninpxs > ap.bitspx)
        printStats("SHZS", enclens_shzs.toList.map(_.toFloat) )
      //println(f"RL  : ${total_inpxs}/${nrl} => ${ti/nrl}")
      println(f"ZS  : ${total_inpxs}/${nzs} => ${ti/nzs}")
      if (ap.ninpxs > ap.bitspx)
        println(f"SHZS: ${total_shuffled_inpxs}/${nshzs} => ${tsi/nshzs}")
    }

    // fix this hard-coded values later
    /*
    val b256nzs = calcNBufferedPixels(enclens_zs.toList, 28)
    val b256nshzs =
      if (ap.ninpxs > ap.bitspx)  calcNBufferedPixels(enclens_shzs.toList, 16)
      else 1
    val b512nzs = calcNBufferedPixels(enclens_zs.toList, 56)
    val b512nshzs =
      if (ap.ninpxs > ap.bitspx) calcNBufferedPixels(enclens_shzs.toList, 32)
      else 1

    if (false) {
      //println(f"B256RL  : ${total_inpxs}/${b256nrl} => ${ti/b256nrl}")
      println(f"B256ZS  : ${total_inpxs}/${b256nzs} => ${ti/b256nzs}")
      if (ap.ninpxs > ap.bitspx)
        println(f"B256SHZS: ${total_shuffled_inpxs}/${b256nshzs} => ${tsi/b256nshzs}")
      //println(f"B512RL  : ${total_inpxs}/${b512nrl} => ${ti/b512nrl}")
      println(f"B512ZS  : ${total_inpxs}/${b512nzs} => ${ti/b512nzs}")
      if (ap.ninpxs > ap.bitspx)
        println(f"B512SHZS: ${total_shuffled_inpxs}/${b512nshzs} => ${tsi/b512nshzs}")
    }
     */

    val ret = Map(
      "ZS" -> ti/nzs
    )
      //"RL" -> ti/nrl,
    // "ZS256" -> ti/b256nzs,  "ZS512" -> ti/b512nzs

    if (ap.ninpxs > ap.bitspx)
      ret ++ Map(
        "SHZS" -> tsi/nshzs
      )
        //"SHRL" -> tsi/nshrl
    //"SHZS256" -> tsi/b256nshzs,
    //"SHZS512" -> tsi/b512nshzs
    else
      ret
  }


  // stats for entire dataset
  var allzeroratios = new ListBuffer[Float]()
  //var allRLs = new ListBuffer[Float]()
  var allZSs = new ListBuffer[Float]()
  var allSHZSs = new ListBuffer[Float]()
  //var allSHRLs = new ListBuffer[Float]()
  /*
  var allZS256s = new ListBuffer[Float]()
  var allSHZS256s = new ListBuffer[Float]()
  var allZS512s = new ListBuffer[Float]()
  var allSHZS512s = new ListBuffer[Float]()
   */

  // per frame compression ratio
  var cr_rl = new ListBuffer[Int]()
  var maxzeroratio = 0.0.toFloat
  var minzeroratio = 1.0.toFloat
  var fno_maxzeroratio = 0 // skip 1.0 zeroratio frame
  var fno_minzeroratio = 0


  // iterate frames
  for (fno <- ap.fnostart to ap.fnostop) {
    if (ap.psize == 4) rawimg.readImageInt(in)
    else if (ap.psize == 1) rawimg.readImageByte(in)

    optionalprocessing(fno)

    val zeroratio = rawimg.zerocnt.toFloat / (ap.width*ap.height).toFloat
    // only calculate the ratio when the current frame has at least one non-zero pixel; skipping the completely blank frame
    if (zeroratio < 1.0.toFloat) {
      allzeroratios += zeroratio

      if (zeroratio > maxzeroratio) {
        fno_maxzeroratio = fno
        maxzeroratio = zeroratio
      }
      if (zeroratio < minzeroratio) {
        fno_minzeroratio = fno
        minzeroratio = zeroratio
      }
      val maxval = rawimg.maxval
      println(f"$fno%04d: zeroratio=$zeroratio%.3f maxval=$maxval")
    }

    if (zeroratio < 1.0.toFloat) {
      val crmap = analyzeframe()

      //allRLs += crmap("RL")
      allZSs += crmap("ZS")
      if (ap.ninpxs > ap.bitspx) {
        allSHZSs += crmap("SHZS")
        //allSHRLs += crmap("SHRL")
      }
      /*
      //allRL256s += crmap("RL256")
      allZS256s += crmap("ZS256")
      if (ap.ninpxs > ap.bitspx)
        allSHZS256s += crmap("SHZS256")
      //allRL512s += crmap("RL512")
      allZS512s += crmap("ZS512")
      if (ap.ninpxs > ap.bitspx)
        allSHZS512s += crmap("SHZS512")
       */
    } else
      println(f"skip fno${fno} because all pixels are zero")
  }
  val et = System.nanoTime()
  val psec = (et-st)*1e-9
  val nframes = ap.fnostop - ap.fnostart + 1
  println(f"Processing Time[Sec] = $psec%.3f total / ${psec/nframes.toFloat}%.3f per frame")

  println()
  printStats("zeroratio", allzeroratios.toList)
  println(f"maxzeroratio=$maxzeroratio@$fno_maxzeroratio")
  println(f"minzeroratio=$minzeroratio@$fno_minzeroratio")
  println()

  val npxs = ap.ninpxs
  //printStats(f"RL$npxs",   allRLs.toList)
  printStats(f"ZS$npxs",   allZSs.toList)
  if (ap.ninpxs > ap.bitspx)
    printStats(f"SHZS$npxs", allSHZSs.toList)
  //if (ap.ninpxs > ap.bitspx)
    //printStats(f"SHRL$npxs", allSHRLs.toList)
/*
  println()
  //printStats(f"RL$npxs-256b",   allRL256s.toList)
  printStats(f"ZS$npxs-256b",   allZS256s.toList)
  if (ap.ninpxs > ap.bitspx)
    printStats(f"SHZS$npxs-256b", allSHZS256s.toList)
  println()
  //printStats(f"RL$npxs-512b",   allRL512s.toList)
  printStats(f"ZS$npxs-512b",   allZS512s.toList)
  if (ap.ninpxs > ap.bitspx)
    printStats(f"SHZS$npxs-512b", allSHZS512s.toList)
 */
  println()
    println()
  }
}
