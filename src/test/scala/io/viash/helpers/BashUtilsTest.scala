package io.viash.helpers

import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import java.nio.file.{Files, Paths, Path}

/**
 * Test suite for bash utility functions.
 * Runs all *.test.sh files in src/test/resources/io/viash/helpers/bashutils/
 */
class BashUtilsTest extends AnyFunSuite with BeforeAndAfterAll {
  Logger.UseColorOverride.value = Some(false)
  
  private val projectRoot = Paths.get(System.getProperty("user.dir"))
  private val bashUtilsTestDir = projectRoot.resolve("src/test/resources/io/viash/helpers/bashutils")

  /** Helper method to run a bash test script and assert it passes */
  private def runBashTest(scriptName: String, testName: String): Unit = {
    val testScript = bashUtilsTestDir.resolve(scriptName)
    
    val result = Exec.runCatchPath(
      List("bash", testScript.toString),
      cwd = Some(projectRoot)
    )
    
    assert(result.exitValue == 0, s"$testName test failed:\n${result.output}")
  }

  // Define test cases as (test name, script filename)
  private val testCases = Seq(
    ("ViashParseArgumentValue", "ViashParseArgumentValue.test.sh"),
    ("ViashCleanupRegistry", "ViashCleanupRegistry.test.sh"),
    ("ViashLogging", "ViashLogging.test.sh"),
    ("ViashQuote", "ViashQuote.test.sh"),
    ("ViashRenderJson", "ViashRenderJson.test.sh"),
    ("ViashAbsolutePath", "ViashAbsolutePath.test.sh"),
    ("ViashRemoveFlags", "ViashRemoveFlags.test.sh"),
    ("ViashSourceDir and ViashFindTargetDir", "ViashSourceDir.test.sh"),
    ("ViashDockerAutodetectMount", "ViashDockerAutodetectMount.test.sh")
  )

  // Register all tests
  testCases.foreach { case (testName, scriptName) =>
    test(testName) {
      runBashTest(scriptName, testName)
    }
  }
}
