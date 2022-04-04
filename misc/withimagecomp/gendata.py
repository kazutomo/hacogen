#!/usr/bin/env python3

import sys, math, os
import subprocess
import numpy as np
import matplotlib.pyplot as plt
from scipy.signal import convolve2d
import struct
import skimage.measure

fn = '/home/kazutomo/xraydata/25.npy'

if len(sys.argv) > 1:
    fn = sys.argv[1]

nframes=-1  # -1 means all
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

def framestat(npdata, nbits):
    ent = calcentropy(npdata, maxbit = nbits)
    maxv = npdata.max()
    nbits = math.ceil(math.log(npdata.max(),2))
    return maxv, nbits, ent

enable_dsav=False
enable_dsmax=False

results={}
results['maxv'] = []
results['nbits'] = []
results['ent'] = []
results['pngcr'] = []
results['jp2cr'] = []
origsize=0
outputdir='tmp'

if not os.path.isdir(outputdir):
    os.mkdir(outputdir)


def runprocess(cmdargs):
    p = subprocess.check_output(cmdargs, encoding='utf8')
    return p

for n in range(firstframe,firstframe+nframes):
    print("== Frame%d ==" % n)

    orig = data[n]
    frombits = 16
    (maxv, nbits, ent) = framestat(orig, frombits)
    #print('  max:', maxv)
    #print('  nbits:', nbits)
    #print('  entropy:', ent)
    bn = '%s/orig%03d'%(outputdir,n)
    tofile(bn, orig)
    results['maxv'].append(maxv)
    results['nbits'].append(nbits)
    results['ent'].append(ent)

    w  = orig.shape[0]
    h = orig.shape[1]
    origsize=w*h*2
    runprocess(['convert', '-size', '%dx%d'%(w,h), '-depth', '%d'%frombits, '%s.gray'%bn, '%s.png'%bn])
    runprocess(['convert', '%s.png'%bn, '%s.jp2'%bn])
    ret = runprocess(['stat', '-c', '%s', '%s.png'%bn])
    pngsize=int(ret)
    ret = runprocess(['stat', '-c', '%s', '%s.jp2'%bn])
    jp2size=int(ret)
    #print('png.cr=%.3f'%(1.0*origsize/pngsize))
    #print('jp2.cr=%.3f'%(1.0*origsize/jp2size))
    results['pngcr'].append(1.0*origsize/pngsize)
    results['jp2cr'].append(1.0*origsize/jp2size)

    if enable_dsav:
        dsav = downsample_average(4, orig)
        framestat('[downsampling - average]', dsav, frombits)
        tofile('%s/dsav%03d'%(outputdir,n), dsav)

    if enable_dsmax:
        dsmax = downsample_max(4, orig)
        framestat('[downsampling - max]', dsmax, frombits)
        tofile('%s/dsmax%03d'%(outputdir,n), dsmax)

pngcrmean = np.mean(results['pngcr'])
pngcrstd  = np.std(results['pngcr'])
pngcrmax  = np.max(results['pngcr'])
pngcrmin  = np.min(results['pngcr'])

jp2crmean = np.mean(results['jp2cr'])
jp2crstd  = np.std(results['jp2cr'])
jp2crmax  = np.max(results['jp2cr'])
jp2crmin  = np.min(results['jp2cr'])


nbitsmean = np.mean(results['nbits'])
nbitsstd  = np.std(results['nbits'])
nbitsmax  = np.max(results['nbits'])
nbitsmin  = np.min(results['nbits'])

entmean = np.mean(results['ent'])
entstd  = np.std(results['ent'])
entmax  = np.max(results['ent'])
entmin  = np.min(results['ent'])

print('nbits: %.2f %.2f %.2f %.2f' % (nbitsmean, nbitsstd, nbitsmax, nbitsmin))

print('ent:   %.2f %.2f %.2f %.2f' % (entmean, entstd, entmax, entmin))

print('pngcr: %.2f %.2f %.2f %.2f' % (pngcrmean, pngcrstd, pngcrmax, pngcrmin))
print('jp2cr: %.2f %.2f %.2f %.2f' % (jp2crmean, jp2crstd, jp2crmax, jp2crmin))



#print('')
#print('Note: entropy [0, 1]  smaller more room')
