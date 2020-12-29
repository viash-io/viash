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
  private val configNonexistentTestFile = getClass.getResource("/testbash/config_nonexistent_test.vsh.yaml").getPath

  private val configMissingFunctionalityFile = getClass.getResource("/testbash/invalid_configs/config_missing_functionality.vsh.yaml").getPath
  private val configTextFile = getClass.getResource("/testbash/invalid_configs/config.txt").getPath
  private val configInvalidYamlFile = getClass.getResource("/testbash/invalid_configs/config_invalid_yaml.vsh.yaml").getPath

  private val configResourcesCopyFile = getClass.getResource("/testbash/config_resource_test.vsh.yaml").getPath
  private val configResourcesUnsupportedProtocolFile = getClass.getResource("/testbash/config_resource_unsupported_protocol.vsh.yaml").getPath

  // custom platform yamls
  private val customPlatformFile = getClass.getResource("/testbash/platform_custom.yaml").getPath

  //<editor-fold desc="Check behavior relative normal behavior such as success, no tests, failed tests, failed build">
  test("Check standard test output for typical outputs", DockerTest) {
    val testText = TestHelper.testMain(
      Array(
        "test",
        "-p", "docker",
        configFile
      ))

    assert(testText.contains("Running tests in temporary directory: "))
    assert(testText.contains("SUCCESS! All 2 out of 2 test scripts succeeded!"))
    assert(testText.contains("Cleaning up temporary directory"))

    checkTempDirAndRemove(testText, false)
  }

  test("Check standard test output with trailing arguments", DockerTest) {
    val testText = TestHelper.testMain(
      Array(
        "test",
        configFile,
        "-p", "docker"
      ))

    assert(testText.contains("Running tests in temporary directory: "))
    assert(testText.contains("SUCCESS! All 2 out of 2 test scripts succeeded!"))
    assert(testText.contains("Cleaning up temporary directory"))

    checkTempDirAndRemove(testText, false)
  }

  test("Check standard test output with leading and trailing arguments", DockerTest) {
    val testText = TestHelper.testMain(
      Array(
        "test",
        "-p", "docker",
        configFile,
        "-k", "false"
      ))

    assert(testText.contains("Running tests in temporary directory: "))
    assert(testText.contains("SUCCESS! All 2 out of 2 test scripts succeeded!"))
    assert(testText.contains("Cleaning up temporary directory"))

    checkTempDirAndRemove(testText, false)
  }

  test("Check test output when no tests are specified in the functionality file", NativeTest) {
    val testText = TestHelper.testMain(
      Array(
        "test",
        "-p", "native",
        configNoTestFile
      ))

    assert(testText.contains("Running tests in temporary directory: "))
    assert(testText.contains("WARNING! No tests found!"))
    assert(testText.contains("Cleaning up temporary directory"))

    checkTempDirAndRemove(testText, false)
  }

  test("Check test output when a test fails", NativeTest) {
    val testText = TestHelper.testMainException[RuntimeException](Array(
      "test",
      "-p", "native",
      configFailedTestFile
    ))

    assert(testText.contains("Running tests in temporary directory: "))
    assert(testText.contains("ERROR! Only 0 out of 1 test scripts succeeded!"))
    assert(!testText.contains("Cleaning up temporary directory"))

    checkTempDirAndRemove(testText, true)
  }

  test("Check failing build", DockerTest) {
    val testOutput = TestHelper.testMainException2[RuntimeException](
      Array(
        "test", configFailedBuildFile
      ))

    assert(testOutput.exceptionText == "Setup failed!")

    assert(testOutput.output.contains("Running tests in temporary directory: "))
    assert(testOutput.output.contains("ERROR! Setup failed!"))
    assert(!testOutput.output.contains("Cleaning up temporary directory"))

    checkTempDirAndRemove(testOutput.output, true)
  }

  test("Check test output when test doesn't exist", DockerTest) {
    val testOutput = TestHelper.testMainException2[RuntimeException](
      Array(
        "test", configNonexistentTestFile
      ))

    assert(testOutput.exceptionText == "Only 0 out of 1 test scripts succeeded!")

    assert(testOutput.output.contains("Running tests in temporary directory: "))
    assert(testOutput.output.contains("ERROR! Only 0 out of 1 test scripts succeeded!"))
    assert(!testOutput.output.contains("Cleaning up temporary directory"))

    checkTempDirAndRemove(testOutput.output, true)
  }
  //</editor-fold>
  //<editor-fold desc="Invalid config files">
  test("Check config file without 'functionality' specified", DockerTest) {
    val testOutput = TestHelper.testMainException2[RuntimeException](
      Array(
        "test",
        "-p", "docker",
        configMissingFunctionalityFile
      ))

    assert(testOutput.exceptionText.contains("must be a yaml file containing a viash config."))
    assert(testOutput.output.isEmpty)
  }

  test("Check valid viash config yaml but with wrong file extension") {
    val testOutput = TestHelper.testMainException2[RuntimeException](
      Array(
        "test",
        "-p", "docker",
        configTextFile
      ))

    assert(testOutput.exceptionText.contains("must be a yaml file containing a viash config."))
    assert(testOutput.output.isEmpty)
  }

  test("Check invalid viash config yaml") {
    val testOutput = TestHelper.testMainException2[io.circe.ParsingFailure](
      Array(
        "test",
        "-p", "docker",
        configInvalidYamlFile
      ))

    assert(testOutput.exceptionText.contains("while parsing a flow mapping"))
    assert(testOutput.output.isEmpty)
  }
  //</editor-fold>
  //<editor-fold desc="Check behavior of successful and failed tests with -keep flag specified">
  test("Check output in case --keep true is specified", DockerTest) {
    val testText = TestHelper.testMain(
      Array(
        "test",
        "-p", "docker",
        "-k", "true",
        configFile
      ))

    assert(testText.contains("Running tests in temporary directory: "))
    assert(testText.contains("SUCCESS! All 2 out of 2 test scripts succeeded!"))
    assert(!testText.contains("Cleaning up temporary directory"))

    checkTempDirAndRemove(testText, true)
  }

  test("Check output in case --keep false is specified", DockerTest) {
    val testText = TestHelper.testMain(
      Array(
        "test",
        "-p", "docker",
        "--keep", "false",
        configFile
      ))

    assert(testText.contains("Running tests in temporary directory: "))
    assert(testText.contains("SUCCESS! All 2 out of 2 test scripts succeeded!"))
    assert(testText.contains("Cleaning up temporary directory"))

    checkTempDirAndRemove(testText, false)
  }

  test("Check test output when a test fails and --keep true is specified", NativeTest) {
    val testOutput = TestHelper.testMainException2[RuntimeException](Array(
      "test",
      "-p", "native",
      "-k", "true",
      configFailedTestFile
    ))

    assert(testOutput.exceptionText == "Only 0 out of 1 test scripts succeeded!")

    assert(testOutput.output.contains("Running tests in temporary directory: "))
    assert(testOutput.output.contains("ERROR! Only 0 out of 1 test scripts succeeded!"))
    assert(!testOutput.output.contains("Cleaning up temporary directory"))

    checkTempDirAndRemove(testOutput.output, true)
  }

  test("Check test output when a test fails and --keep false is specified", NativeTest) {
    val testOutput = TestHelper.testMainException2[RuntimeException](Array(
      "test",
      "-p", "native",
      "-k", "false",
      configFailedTestFile
    ))

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
      Array(
        "test",
        "-p", "non_existing_platform",
        configFile
      ))

    assert(testOutput.exceptionText == "platform must be a platform id specified in the config or a path to a platform yaml file.")
    assert(testOutput.output.isEmpty)
  }

  test("Check standard test output with custom platform file", NativeTest) {
    val testText = TestHelper.testMain(
      Array(
        "test",
        "-p", customPlatformFile,
        configFile
      ))

    assert(testText.contains("check_for_detecting_platform"))
    assert(testText.contains("Running tests in temporary directory: "))
    assert(testText.contains("SUCCESS! All 2 out of 2 test scripts succeeded!"))
    assert(testText.contains("Cleaning up temporary directory"))
  }
  //</editor-fold>
  //<editor-fold desc="Verify correct copying of resources">
  test("Check resources are copied from and to the correct location", DockerTest) {

    // copy some resources to /tmp/viash_tmp_resources/ so we can test absolute path resources
    val tmpFolderResourceSourceFile = Paths.get(getClass.getResource("/testbash/resource3.txt").getFile)

    val tmpFolderResourceDestinationFolder = Paths.get("/tmp/viash_tmp_resources/").toFile
    val tmpFolderResourceDestinationFile = Paths.get(tmpFolderResourceDestinationFolder.getPath, "resource3.txt")

    if (!tmpFolderResourceDestinationFolder.exists())
      tmpFolderResourceDestinationFolder.mkdir()

    Files.copy(tmpFolderResourceSourceFile, tmpFolderResourceDestinationFile, StandardCopyOption.REPLACE_EXISTING)

    // generate viash script
    val testText = TestHelper.testMain(
      Array(
        "test",
        "-p", "docker",
        "-k", "true",
        configResourcesCopyFile
      ))

    // basic checks to see if standard test/build was correct
    assert(testText.contains("Running tests in temporary directory: "))
    assert(testText.contains("WARNING! No tests found!"))
    assert(!testText.contains("Cleaning up temporary directory"))

    val FolderRegex = ".*Running tests in temporary directory: '([^']*)'.*".r

    val tempPath = testText.replaceAll("\n", "") match {
      case FolderRegex(path) => path
      case _ => ""
    }

    // List all expected resources and their md5sum
    val expectedResources = List(
      //("check_bash_version.sh", "0c3c134d4ff0ea3a4a3b32e09fb7c100"),
      ("code.sh", "efa9e1aa1c5f2a0b91f558ead5917c68"),
      ("NOTICE", "72227b5fda1a673b084aef2f1b580ec3"),
      ("resource1.txt", "bc9171172c4723589a247f99b838732d"),
      ("resource2.txt", "9cd530447200979dbf9e117915cbcc74"),
      ("resource_folder/resource_L1_1.txt", "51954bf10062451e683121e58d858417"),
      ("resource_folder/resource_L1_2.txt", "b43991c0ef5d15710faf976e02cbb206"),
      ("resource_folder/resource_L2/resource_L2_1.txt", "63165187f791a8dfff628ef8090e56ff"),
      ("target_folder/relocated_file_1.txt", "bc9171172c4723589a247f99b838732d"),
      ("target_folder/relocated_file_2.txt", "51954bf10062451e683121e58d858417"),
      ("target_folder/relocated_file_3.txt", "6b0e05ae3d38b7db48ebdfc564366bce"),
      ("resource3.txt", "aa2037b3d308bcb6a78a3d4fbf04b297"),
      ("target_folder/relocated_file_4.txt", "aa2037b3d308bcb6a78a3d4fbf04b297")
    )

    //Paths.get(tempPath, "build_executable", "resource_folder").toFile.setExecutable(true)

    // Check all resources can be found in the folder
    for ((name, md5sum) <- expectedResources) {
      val resourceFile = Paths.get(tempPath, "build_executable", name).toFile

      assert(resourceFile.exists, s"Could not find $name")

      val hash = TestHelper.computeHash(resourceFile.getPath)
      assert(hash == md5sum, s"Calculated md5sum doesn't match the given md5sum for $name")
    }

    Directory(tmpFolderResourceDestinationFolder).deleteRecursively()
    checkTempDirAndRemove(testText, true)
  }

  test("Check resources with unsupported format", DockerTest) {
    // generate viash script
    val testOutput = TestHelper.testMainException2[RuntimeException](
      Array(
        "test",
        "-p", "docker",
        "-k", "true",
        configResourcesUnsupportedProtocolFile
      ))

    assert(testOutput.exceptionText == "Unsupported scheme: ftp")

    // basic checks to see if standard test/build was correct
    assert(testOutput.output.contains("Running tests in temporary directory: "))
    assert(!testOutput.output.contains("WARNING! No tests found!"))
    assert(!testOutput.output.contains("Cleaning up temporary directory"))

    checkTempDirAndRemove(testOutput.output, true)
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
