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
	@echo "target list: header, selector, squeeze, stbuf"
	@echo ""


v verilog:
	sbt "test:runMain hwcomp.TestMain $T:verilog"

t test:
	sbt "test:runMain hwcomp.TestMain $T"

s simulate:
	sbt "test:runMain hwcomp.TestMain $T --backend-name verilator"

h help:
	sbt "test:runMain hwcomp.TestMain --help"

clean:
	rm -rf project target test_run_dir generated *.class
