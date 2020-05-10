package refcomp

import scala.collection.mutable.ListBuffer

// software reference implementations of encoders, compression ratio estimator
object RefComp {


  def shuffleVerticalLineSample(data: List[Int], stride: Int) : List[Int] = {
    val a = data.toArray
    List.tabulate(data.length) {i =>  a( ((i%stride)*stride) + (i/stride) )}
  }


  // simple runlength encoding
  def runlengthEncoding(data: List[Int]) : List[Int] = {
    var curval = 0
    var cnt = 0
    var res = ListBuffer[Int]()

    for (p <- data) {
      if (curval == p) cnt += 1
      else {
        res += curval
        res += cnt
        curval = p
        cnt = 0
      }
    }
    res += curval
    res += cnt

    res.toList
  }

  // zero-suppression
  def zsEncoding(data: List[Int], npxblock: Int, nheaders: Int) : List[Int] = {
    var res = ListBuffer[Int]()

    for(i <- 0 until data.length by npxblock) {
      for (j <- 0 until nheaders)  res += 0xffff // dummy header
      data.slice(i, i+npxblock).filter(_ > 0).foreach {v => res += v}
    }
    res.toList
  }

  def clipSample(data: List[Int], bitspx: Int) : List[Int] = {
    val maxval = (1<<bitspx) - 1 // move out
    data.map(v => if(v < maxval) v else maxval)
  }


    // Assume that pxs.length is npxblock and each px is bitspx bits
  // The length of a returned list is bitspx and each px is npxblock bits
  // pin_i_j => pout_j_i, where i is i-the element in list and j is j-th bit of the element. it resembles a matrix transpose operation.
  // also note that this is a reversible operation.
  def bitshuffleBlock(pxs: List[Int], bitspx: Int) : List[Int] = {
    val npxblock = pxs.length
    val inp = pxs.toArray
    var res = new Array[Int](bitspx)

    for (bpos <- 0 until bitspx) {
      res(bpos) =
        List.tabulate(npxblock) {i => if((inp(i)&(1<<bpos)) > 0) 1<<i else 0} reduce (_|_)
    }
    res.toList
  }

  // Assume that data.length is a multiple of npxblock
  def bitshuffleVerticalLineSample(data: List[Int], npxblock: Int, bitspx: Int) : List[Int] = {
    var res = List[Int]()

    for (block <- data.sliding(npxblock, npxblock)) {
      res = res ::: bitshuffleBlock(block, bitspx)
/*
      val a = bitshuffleBlock(block, bitspx)
      val b = bitshuffleBlock(a, npxblock)
      for ((p,q) <- block zip b) {
        if (p != q) {
          println(s"ERROR: $p $q")
        }
      }
 */
    }
    res
  }
}
