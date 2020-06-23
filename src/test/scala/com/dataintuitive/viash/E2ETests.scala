package com.dataintuitive.viash

import org.scalatest.FunSuite
import java.nio.file.{Path, Paths, Files}
import java.io.File
import sys.process.Process
import com.dataintuitive.viash.functionality.Functionality
import com.dataintuitive.viash.targets.Target
import scala.io.Source
import scala.reflect.io.Directory
import com.dataintuitive.viash.helpers.Exec

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
      val functionality = Functionality.parse(new File(funcFile))
      val platform = Target.parse(new File(platFile))

      // run tests
      val dir = Exec.makeTemp("viash_test_" + functionality.name)

      val results = try {
        ViashTester.runTests(functionality, platform, dir, verbose = false)
      } finally {
        Exec.deleteRecursively(dir)
      }

      for (res <- results) {
        test(s"Testing $testName platform $platName with test ${res.name}") {
          if (res.exitValue != 0) {
            println(res.output)
          }
          assert(res.exitValue == 0, res.output)
        }
      }
    }
  }
}