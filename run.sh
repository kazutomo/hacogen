#!/bin/bash

if [ -z "$1" ] ; then
    echo "Usage: $0 command [target]"
    echo ""
    echo "Available commands:"
    echo "test or t     : runs target testbench (Scala-based tester)"
    echo "simulate or s : similar to test, but runs Verilator. A VCD file will be generated."
    echo "verilog or v  : generates a Verilog code"
    echo "list or l     : lists registered modules"
    echo ""
    exit 0
fi

mode=$1
shift

target=Header   # default
if [ ! -z "$1" ] ; then
    target=$1
fi
shift
opts=$@

if [ ${mode:0:1} = 's' ] ; then
    opts="--backend-name verilator $opts"
fi

sbt "test:runMain testmain.Main $mode $target $opts"

exit 0
