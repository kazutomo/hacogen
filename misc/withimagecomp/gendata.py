#!/usr/bin/env python3

import sys, math, os
import numpy as np
import matplotlib.pyplot as plt
from scipy.signal import convolve2d
import struct
import skimage.measure

fn = '/home/kazutomo/xraydata/25.npy'

if len(sys.argv) > 1:
    fn = sys.argv[1]

nframes=1  # -1 means all
firstframe=0
if len(sys.argv) > 2:
    firstframe=int(sys.argv[2])
if len(sys.argv) > 3:
    nframes=int(sys.argv[3])
    
data = np.load(fn)
print('data:', fn, data.shape)

if nframes < 0:
    nframes=data.shape[0]

print('firstframe:', firstframe)
print('nframes:', nframes)

origsize = data.shape[1] * data.shape[2] *  2  # size in byte. assume each element is 2 bytes.

print('origsize:', origsize)

def calcentropy(npdata, maxbit = 16):
    data = npdata.flatten().astype('uint16')
    hmap = {}
    for v in data:
        if v in hmap.keys():
            hmap[v] += 1
        else:
            hmap[v] = 1

    entropy = 0.0
    
    ndata = float(len(data))
    nvals = 1<<maxbit
    for k in hmap.keys():
        p = float(hmap[k]) / ndata
        #entropy -= p * math.log(p, nvals)
        entropy -= p * math.log(p, 2)
    
    return entropy

# fakedata set. 32*32 becasue of 10 bits
if False:
    print('No room for compression')
    ret = calcentropy(np.arange(0, 32*32).reshape(32,32), maxbit=10)
    if ret != 1.0:
        print('Failed to test ret=',ret)
    print(ret)
    print('Full room for compression')
    ret = calcentropy(np.full(32*32, 2).reshape(32,32), maxbit=10)
    if ret != 0.0:
        print('Failed to test ret=',ret)
    print(ret)

dims = data.shape

# generate a raw grayscale file
def tofile(bn, npdata):
    d = npdata.flatten()
    n = len(d)
    duint64 = d.astype('uint16')
    bd = struct.pack('=%dH' % n, *duint64)
    f = open(bn+'.gray', mode='wb')
    f.write(bd)
    f.close()

def downsample_average(nds, npdata):
    return skimage.measure.block_reduce(npdata, (nds,nds), np.mean)

def downsample_max(nds, npdata):
    return skimage.measure.block_reduce(npdata, (nds,nds), np.max)

def framestat(label, npdata, nbits):
    ent = calcentropy(npdata, maxbit = nbits)
    print(label)
    maxv = npdata.max()
    nbits = math.ceil(math.log(npdata.max(),2))
    print('  max:', maxv)
    print('  nbits:', nbits)
    print('  entropy:', ent)
    return maxv, nbits, ent

enable_dsav=False
enable_dsmax=False

results={}
results['maxv'] = []
results['nbits'] = []
results['ent'] = []
results['pngsize'] = []
results['jp2size'] = []

outputdir='tmp'

if not os.path.isdir(outputdir):
    os.mkdir(outputdir)

for n in range(firstframe,firstframe+nframes):
    print("== Frame%d ==" % n)

    orig = data[n]
    frombits = 16
    (maxv, nbits, ent) = framestat('[orig]', orig, frombits)
    tofile('%s/orig%03d'%(outputdir,n), orig)
    results['maxv'].append(maxv)
    results['nbits'].append(nbits)
    results['ent'].append(ent)

    # convert -size 512x512 -depth ${bits} orig000.gray orig000.png
    # convert ${fn}.png ${fn}.jpg
    # convert ${fn}.png ${fn}.jp2
    # pngsize=`stat -c %s ${fn}.png`

    
    if enable_dsav:
        dsav = downsample_average(4, orig)
        framestat('[downsampling - average]', dsav, frombits)
        tofile('%s/dsav%03d'%(outputdir,n), dsav)

    if enable_dsmax:
        dsmax = downsample_max(4, orig)
        framestat('[downsampling - max]', dsmax, frombits)
        tofile('%s/dsmax%03d'%(outputdir,n), dsmax)
    
print('')
print('Note: entropy [0, 1]  smaller more room')
