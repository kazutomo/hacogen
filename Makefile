# HACOGen makefile
# written by Kazutomo Yoshii <kazutomo.yoshii@gmail.com>

T=shcomp

all:
	@echo "To run HACOGen:"
	@echo "$ make TARGET"
	@echo ""
	@echo "To generate Verilog file"
	@echo "$ make TARGET.v	"
	@echo ""
	@echo "To simulate using Verilator"
	@echo "$ make TARGET.vcd"
	@echo ""
	@echo "To list target available modules:"
	@echo "$ make list"
	@echo ""

% : %.scala
	@./run.sh test $@

%.v : %.scala
	@./run.sh verilog $(@:.v=)

%.vcd : %.scala
	@./run.sh simulate $(@:.vcd=)
	@find -type f -name $@

l list:
	@./run.sh list

#
# utility
#

Estimator: Estimator.scala EstimatorAppParams.scala EstimatorPrev.scala RawImageTool.scala Localutils.scala RefComp.scala
	@mkdir -p classes
	fsc -d classes $^

clean:
	rm -rf project target test_run_dir generated *.class
	rm -rf *.fir *.anno.json
	rm -rf classes
