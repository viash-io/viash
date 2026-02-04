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

  test("ViashParseArgumentValue") {
    val testScript = bashUtilsTestDir.resolve("ViashParseArgumentValue.test.sh")
    
    val result = Exec.runCatchPath(
      List("bash", testScript.toString),
      cwd = Some(projectRoot)
    )
    
    assert(result.exitValue == 0, s"ViashParseArgumentValue test failed:\n${result.output}")
  }

  test("ViashCleanupRegistry") {
    val testScript = bashUtilsTestDir.resolve("ViashCleanupRegistry.test.sh")
    
    val result = Exec.runCatchPath(
      List("bash", testScript.toString),
      cwd = Some(projectRoot)
    )
    
    assert(result.exitValue == 0, s"ViashCleanupRegistry test failed:\n${result.output}")
  }
}
