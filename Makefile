# HACOGen makefile
# written by Kazutomo Yoshii <kazutomo.yoshii@gmail.com>

T=shcomp

all:
	@echo "To run HACOGen:"
	@echo ""
	@echo "$ make test       # run Scala test"
	@echo "$ make simulate   # invoke Verilator"
	@echo "$ make verilog    # generate Verilog codes"
	@echo ""
	@echo "Shortcut for convinice"
	@echo "$ make t   # for test"
	@echo "$ make s   # for simulate"
	@echo "$ make v   # for verilog"
	@echo ""
	@echo "To test invididual module"
	@echo "$ make test T=squeeze  #  test the squeeze module"
	@echo ""
	@echo "To list target available modules"
	@echo "$ make list"
	@echo ""


v verilog:
	sbt "test:runMain hacogen.HacoGen $T:verilog"

t test:
	sbt "test:runMain hacogen.HacoGen $T"

s simulate:
	sbt "test:runMain hacogen.HacoGen $T --backend-name verilator"

l list:
	sbt "test:runMain hacogen.HacoGen list"

h help:
	sbt "test:runMain hacogen.HacoGen --help"

#
# utility
#

Estimator: Estimator.scala RawImageTool.scala Localutils.scala RefComp.scala
	@mkdir -p classes
	fsc -d classes $^

clean:
	rm -rf project target test_run_dir generated *.class
	rm -rf *.fir *.anno.json
	rm -rf classes
