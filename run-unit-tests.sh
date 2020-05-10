
T="header selector squeeze stbuf shuffle"

LOGFN=tmp-output-unit-tests.txt
rm -f $LOGFN
for t in $T ; do
    make t T=$t | tee -a $LOGFN
done

echo ""
echo ""
echo "------------------------"
if grep FAIL $LOGFN ; then
    echo "FAILED!!!"
    echo "Check $LOGFN"
else
    echo "Done"
fi
