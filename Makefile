

T=default

all:
	@echo [targets]
	@echo "t (test), v (verilog), h (help) or clean"
	@echo ""
	@echo "Option T=targetname"
	@echo "`make T=l` shows the list"

t test:
	sbt "test:runMain hwcomp.TestMain $T"

v verilog:
	sbt "test:runMain hwcomp.TestMain $T --backend-name verilator"

h help:
	sbt "test:runMain hwcomp.TestMain --help"

clean:
	rm -rf project target test_run_dir generated *.class
