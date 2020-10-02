package com.dataintuitive.viash

import java.nio.file.Paths
import com.dataintuitive.viash.helpers.IO
import org.scalatest.{BeforeAndAfterAll, FunSuite}
import scala.reflect.io.Directory

class MainTestDockerTest extends FunSuite with BeforeAndAfterAll {
  // which platform to test
  private val funcFile = getClass.getResource("/testbash/functionality.yaml").getPath
  private val funcNoTestFile = getClass.getResource("/testbash/functionality_no_tests.yaml").getPath
  private val funcFailedTestFile = getClass.getResource("/testbash/functionality_failed_test.yaml").getPath
  private val platFile = getClass.getResource("/testbash/platform_docker.yaml").getPath
  private val platFailedBuildFile = getClass.getResource("/testbash/platform_docker_failed_build.yaml").getPath

  private val expectedTmpDirStr = s"${IO.tempDir}/viash_test_testbash"

  test("Check standard test output for typical outputs") {
    val testText = TestHelper.testMain(
      Array(
        "test",
        "-f", funcFile,
        "-p", platFile
      ))

    assert(testText.contains("Running tests in temporary directory: "))
    assert(testText.contains("SUCCESS! All 2 out of 2 test scripts succeeded!"))
    assert(testText.contains("Cleaning up temporary directory"))

    checkTempDirAndRemove(testText, expectedTmpDirStr, false)
  }

  test("Check output in case --keep is specified") {
    val testText = TestHelper.testMain(
      Array(
        "test",
        "-f", funcFile,
        "-p", platFile,
        "--keep"
      ))

    assert(testText.contains("Running tests in temporary directory: "))
    assert(testText.contains("SUCCESS! All 2 out of 2 test scripts succeeded!"))
    assert(!testText.contains("Cleaning up temporary directory"))

    checkTempDirAndRemove(testText, expectedTmpDirStr, true)
  }


  test("Check test output when no tests are specified in the functionality file") {
    val testText = TestHelper.testMain(
      Array(
        "test",
        "-f", funcNoTestFile,
        "-p", platFile
      ))

    assert(testText.contains("Running tests in temporary directory: "))
    assert(testText.contains("WARNING! No tests found!"))
    assert(testText.contains("Cleaning up temporary directory"))

    checkTempDirAndRemove(testText, expectedTmpDirStr, false)
  }

  test("Check test output when a test fails") {
    val testText = TestHelper.testMainException[RuntimeException](Array(
      "test",
      "-f", funcFailedTestFile,
      "-p", platFile
    ))

    assert(testText.contains("Running tests in temporary directory: "))
    assert(testText.contains("ERROR! Only 1 out of 2 test scripts succeeded!"))
    assert(!testText.contains("Cleaning up temporary directory"))

    checkTempDirAndRemove(testText, expectedTmpDirStr, true)
  }

  test("Check failing build") {
    val testText = TestHelper.testMainException[RuntimeException](
      Array(
        "test",
        "-f", funcFile,
        "-p", platFailedBuildFile
      ))

    assert(testText.contains("Running tests in temporary directory: "))
    assert(testText.contains("ERROR! Setup failed!"))
    assert(!testText.contains("Cleaning up temporary directory"))

    checkTempDirAndRemove(testText, expectedTmpDirStr, true)
  }


  /**
   * Searches the output generated by Main.main() during tests for the temporary directory name and verifies if it still exists or not.
   * If directory was expected to be present and actually is present, it will be removed.
   * @param testText the text generated by Main.main()
   * @param tmpDirText The path of the expected dir
   * @param expectDirectoryExists expect the directory to be present or not
   * @return
   */
  def checkTempDirAndRemove(testText: String, tmpDirText: String, expectDirectoryExists: Boolean) {
    // Get temporary directory
    val FolderRegex = ".*Running tests in temporary directory: '([^']*)'.*".r

    val tempPath = testText.replaceAll("\n", "") match {
      case FolderRegex(path) => path
      case _ => ""
    }

    assert(tempPath.contains(tmpDirText))

    val tempFolder = new Directory(Paths.get(tempPath).toFile)

    if (expectDirectoryExists) {
      // Check temporary directory is still present
      assert(tempFolder.exists)
      assert(tempFolder.isDirectory)

      // Remove the temporary directory
      tempFolder.deleteRecursively()
    }

    // folder should always have been removed at this stage
    assert(!tempFolder.exists)
  }
}
