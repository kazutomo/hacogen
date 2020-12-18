package refcomp

import scala.collection.mutable.ListBuffer


// software reference implementations of encoders, compression ratio estimator
object RefComp {
  def clipSample(data: List[Int], bitspx: Int) : List[Int] = {
    val maxval = (1<<bitspx) - 1 // move out
    data.map(v => if(v < maxval) v else maxval)
  }

  // a simple run-length encoding
  def rlEncode(data: List[Int]) : List[Int] = {
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

  def RLDecode(data: List[Int]) : List[Int] = {
    var res = ListBuffer[Int]()

    for(e <- data.sliding(2,2))
      for(t <- List.fill(e(0))(e(1))) res += t

    res.toList
  }

  // zero-suppression encode
  def zsEncode(px: List[Int], bitspx: Int) : List[Int] = {
    val ninpxs = px.length
    val nheaders = math.ceil(ninpxs.toDouble/bitspx.toDouble).toInt
    val metadatabits = List.tabulate(px.length)(i => if (px(i) == 0) 0 else 1<<(i%bitspx))
    val metadatapxs = metadatabits.sliding(bitspx, bitspx).toList
    val headers = metadatapxs.map(v => v.reduce(_|_))

    // headers foreach {v => println(v.toBinaryString)}

    val nonzero = px.filter(x => x > 0)
    // HZM quick hack
    //    if (nonzero.length < 2)
    //  return nonzero
//    else
      return headers ::: nonzero
  }

  def zsDecode(px: List[Int], bitspx: Int, ninpxs: Int) : List[Int] = {
    var res = ListBuffer[Int]()
    val nheaders = math.ceil(ninpxs.toDouble/bitspx.toDouble).toInt
    var dpos = nheaders
    for (i <- 0 until ninpxs) {
      val hpos = i/bitspx
      val mask = 1<<(i%bitspx)
      if( (px(hpos) & mask) > 0 ) {
        res += px(dpos)
        dpos += 1
      } else {
        res += 0
      }
    }
    res.toList
  }

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
  def shzsEncode(px: List[Int], bitspx: Int) : List[Int] = {
    val bs = bitShuffle(px, bitspx)
    zsEncode(bs, bitspx)
  }

  // shuffled rl
  def shrlEncode(px: List[Int], bitspx: Int) : List[Int] = {
    val bs = bitShuffle(px, bitspx)
    rlEncode(bs)
  }

  def compareTwoLists(a: List[Int], b: List[Int]) : Boolean = {
    if (a.length != b.length) return false

    for( (aa,bb) <- a zip b) {
      if (aa != bb) {
        return false
      }
    }
    true
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
  def calcNBufferedPixels(enclens: List[Int], nbufpxs: Int):
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
        else nbufpxs_used = 0
      }
    }
    // count a partially-filled buffer as one buffer
    if (nbufpxs_used>0) bufcnt += 1

    bufcnt * nbufpxs
  }
}
