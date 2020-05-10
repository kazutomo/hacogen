package refcomp

import scala.collection.mutable.ListBuffer

// software reference implementations of encoders, compression ratio estimator
object RefComp {
  def clipSample(data: List[Int], bitspx: Int) : List[Int] = {
    val maxval = (1<<bitspx) - 1 // move out
    data.map(v => if(v < maxval) v else maxval)
  }

  // a simple runlength encoding
  def rlEncoding(data: List[Int]) : List[Int] = {
    var curval = -1
    var cnt = 0
    var res = ListBuffer[Int]()

    for (p <- data) {
      if (curval < 0 ) { // the first val
        curval = p
        cnt = 1
      } else if (curval == p) cnt += 1
      else {
        res += cnt
        res += curval
        curval = p
        cnt = 1
      }
    }
    res += cnt
    res += curval

    res.toList
  }

  // zero-suppression encoding
  def zsEncoding(px: List[Int], bitspx: Int) : List[Int] = {
    val headerlist = List.tabulate(px.length)(i => if (px(i) == 0) 0 else 1<<i)
    val ninpxs = px.length
    val nheaders = math.ceil(ninpxs.toDouble/bitspx.toDouble).toInt
    val header = headerlist.reduce(_ + _) // | (1 << (c.elemsize-1))
    val headers = List.fill(nheaders)(header) // faking, but ok, only count the number of encoded data
    val nonzero = px.filter(x => x > 0)
    return headers ::: nonzero
  }

  // Assume that pxs.length is npxblock and each px is bitspx bits
  // The length of a returned list is bitspx and each px is npxblock bits
  // pin_i_j => pout_j_i, where i is i-the element in list and j is j-th bit of the element. it resembles a matrix transpose operation.
  // also note that this is a reversible operation.
  def bitShuffle(pxs: List[Int], bitspx: Int) : List[Int] = {
    val npxblock = pxs.length
    val inp = pxs.toArray
    var res = new Array[Int](bitspx)

    for (bpos <- 0 until bitspx) {
      res(bpos) =
        List.tabulate(npxblock) {i => if((inp(i)&(1<<bpos)) > 0) 1<<i else 0} reduce (_|_)
    }
    res.toList
  }

  // shuffled zs
  def shzsEncoding(px: List[Int], bitspx: Int) : List[Int] = {
    val bs = bitShuffle(px, bitspx)
    zsEncoding(bs, bitspx)
  }

  // unused
  def shuffleVerticalLineSample(data: List[Int], stride: Int) : List[Int] = {
    val a = data.toArray
    List.tabulate(data.length) {i =>  a( ((i%stride)*stride) + (i/stride) )}
  }

  // unused for now
  // Assume that data.length is a multiple of npxblock
  def bitshuffleVerticalLineSample(data: List[Int], npxblock: Int, bitspx: Int) : List[Int] = {
    var res = List[Int]()

    for (block <- data.sliding(npxblock, npxblock)) {
      res = res ::: bitShuffle(block, bitspx)
    }
    res
  }
  // enclens : a list of encoded length
  // nbufpxs : the number of pixels of the output buffer
  // return the number of buffers used
  def fillBuffer(enclens: List[Int], nbufpxs: Int):
      Int = {
    var bufcnt = 0
    var nbufpxs_used = 0 // the number of buffer pixels used

    for (l <- enclens) {
      if (nbufpxs_used + l < nbufpxs) {
        // add to a buffer if there is a room
        nbufpxs_used += l
      } else {
        bufcnt += 1

        // if not enough space, fill the next buffer
        if (nbufpxs_used + l > nbufpxs)  nbufpxs_used = l
      }
    }
    // count a partially-filled buffer as one buffer
    if (nbufpxs_used>0) bufcnt += 1

    bufcnt
  }

  // class CompRatioT(val mean: Float=0.0, val std: Float, val min: Float, val max: Float)
}
