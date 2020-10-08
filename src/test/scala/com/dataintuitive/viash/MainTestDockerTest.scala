package com.dataintuitive.viash

import java.nio.file.{Files, Paths, StandardCopyOption}

import com.dataintuitive.viash.helpers.IO
import org.scalatest.{BeforeAndAfterAll, FunSuite}

import scala.reflect.io.Directory

class MainTestDockerTest extends FunSuite with BeforeAndAfterAll {
  // default yaml
  private val configFile = getClass.getResource("/testbash/config.vsh.yaml").getPath

  // workaround for having no '.vsh.' in the filename as to bypass detection of namespace command
  private val configNoTestFile = getClass.getResource("/testbash/config_no_tests.vsh.yaml").getPath
  private val configFailedTestFile = getClass.getResource("/testbash/config_failed_test.vsh.yaml").getPath
  private val configFailedBuildFile = getClass.getResource("/testbash/config_failed_build.vsh.yaml").getPath

  // custom platform yamls
  private val customPlatformFile = getClass.getResource("/testbash/platform_custom.yaml").getPath


  test("Check standard test output for typical outputs", DockerTest) {
    val testText = TestHelper.testMain(
      Array(
        "test", configFile,
        "-p", "docker"
      ))

    assert(testText.contains("Running tests in temporary directory: "))
    assert(testText.contains("SUCCESS! All 2 out of 2 test scripts succeeded!"))
    assert(testText.contains("Cleaning up temporary directory"))

    checkTempDirAndRemove(testText, false)
  }

  test("Check output in case --keep true is specified", DockerTest) {
    val testText = TestHelper.testMain(
      Array(
        "test", configFile,
        "-p", "docker",
        "-k", "true"
      ))

    assert(testText.contains("Running tests in temporary directory: "))
    assert(testText.contains("SUCCESS! All 2 out of 2 test scripts succeeded!"))
    assert(!testText.contains("Cleaning up temporary directory"))

    checkTempDirAndRemove(testText, true)
  }

  test("Check output in case --keep false is specified", DockerTest) {
    val testText = TestHelper.testMain(
      Array(
        "test", configFile,
        "-p", "docker",
        "--keep", "false"
      ))

    assert(testText.contains("Running tests in temporary directory: "))
    assert(testText.contains("SUCCESS! All 2 out of 2 test scripts succeeded!"))
    assert(testText.contains("Cleaning up temporary directory"))

    checkTempDirAndRemove(testText, false)
  }

  test("Check test output when no tests are specified in the functionality file", NativeTest) {
    val testText = TestHelper.testMain(
      Array(
        "test", configNoTestFile,
        "-p", "native"
      ))

    assert(testText.contains("Running tests in temporary directory: "))
    assert(testText.contains("WARNING! No tests found!"))
    assert(testText.contains("Cleaning up temporary directory"))

    checkTempDirAndRemove(testText, false)
  }

  test("Check test output when a test fails", NativeTest) {
    val testText = TestHelper.testMainException[RuntimeException](Array(
      "test", configFailedTestFile,
      "-p", "native"
    ))

    assert(testText.contains("Running tests in temporary directory: "))
    assert(testText.contains("ERROR! Only 0 out of 1 test scripts succeeded!"))
    assert(!testText.contains("Cleaning up temporary directory"))

    checkTempDirAndRemove(testText, true)
  }

  test("Check test output when a test fails and --keep true is specified", NativeTest) {
    val testText = TestHelper.testMainException[RuntimeException](Array(
      "test", configFailedTestFile,
      "-p", "native",
      "-k", "true"
    ))

    assert(testText.contains("Running tests in temporary directory: "))
    assert(testText.contains("ERROR! Only 0 out of 1 test scripts succeeded!"))
    assert(!testText.contains("Cleaning up temporary directory"))

    checkTempDirAndRemove(testText, true)
  }

  test("Check test output when a test fails and --keep false is specified", NativeTest) {
    val testText = TestHelper.testMainException[RuntimeException](Array(
      "test", configFailedTestFile,
      "-p", "native",
      "-k", "false"
    ))

    assert(testText.contains("Running tests in temporary directory: "))
    assert(testText.contains("ERROR! Only 0 out of 1 test scripts succeeded!"))
    assert(testText.contains("Cleaning up temporary directory"))

    checkTempDirAndRemove(testText, false)
  }

  test("Check failing build", DockerTest) {
    val testText = TestHelper.testMainException[RuntimeException](
      Array(
        "test", configFailedBuildFile
      ))

    assert(testText.contains("Running tests in temporary directory: "))
    assert(testText.contains("ERROR! Setup failed!"))
    assert(!testText.contains("Cleaning up temporary directory"))

    checkTempDirAndRemove(testText, true)
  }


  test("Check standard test output with bad platform name", NativeTest) {
    val testText = TestHelper.testMainException[RuntimeException](
      Array(
        "test", configFile,
        "-p", "non_existing_platform"
      ))

    assert(!testText.contains("Running tests in temporary directory: "))
    assert(!testText.contains("SUCCESS! All 2 out of 2 test scripts succeeded!"))
    assert(!testText.contains("Cleaning up temporary directory"))
  }

  test("Check standard test output with custom platform file", NativeTest) {
    val testText = TestHelper.testMain(
      Array(
        "test", configFile,
        "-p", customPlatformFile
      ))

    assert(testText.contains("check_for_detecting_platform"))
    assert(testText.contains("Running tests in temporary directory: "))
    assert(testText.contains("SUCCESS! All 2 out of 2 test scripts succeeded!"))
    assert(testText.contains("Cleaning up temporary directory"))
  }

  /**
   * Searches the output generated by Main.main() during tests for the temporary directory name and verifies if it still exists or not.
   * If directory was expected to be present and actually is present, it will be removed.
   * @param testText the text generated by Main.main()
   * @param expectDirectoryExists expect the directory to be present or not
   * @return
   */
  def checkTempDirAndRemove(testText: String, expectDirectoryExists: Boolean) {
    // Get temporary directory
    val FolderRegex = ".*Running tests in temporary directory: '([^']*)'.*".r

    val tempPath = testText.replaceAll("\n", "") match {
      case FolderRegex(path) => path
      case _ => ""
    }

    assert(tempPath.contains(s"${IO.tempDir}/viash_test_testbash"))

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
