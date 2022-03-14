#!/usr/bin/env bash

#
inputdata="$HOME/xraydata/25.npy"

bits=16
#targets="orig000 dsav000 dsmax000"
targets="orig000"

odir='tmp'
# prepare dataset
./gendata.py $inputdata 0 1
#convert -size 128x128 -depth ${bits} $odir/dsav000.gray $odir/dsav000.png
#convert -size 128x128 -depth ${bits} $odir/dsmax000.gray $odir/dsmax000.png
convert -size 512x512 -depth ${bits} $odir/orig000.gray $odir/orig000.png


#
#
#

for fn in $targets ; do
	echo "[$fn]"

	convert $odir/${fn}.png $odir/${fn}.jpg
	convert $odir/${fn}.png $odir/${fn}.jp2

	origsize=`stat -c %s $odir/${fn}.gray`
	echo "origsize=$origsize"
	pngsize=`stat -c %s $odir/${fn}.png`
	jpgsize=`stat -c %s $odir/${fn}.jpg`
	jp2size=`stat -c %s $odir/${fn}.jp2`
	echo pngsize=$pngsize cr=`python -c "print($origsize/$pngsize)"`
	echo jp2size=$jp2size cr=`python -c "print($origsize/$jp2size)"`


	for q in 90 80 70 60 50 ; do
		echo -n "q${q} psnr: "
		convert $odir/${fn}.png -quality ${q} $odir/${fn}-q${q}.jp2
		sz=`stat -c %s $odir/${fn}-q${q}.jp2`
		echo -n jp2q${q}size=$sz cr=`python -c "print($origsize/$sz)"` " "
		compare -metric PSNR $odir/${fn}.png $odir/${fn}-q${q}.jp2 $odir/diff-${fn}-q${q}.jp2
		echo ""
	done

	echo -n "jpeg psnr: "
	echo -n jpgsize=$jpgsize cr=`python -c "print($origsize/$jpgsize)"` " "
	compare -metric PSNR $odir/${fn}.png $odir/${fn}.jpg $odir/diff-${fn}.jpg
	echo ""
done
