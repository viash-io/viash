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
  for (testName <- List("testbash", "testpython", "testr")) {
    for (platName <- List("docker", "native")) {
      val funcFile = getClass.getResource(s"/$testName/functionality.yaml").getPath
      val platFile = getClass.getResource(s"/$testName/platform_$platName.yaml").getPath

      val temporaryFolder = Files.createTempDirectory(Paths.get("/tmp"), "viash_tester").toFile()

      val tempFolStr = temporaryFolder.toString()

      // parse functionality from file
      val functionality = Functionality.parse(new File(funcFile))
      val platform = Target.parse(new File(platFile))

      // run tests
      val results = ViashTester.testFunctionality(functionality, platform)

      for ((file, outcode, output) <- results) {
        test(s"Testing $testName platform $platName with test $file") {
          assert(outcode == 0, output)
        }
      }
    }
  }
}