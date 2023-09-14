package io.viash.e2e.test

import io.viash._

import java.nio.file.{Files, Paths, StandardCopyOption}

import io.viash.helpers.{IO, Exec, Logger}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite

import scala.reflect.io.Directory
import sys.process._
import org.scalatest.ParallelTestExecution

class MainTestDockerSuite extends AnyFunSuite with BeforeAndAfterAll with ParallelTestExecution{
  Logger.UseColorOverride.value = Some(false)
  // default yaml
  private val configFile = getClass.getResource("/testbash/config.vsh.yaml").getPath

  private val temporaryFolder = IO.makeTemp(s"viash_${this.getClass.getName}_")
  private val tempFolStr = temporaryFolder.toString
  private val configDeriver = ConfigDeriver(Paths.get(configFile), temporaryFolder)

  test("Check standard test output for typical outputs", DockerTest) {
    val testText = TestHelper.testMain(
      "test",
      "--engine", "docker",
      "--runner", "docker",
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
      "--engine", "docker",
      "--runner", "docker"
    )

    assert(testText.contains("Running tests in temporary directory: "))
    assert(testText.contains("SUCCESS! All 2 out of 2 test scripts succeeded!"))
    assert(testText.contains("Cleaning up temporary directory"))

    checkTempDirAndRemove(testText, false)
  }

  test("Check standard test output with leading and trailing arguments", DockerTest) {
    val testText = TestHelper.testMain(
      "test",
      "--engine", "docker",
      "--runner", "docker",
      configFile,
      "-k", "false"
    )

    assert(testText.contains("Running tests in temporary directory: "))
    assert(testText.contains("SUCCESS! All 2 out of 2 test scripts succeeded!"))
    assert(testText.contains("Cleaning up temporary directory"))

    checkTempDirAndRemove(testText, false)
  }

  test("Check setup strategy", DockerTest) {
    val newConfigFilePath = configDeriver.derive(""".platforms[.type == "docker" && !has(.id) ].setup := [{ type: "docker", run: "echo 'Hello world!'" }]""", "cache_config")
    // first run to create cache entries
    val testText = TestHelper.testMain(
      "test",
      "--engine", "docker",
      "--runner", "docker",
      newConfigFilePath,
      "--keep", "false"
    )

    // Do a second run to check if forcing a docker build using setup works
    val testTextNoCaching = TestHelper.testMain(
      "test",
      "--engine", "docker",
      "--runner", "docker",
      newConfigFilePath,
      "--setup", "build",
      "--keep", "false"
    )

    val regexBuildCache = raw"\n#\d \[\d/\d\] RUN echo 'Hello world!'\n#\d CACHED\n".r
    assert(!regexBuildCache.findFirstIn(testTextNoCaching).isDefined, "Expected to not find caching.")

    // Do a third run to check caching
    val testTextCaching = TestHelper.testMain(
      "test",
      "--engine", "docker",
      "--runner", "docker",
      newConfigFilePath,
      "--setup", "cb",
      "--keep", "false"
    )
    
    assert(regexBuildCache.findFirstIn(testTextCaching).isDefined, "Expected to find caching.")

    checkTempDirAndRemove(testText, false)
    checkTempDirAndRemove(testTextCaching, false)
    checkTempDirAndRemove(testTextNoCaching, false)
  }

  test("Verify base config derivation", NativeTest) {
    val newConfigFilePath = configDeriver.derive(Nil, "default_config")
    val testText = TestHelper.testMain(
      "test",
      "--engine", "native",
      "--runner", "native",
      newConfigFilePath
    )

    assert(testText.contains("Running tests in temporary directory: "))
    assert(testText.contains("SUCCESS! All 2 out of 2 test scripts succeeded!"))
    assert(testText.contains("Cleaning up temporary directory"))

    checkTempDirAndRemove(testText, false)
  }

  test("Check failing build", DockerTest) {
    val newConfigFilePath = configDeriver.derive(""".platforms[.type == "docker" && !has(.id) ].setup := [{ type: "apt", packages: ["get_the_machine_that_goes_ping"] }]""", "failed_build")
    val testOutput = TestHelper.testMainException2[RuntimeException](
      "test",
      "--engine", "docker",
      "--runner", "docker",
      newConfigFilePath
    )

    assert(testOutput.exceptionText == "Setup failed!")

    assert(testOutput.output.contains("Running tests in temporary directory: "))
    assert(testOutput.output.contains("ERROR! Setup failed!"))
    assert(!testOutput.output.contains("Cleaning up temporary directory"))

    checkTempDirAndRemove(testOutput.output, true)
  }

  test("Check config and resource files with spaces in the filename", DockerTest) {
    val newConfigFilePath = Paths.get(tempFolStr, "config with spaces.vsh.yaml")
    Files.copy(Paths.get(configFile), newConfigFilePath)
    val testText = TestHelper.testMain(
      "test",
      "--engine", "docker",
      "--runner", "docker",
      newConfigFilePath.toString()
    )

    assert(testText.contains("Running tests in temporary directory: "))
    assert(testText.contains("SUCCESS! All 2 out of 2 test scripts succeeded!"))
    assert(testText.contains("Cleaning up temporary directory"))

    checkTempDirAndRemove(testText, false)
  }

  /**
   * Searches the output generated by Main.main() during tests for the temporary directory name and verifies if it still exists or not.
   * If directory was expected to be present and actually is present, it will be removed.
   * @param testText the text generated by Main.main()
   * @param expectDirectoryExists expect the directory to be present or not
   * @return
   */
  def checkTempDirAndRemove(testText: String, expectDirectoryExists: Boolean): Unit = {
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

  override def afterAll(): Unit = {
    IO.deleteRecursively(temporaryFolder)
  }
}
