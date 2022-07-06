package com.dataintuitive.viash

import java.nio.file.{Files, Paths, StandardCopyOption}

import com.dataintuitive.viash.helpers.IO
import org.scalatest.{BeforeAndAfterAll, FunSuite}

import scala.reflect.io.Directory

class MainTestDockerSuite extends FunSuite with BeforeAndAfterAll {
  // default yaml
  private val configFile = getClass.getResource("/testbash/config.vsh.yaml").getPath

  // workaround for having no '.vsh.' in the filename as to bypass detection of namespace command
  private val configNoTestFile = getClass.getResource("/testbash/config_no_tests.vsh.yaml").getPath
  private val configFailedTestFile = getClass.getResource("/testbash/config_failed_test.vsh.yaml").getPath
  private val configFailedBuildFile = getClass.getResource("/testbash/config_failed_build.vsh.yaml").getPath
  private val configNonexistentTestFile = getClass.getResource("/testbash/config_nonexistent_test.vsh.yaml").getPath
  private val configWithSpacesFile = getClass.getResource("/testbash/config test.vsh.yaml").getPath.replaceAll("%20", " ")
  private val configLegacyTestFile = getClass.getResource("/testbash/config_legacy.vsh.yaml").getPath

  private val configMissingFunctionalityFile = getClass.getResource("/testbash/invalid_configs/config_missing_functionality.vsh.yaml").getPath
  private val configTextFile = getClass.getResource("/testbash/invalid_configs/config.txt").getPath
  private val configInvalidYamlFile = getClass.getResource("/testbash/invalid_configs/config_invalid_yaml.vsh.yaml").getPath

  // custom platform yamls
  private val customPlatformFile = getClass.getResource("/testbash/platform_custom.yaml").getPath

  //<editor-fold desc="Check behavior relative normal behavior such as success, no tests, failed tests, failed build">
  test("Check standard test output for typical outputs", DockerTest) {
    val testText = TestHelper.testMain(
      "test",
      "-p", "docker",
      configFile
    )

    assert(testText.contains("Running tests in temporary directory: "))
    assert(testText.contains("SUCCESS! All 2 out of 2 test scripts succeeded!"))
    assert(testText.contains("Cleaning up temporary directory"))

    checkTempDirAndRemove(testText, false)
  }

  test("Check standard test output with trailing arguments", DockerTest) {
    val testText = TestHelper.testMain(
      "test",
      configFile,
      "-p", "docker"
    )

    assert(testText.contains("Running tests in temporary directory: "))
    assert(testText.contains("SUCCESS! All 2 out of 2 test scripts succeeded!"))
    assert(testText.contains("Cleaning up temporary directory"))

    checkTempDirAndRemove(testText, false)
  }

  test("Check standard test output with leading and trailing arguments", DockerTest) {
    val testText = TestHelper.testMain(
      "test",
      "-p", "docker",
      configFile,
      "-k", "false"
    )

    assert(testText.contains("Running tests in temporary directory: "))
    assert(testText.contains("SUCCESS! All 2 out of 2 test scripts succeeded!"))
    assert(testText.contains("Cleaning up temporary directory"))

    checkTempDirAndRemove(testText, false)
  }

  test("Check test output when no tests are specified in the functionality file", NativeTest) {
    val testText = TestHelper.testMain(
      "test",
      "-p", "native",
      configNoTestFile
    )

    assert(testText.contains("Running tests in temporary directory: "))
    assert(testText.contains("WARNING! No tests found!"))
    assert(testText.contains("Cleaning up temporary directory"))

    checkTempDirAndRemove(testText, false)
  }

  test("Check test output when a test fails", NativeTest) {
    val testText = TestHelper.testMainException[RuntimeException](
      "test",
      "-p", "native",
      configFailedTestFile
    )

    assert(testText.contains("Running tests in temporary directory: "))
    assert(testText.contains("ERROR! Only 0 out of 1 test scripts succeeded!"))
    assert(!testText.contains("Cleaning up temporary directory"))

    checkTempDirAndRemove(testText, true)
  }

  test("Check failing build", DockerTest) {
    val testOutput = TestHelper.testMainException2[RuntimeException](
      "test", configFailedBuildFile
    )

    assert(testOutput.exceptionText == "Setup failed!")

    assert(testOutput.output.contains("Running tests in temporary directory: "))
    assert(testOutput.output.contains("ERROR! Setup failed!"))
    assert(!testOutput.output.contains("Cleaning up temporary directory"))

    checkTempDirAndRemove(testOutput.output, true)
  }

  test("Check test output when test doesn't exist", DockerTest) {
    val testOutput = TestHelper.testMainException2[RuntimeException](
      "test", configNonexistentTestFile
    )

    assert(testOutput.exceptionText == "Only 0 out of 1 test scripts succeeded!")

    assert(testOutput.output.contains("Running tests in temporary directory: "))
    assert(testOutput.output.contains("ERROR! Only 0 out of 1 test scripts succeeded!"))
    assert(!testOutput.output.contains("Cleaning up temporary directory"))

    checkTempDirAndRemove(testOutput.output, true)
  }

  test("Check config and resource files with spaces in the filename", DockerTest) {
    val testText = TestHelper.testMain(
      "test",
      "-p", "docker",
      configWithSpacesFile
    )

    assert(testText.contains("Running tests in temporary directory: "))
    assert(testText.contains("SUCCESS! All 2 out of 2 test scripts succeeded!"))
    assert(testText.contains("Cleaning up temporary directory"))

    checkTempDirAndRemove(testText, false)
  }

  test("Check standard test output with legacy 'tests' definition", DockerTest) {
    val (stdout, stderr) = TestHelper.testMainWithStdErr(
      "test",
      "-p", "docker",
      configLegacyTestFile
    )

    assert(stderr.contains("Notice: functionality.tests is deprecated. Please use functionality.test_resources instead."))

    assert(stdout.contains("Running tests in temporary directory: "))
    assert(stdout.contains("SUCCESS! All 2 out of 2 test scripts succeeded!"))
    assert(stdout.contains("Cleaning up temporary directory"))

    checkTempDirAndRemove(stdout, false)
  }
  //</editor-fold>
  //<editor-fold desc="Invalid config files">
  test("Check config file without 'functionality' specified", DockerTest) {
    val testOutput = TestHelper.testMainException2[RuntimeException](
      "test",
      "-p", "docker",
      configMissingFunctionalityFile
    )

    assert(testOutput.exceptionText.contains("must be a yaml file containing a viash config."))
    assert(testOutput.output.isEmpty)
  }

  test("Check valid viash config yaml but with wrong file extension") {
    val testOutput = TestHelper.testMainException2[RuntimeException](
      "test",
      "-p", "docker",
      configTextFile
    )

    assert(testOutput.exceptionText.contains("must be a yaml file containing a viash config."))
    assert(testOutput.output.isEmpty)
  }

  test("Check invalid viash config yaml") {
    val testOutput = TestHelper.testMainException2[io.circe.ParsingFailure](
      "test",
      "-p", "docker",
      configInvalidYamlFile
    )

    assert(testOutput.exceptionText.contains("while parsing a flow mapping"))
    assert(testOutput.output.isEmpty)
  }
  //</editor-fold>
  //<editor-fold desc="Check behavior of successful and failed tests with -keep flag specified">
  test("Check output in case --keep true is specified", DockerTest) {
    val testText = TestHelper.testMain(
      "test",
      "-p", "docker",
      "-k", "true",
      configFile
    )

    assert(testText.contains("Running tests in temporary directory: "))
    assert(testText.contains("SUCCESS! All 2 out of 2 test scripts succeeded!"))
    assert(!testText.contains("Cleaning up temporary directory"))

    checkTempDirAndRemove(testText, true)
  }

  test("Check output in case --keep false is specified", DockerTest) {
    val testText = TestHelper.testMain(
      "test",
      "-p", "docker",
      "--keep", "false",
      configFile
    )

    assert(testText.contains("Running tests in temporary directory: "))
    assert(testText.contains("SUCCESS! All 2 out of 2 test scripts succeeded!"))
    assert(testText.contains("Cleaning up temporary directory"))

    checkTempDirAndRemove(testText, false)
  }

  test("Check test output when a test fails and --keep true is specified", NativeTest) {
    val testOutput = TestHelper.testMainException2[RuntimeException](
      "test",
      "-p", "native",
      "-k", "true",
      configFailedTestFile
    )

    assert(testOutput.exceptionText == "Only 0 out of 1 test scripts succeeded!")

    assert(testOutput.output.contains("Running tests in temporary directory: "))
    assert(testOutput.output.contains("ERROR! Only 0 out of 1 test scripts succeeded!"))
    assert(!testOutput.output.contains("Cleaning up temporary directory"))

    checkTempDirAndRemove(testOutput.output, true)
  }

  test("Check test output when a test fails and --keep false is specified", NativeTest) {
    val testOutput = TestHelper.testMainException2[RuntimeException](
      "test",
      "-p", "native",
      "-k", "false",
      configFailedTestFile
    )

    assert(testOutput.exceptionText == "Only 0 out of 1 test scripts succeeded!")

    assert(testOutput.output.contains("Running tests in temporary directory: "))
    assert(testOutput.output.contains("ERROR! Only 0 out of 1 test scripts succeeded!"))
    assert(testOutput.output.contains("Cleaning up temporary directory"))

    checkTempDirAndRemove(testOutput.output, false)
  }
  //</editor-fold>
  //<editor-fold desc="Verify behavior of platform name specifications">
  test("Check standard test output with bad platform name", NativeTest) {
    val testOutput = TestHelper.testMainException2[RuntimeException](
      "test",
      "-p", "non_existing_platform",
      configFile
    )

    assert(testOutput.exceptionText == "platform must be a platform id specified in the config or a path to a platform yaml file.")
    assert(testOutput.output.isEmpty)
  }

  test("Check standard test output with custom platform file", DockerTest) {
    val testText = TestHelper.testMain(
      "test",
      "-p", customPlatformFile,
      configFile
    )

    assert(testText.contains("custom_target_image_tag")) // check whether custom package was picked up
    assert(testText.contains("Running tests in temporary directory: "))
    assert(testText.contains("SUCCESS! All 2 out of 2 test scripts succeeded!"))
    assert(testText.contains("Cleaning up temporary directory"))
  }
  //</editor-fold>

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
