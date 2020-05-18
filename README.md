HACOGen - hardware compressor generator

Hardware compressor generator (HACOGen), written in Chisel3, is a
generator framework for hardware stream compressors.  The current
version generates a simple zero-skimming compressor logic (Figure
below). The compressor receives data as a vector of elements every
single cycle, compress the data and stack it into an internal
buffer. When the buffer gets full, the flush signal is raised; its
content is ready to be read. This reposiroty also includes a
Scala-based estimation tool to evaluate different compresison schemes
and operations such as runlength, zero skimming and a bit shuffle
operation.

<img src="https://raw.githubusercontent.com/kazutomo/hacogen/master/figs/streamcomp.png"  width="512" />



Getting Started
---------------

     $ git clone https://github.com/kazutomo/hacogen.git
     $ cd hacogen
     $ make test

NOTE: tested on Fedora 28 with sbt 1.3.7

How to use HACOGen
--------------

     $ make test       # runs Scala test.
     $ make simulate   # invokes Verilator and generates vcd.
     $ make verilog    # generates Verilog codes.

Shorter target names for convinice

     $ make t
     $ make s
     $ make v

To test invididual module:

     $ make test T=selector

To list available target:

     $ make list
     Available targets: header, selector, squeeze, stbuf


Design RTL view
---------------

<img src="https://raw.githubusercontent.com/kazutomo/hacogen/master/figs/comp-rtl-view.png"  width="512" />


Simulation results
------------------

<img src="https://raw.githubusercontent.com/kazutomo/hacogen/master/figs/haco-wave.png"  width="512" />


<a href="https://raw.githubusercontent.com/kazutomo/hacogen/master/results/comp-output.txt" >simulation result in text file</a>


Component RTL view
------------------

<img src="https://raw.githubusercontent.com/kazutomo/hacogen/master/figs/rtl-view-squeeze-shiftup.png"  width="512" />

<img src="https://raw.githubusercontent.com/kazutomo/hacogen/master/figs/rtl-view-stbuf.png"  width="512" />



----
Developed by Kazutomo Yoshii <kazutomo.yoshii@gmail.com>
