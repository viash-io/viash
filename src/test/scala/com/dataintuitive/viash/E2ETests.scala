package com.dataintuitive.viash

import org.scalatest.{FunSuite, Tag}
import com.dataintuitive.viash.functionality.Functionality
import com.dataintuitive.viash.platforms.Platform
import com.dataintuitive.viash.helpers._

class E2ETests extends FunSuite {
  for (
    testName <- List("testbash", "testpython", "testr", "testexecutable");
    platName <- List("docker", "native")
  ) {
    val funcFile = getClass.getResource(s"/$testName/functionality.yaml").getPath()
    val platRes = getClass.getResource(s"/$testName/platform_$platName.yaml")

    if (platRes != null) {
      val platFile = platRes.getPath()
      // parse functionality from file
      val functionality = Functionality.parse(IOHelper.uri(funcFile))
      val platform = Platform.parse(IOHelper.uri(platFile))

      // run tests
      val dir = IOHelper.makeTemp("viash_test_" + functionality.name)

      val tags: List[Tag] = if (platName == "docker") List(DockerTest) else Nil

      test(s"Testing $testName platform $platName", tags: _*) {
        val results = try {
          ViashTester.runTests(functionality, platform, dir, verbose = false)
        } finally {
          IOHelper.deleteRecursively(dir)
        }

        for (res <- results) {
          val out = "!!!! TEST FAILED '" + res.name + "' !!!!\n" + res.output
          if (res.exitValue != 0) {
            println(out)
          }
          assert(res.exitValue == 0, out)
        }
      }
    }
  }
}