# HACOGen makefile
# written by Kazutomo Yoshii <kazutomo.yoshii@gmail.com>

T=comp

all:
	@echo "To run HACOGen:"
	@echo ""
	@echo "$ make test       # run Scala test"
	@echo "$ make simulate   # invoke Verilator"
	@echo "$ make verilog    # only generate Verilog codes"
	@echo ""
	@echo "Shorter form for convinice"
	@echo "$ make t"
	@echo "$ make s"
	@echo "$ make v"
	@echo ""
	@echo "To test invididual module"
	@echo "$ make test T=$target"
	@echo ""
	@echo "target list: header, selector, squeeze, stbuf, shuffle"
	@echo ""


v verilog:
	sbt "test:runMain hacogen.HacoGen $T:verilog"

t test:
	sbt "test:runMain hacogen.HacoGen $T"

s simulate:
	sbt "test:runMain hacogen.HacoGen $T --backend-name verilator"

l list:
	@echo "Available targets: header, selector, squeeze, stbuf"
#	sbt "test:runMain hacogen.HacoGen list"

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
