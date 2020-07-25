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

classes/EstimatorMain.class: Estimator.scala
	@mkdir -p classes
	fsc -d classes $<

classes/rawimagetool/RawImageTool.class: RawImageTool.scala
	@mkdir -p classes
	fsc -d classes $<

classes/localutil/Localutils.class: Localutils.scala
	@mkdir -p classes
	fsc -d classes $<

classes/refcomp/RefComp.class: RefComp.scala
	@mkdir -p classes
	fsc -d classes $<

EstimatorClasses=\
classes/rawimagetool/RawImageTool.class \
classes/localutil/Localutils.class \
classes/refcomp/RefComp.class \
classes/EstimatorMain.class

Estimator: $(EstimatorClasses)


clean:
	rm -rf project target test_run_dir generated *.class
	rm -rf *.fir *.anno.json
	rm -rf classes
