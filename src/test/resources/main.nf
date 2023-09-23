class Config {
	int foo
	Config(int foo) {
		this.foo = foo
	}
}

def test(path) {
	import java.nio.file.Paths
	Paths.get(path).isAbsolute()
}

println("Makefile.in: ${test("Makefile.in")}")
println("/home/rcannood/workspace/viash-io/viash/Makefile.in: ${test("/home/rcannood/workspace/viash-io/viash/Makefile.in")}")
