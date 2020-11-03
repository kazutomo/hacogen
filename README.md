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

<p align="center">
<img src="https://raw.githubusercontent.com/kazutomo/hacogen/master/figs/streamcomp.png"  width="512" />
</p>


Getting Started
---------------

     $ git clone https://github.com/kazutomo/hacogen.git
     $ cd hacogen
     $ make test

NOTE: I personally tested hacogen on Fedora28 and Fedora32.
Additional packages you need to install are sbt and verilator.  I hope
this should work on other Linux distros as well.

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

<p align="center">
<img src="https://raw.githubusercontent.com/kazutomo/hacogen/master/figs/comp-rtl-view.png"  width="512" />
</p>

Simulation results
------------------

<p align="center">
<img src="https://raw.githubusercontent.com/kazutomo/hacogen/master/figs/haco-wave.png"  width="512" />
</p>

<p align="center">
<a href="https://raw.githubusercontent.com/kazutomo/hacogen/master/results/comp-output.txt" >simulation result in text file</a>
</p>


Component RTL view
------------------

<p align="center">
<img src="https://raw.githubusercontent.com/kazutomo/hacogen/master/figs/rtl-view-squeeze-shiftup.png"  width="512" />
</p>

<p align="center">
<img src="https://raw.githubusercontent.com/kazutomo/hacogen/master/figs/rtl-view-stbuf.png"  width="512" />
</p>



----
Developed by Kazutomo Yoshii <kazutomo.yoshii@gmail.com>
