package io.viash

import java.nio.file.{Files, Paths, StandardCopyOption}

import io.viash.helpers._
import org.scalatest.{BeforeAndAfterAll, FunSuite}

import scala.reflect.io.Directory
import sys.process._

class MainTestDockerSuite extends FunSuite with BeforeAndAfterAll {
  // default yaml
  private val configFile = getClass.getResource("/testbash/config.vsh.yaml").getPath

  private val temporaryFolder = IO.makeTemp(s"viash_${this.getClass.getName}_")
  private val tempFolStr = temporaryFolder.toString

  private val configInvalidYamlFile = getClass.getResource("/testbash/invalid_configs/config_invalid_yaml.vsh.yaml").getPath
  private val customPlatformFile = getClass.getResource("/testbash/platform_custom.yaml").getPath

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

  test("Prepare tests with derived configs, copy resources to temporary folder", NativeTest) {
    val rootPath = getClass.getResource(s"/testbash/").getPath
    TestHelper.copyFolder(rootPath, tempFolStr)
    
    val newConfigFilePath = deriveNewConfigWithYq(".", "default_config")

    val testText = TestHelper.testMain(
      "test",
      "-p", "native",
      newConfigFilePath
    )

    assert(testText.contains("Running tests in temporary directory: "))
    assert(testText.contains("SUCCESS! All 2 out of 2 test scripts succeeded!"))
    assert(testText.contains("Cleaning up temporary directory"))

    checkTempDirAndRemove(testText, false)
  }

  test("Check test output when no tests are specified in the functionality file", NativeTest) {
    val newConfigFilePath = deriveNewConfigWithYq("del(.functionality.test_resources)", "no_tests")
    val testText = TestHelper.testMain(
      "test",
      "-p", "native",
      newConfigFilePath
    )

    assert(testText.contains("Running tests in temporary directory: "))
    assert(testText.contains("WARNING! No tests found!"))
    assert(testText.contains("Cleaning up temporary directory"))

    checkTempDirAndRemove(testText, false)
  }

  test("Check test output when a test fails", NativeTest) {
    val newConfigFilePath = deriveNewConfigWithYq(""".functionality.test_resources[0].path="tests/fail_failed_test.sh"""", "failed_test")
    val testText = TestHelper.testMainException[RuntimeException](
      "test",
      "-p", "native",
      newConfigFilePath
    )

    assert(testText.contains("Running tests in temporary directory: "))
    assert(testText.contains("ERROR! Only 1 out of 2 test scripts succeeded!"))
    assert(!testText.contains("Cleaning up temporary directory"))

    checkTempDirAndRemove(testText, true)
  }

  test("Check failing build", DockerTest) {
    val newConfigFilePath = deriveNewConfigWithYq(""".platforms[1].apt.packages[0]="get_the_machine_that_goes_ping"""", "failed_build")
    val testOutput = TestHelper.testMainException2[RuntimeException](
      "test",
      "-p", "docker",
      newConfigFilePath
    )

    assert(testOutput.exceptionText == "Setup failed!")

    assert(testOutput.output.contains("Running tests in temporary directory: "))
    assert(testOutput.output.contains("ERROR! Setup failed!"))
    assert(!testOutput.output.contains("Cleaning up temporary directory"))

    checkTempDirAndRemove(testOutput.output, true)
  }

  test("Check test output when test doesn't exist", DockerTest) {
    val newConfigFilePath = deriveNewConfigWithYq(""".functionality.test_resources[0].path="tests/nonexistent_test.sh"""", "nonexisting_test")
    val testOutput = TestHelper.testMainException2[RuntimeException](
      "test",
      "-p", "docker",
      newConfigFilePath
    )

    assert(testOutput.exceptionText == "Only 1 out of 2 test scripts succeeded!")

    assert(testOutput.output.contains("Running tests in temporary directory: "))
    assert(testOutput.output.contains("ERROR! Only 1 out of 2 test scripts succeeded!"))
    assert(!testOutput.output.contains("Cleaning up temporary directory"))

    checkTempDirAndRemove(testOutput.output, true)
  }

  test("Check config and resource files with spaces in the filename", DockerTest) {
    val newConfigFilePath = Paths.get(tempFolStr, "config with spaces.vsh.yaml")
    Files.copy(Paths.get(configFile), newConfigFilePath)
    val testText = TestHelper.testMain(
      "test",
      "-p", "docker",
      newConfigFilePath.toString()
    )

    assert(testText.contains("Running tests in temporary directory: "))
    assert(testText.contains("SUCCESS! All 2 out of 2 test scripts succeeded!"))
    assert(testText.contains("Cleaning up temporary directory"))

    checkTempDirAndRemove(testText, false)
  }

  test("Check standard test output with legacy 'tests' definition", DockerTest) {
    val newConfigFilePath = deriveNewConfigWithYq("""with(.functionality ; .tests=.test_resources | del(.test_resources))""", "legacy")
    val (stdout, stderr) = TestHelper.testMainWithStdErr(
      "test",
      "-p", "docker",
      newConfigFilePath
    )

    assert(stderr.contains("Notice: functionality.tests is deprecated. Please use functionality.test_resources instead."))

    assert(stdout.contains("Running tests in temporary directory: "))
    assert(stdout.contains("SUCCESS! All 2 out of 2 test scripts succeeded!"))
    assert(stdout.contains("Cleaning up temporary directory"))

    checkTempDirAndRemove(stdout, false)
  }

  test("Check config file without 'functionality' specified", DockerTest) {
    val newConfigFilePath = deriveNewConfigWithYq(""".unctionality=.functionality | del(.functionality)""", "missing_functionality")
    val testOutput = TestHelper.testMainException2[RuntimeException](
      "test",
      "-p", "docker",
      newConfigFilePath
    )

    assert(testOutput.exceptionText.contains("must be a yaml file containing a viash config."))
    assert(testOutput.output.isEmpty)
  }

  test("Check valid viash config yaml but with wrong file extension") {
    val newConfigFilePath = Paths.get(tempFolStr, "config.txt")
    Files.copy(Paths.get(configFile), newConfigFilePath)
    val testOutput = TestHelper.testMainException2[RuntimeException](
      "test",
      "-p", "docker",
      newConfigFilePath.toString()
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
    val newConfigFilePath = deriveNewConfigWithYq(""".functionality.test_resources[0].path="tests/fail_failed_test.sh"""", "failed_test_keep_true")
    val testOutput = TestHelper.testMainException2[RuntimeException](
      "test",
      "-p", "native",
      "-k", "true",
      newConfigFilePath
    )

    assert(testOutput.exceptionText == "Only 1 out of 2 test scripts succeeded!")

    assert(testOutput.output.contains("Running tests in temporary directory: "))
    assert(testOutput.output.contains("ERROR! Only 1 out of 2 test scripts succeeded!"))
    assert(!testOutput.output.contains("Cleaning up temporary directory"))

    checkTempDirAndRemove(testOutput.output, true)
  }

  test("Check test output when a test fails and --keep false is specified", NativeTest) {
    val newConfigFilePath = deriveNewConfigWithYq(""".functionality.test_resources[0].path="tests/fail_failed_test.sh"""", "failed_test_keep_false")
    val testOutput = TestHelper.testMainException2[RuntimeException](
      "test",
      "-p", "native",
      "-k", "false",
      newConfigFilePath
    )

    assert(testOutput.exceptionText == "Only 1 out of 2 test scripts succeeded!")

    assert(testOutput.output.contains("Running tests in temporary directory: "))
    assert(testOutput.output.contains("ERROR! Only 1 out of 2 test scripts succeeded!"))
    assert(testOutput.output.contains("Cleaning up temporary directory"))

    checkTempDirAndRemove(testOutput.output, false)
  }

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

  /**
    * Derive a config file from the default config
    * @param cmd Command to pass to yq
    * @param name name of .sh and .vsh.yaml files
    * @return Path to the new config file as string
    */
  def deriveNewConfigWithYq(cmd: String, name: String): String = {
    val shFileStr = s"$name.sh"
    val yamlFileStr = s"$name.vsh.yaml"
    val newConfigFilePath = Paths.get(tempFolStr, s"$yamlFileStr")
    Files.copy(Paths.get(configFile), newConfigFilePath)

    val fullCmd = s"""yq '$cmd' -i ./$yamlFileStr"""
    val newConfigShPath = Paths.get(tempFolStr, shFileStr)
    Files.write(newConfigShPath, fullCmd.getBytes())

    Exec.run(
      Seq("sh", shFileStr),
      cwd = Some(Paths.get(tempFolStr).toFile())
    )

    newConfigFilePath.toString
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

  override def afterAll() {
    IO.deleteRecursively(temporaryFolder)
  }
}
