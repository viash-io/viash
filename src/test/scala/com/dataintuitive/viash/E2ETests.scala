package com.dataintuitive.viash

import org.scalatest.{FunSuite, Tag}
import com.dataintuitive.viash.config.Config
import com.dataintuitive.viash.helpers._

class E2ETests extends FunSuite {
  for (
    (testName, scriptName) <- List(
      ("testbash", None),
      ("testpython", Some("config.vsh.yaml")),
      ("testr", Some("code.vsh.R")),
      ("testexecutable", None)
    );
    platName <- List("docker", "native")
  ) {

    val config =
      if (scriptName.isDefined) {
        val compRes = getClass.getResource(s"/$testName/${scriptName.get}")
        Some(Config.readSplitOrJoined(
          joined = Some(compRes.toString),
          platformID = Some(platName),
          modifyFun = false
        ))
      } else {
        val funcRes = getClass.getResource(s"/$testName/functionality.yaml")
        val platRes = getClass.getResource(s"/$testName/platform_$platName.yaml")

        if (platRes != null) {
          Some(Config.readSplitOrJoined(
            functionality = Some(funcRes.toString),
            platform = Some(platRes.toString),
            modifyFun = false
          ))
        } else {
          None
        }
      }

    if (config.isDefined) {
      // run tests
      val dir = IOHelper.makeTemp("viash_test_" + config.get.functionality.name)

      val tags: List[Tag] = platName match {
        case "docker" => List(DockerTest)
        case "native" => List(NativeTest)
        case _ => Nil
      }

      test(s"Testing $testName platform $platName", tags: _*) {
        val results = try {
          ViashTester.runTests(config.get.functionality, config.get.platform.get, dir)
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