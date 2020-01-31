HACOGen - hardware compressor generator


Hardware compressor generator (HACOGen), written in Chisel, generates
hardware compressor Verilog codes that compresses input data in a
streaming manner (without stall).  Just starting up a project. It only
generates a simple zero-skimming compressor at this point. The codes
are not well-organized now. I will add more details here and sort out
and improve the codes one by one.


Getting Started
---------------

     $ git clone https://github.com/kazutomo/hacogen.git
     $ cd hacogen
     $ make t


How to use HACOGen
--------------

     $ make test       # run Scala test
     $ make simulate   # invoke Verilator
     $ make verilog    # only generate Verilog codes

Shorter target names for convinice

     $ make t
     $ make s
     $ make v

To test invididual module

     $ make test T=selector

To list available target

     $ make list
     Available targets: header, selector, squeeze, stbuf


Developed by Kazutomo Yoshii <kazutomo.yoshii@gmail.com>
