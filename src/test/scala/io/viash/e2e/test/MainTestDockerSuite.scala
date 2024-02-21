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
    val testOutput = TestHelper.testMain(
      "test",
      "--engine", "docker",
      "--runner", "executable",
      configFile
    )

    assert(testOutput.stdout.contains("Running tests in temporary directory: "))
    assert(testOutput.stdout.contains("SUCCESS! All 2 out of 2 test scripts succeeded!"))
    assert(testOutput.stdout.contains("Cleaning up temporary directory"))

    checkTempDirAndRemove(testOutput.stdout, false)
  }

  test("Check standard test output with trailing arguments", DockerTest) {
    val testOutput = TestHelper.testMain(
      "test",
      configFile,
      "--engine", "docker",
      "--runner", "executable"
    )

    assert(testOutput.stdout.contains("Running tests in temporary directory: "))
    assert(testOutput.stdout.contains("SUCCESS! All 2 out of 2 test scripts succeeded!"))
    assert(testOutput.stdout.contains("Cleaning up temporary directory"))

    checkTempDirAndRemove(testOutput.stdout, false)
  }

  test("Check standard test output with leading and trailing arguments", DockerTest) {
    val testOutput = TestHelper.testMain(
      "test",
      "--engine", "docker",
      "--runner", "executable",
      configFile,
      "-k", "false"
    )

    assert(testOutput.stdout.contains("Running tests in temporary directory: "))
    assert(testOutput.stdout.contains("SUCCESS! All 2 out of 2 test scripts succeeded!"))
    assert(testOutput.stdout.contains("Cleaning up temporary directory"))

    checkTempDirAndRemove(testOutput.stdout, false)
  }

  test("Check setup strategy", DockerTest) {
    val newConfigFilePath = configDeriver.derive(""".engines[.type == "docker" && !has(.id) ].setup := [{ type: "docker", run: "echo 'Hello world!'" }]""", "cache_config")
    // first run to create cache entries
    val testOutput = TestHelper.testMain(
      "test",
      "--engine", "docker",
      "--runner", "executable",
      newConfigFilePath,
      "--keep", "false"
    )

    // Do a second run to check if forcing a docker build using setup works
    val testOutputNoCaching = TestHelper.testMain(
      "test",
      "--engine", "docker",
      "--runner", "executable",
      newConfigFilePath,
      "--setup", "build",
      "--keep", "false"
    )

    val regexBuildCache = raw"\n#\d \[\d/\d\] RUN echo 'Hello world!'\n#\d CACHED\n".r
    assert(!regexBuildCache.findFirstIn(testOutputNoCaching.stdout).isDefined, "Expected to not find caching.")

    // Do a third run to check caching
    val testOutputCaching = TestHelper.testMain(
      "test",
      "--engine", "docker",
      "--runner", "executable",
      newConfigFilePath,
      "--setup", "cb",
      "--keep", "false"
    )

    // retry once if it failed
    val testTextCachingWithRetry = 
      if (regexBuildCache.findFirstIn(testOutputCaching.stdout).isDefined) {
        testOutputCaching
      } else {
        checkTempDirAndRemove(testOutputCaching.stdout, false)
        
        TestHelper.testMain(
          "test",
          "--engine", "docker",
          "--runner", "executable",
          newConfigFilePath,
          "--setup", "cb",
          "--keep", "false"
        )
      }

    assert(regexBuildCache.findFirstIn(testTextCachingWithRetry.stdout).isDefined, "Expected to find caching.")

    checkTempDirAndRemove(testOutput.stdout, false)
    checkTempDirAndRemove(testOutputNoCaching.stdout, false)
    checkTempDirAndRemove(testTextCachingWithRetry.stdout, false)
  }

  test("Verify base config derivation", NativeTest) {
    val newConfigFilePath = configDeriver.derive(Nil, "default_config")
    val testOutput = TestHelper.testMain(
      "test",
      "--engine", "native",
      "--runner", "executable",
      newConfigFilePath
    )

    assert(testOutput.stdout.contains("Running tests in temporary directory: "))
    assert(testOutput.stdout.contains("SUCCESS! All 2 out of 2 test scripts succeeded!"))
    assert(testOutput.stdout.contains("Cleaning up temporary directory"))

    checkTempDirAndRemove(testOutput.stdout, false)
  }

  test("Check failing build", DockerTest) {
    val newConfigFilePath = configDeriver.derive(""".engines[.type == "docker" && !has(.id) ].setup := [{ type: "apt", packages: ["get_the_machine_that_goes_ping"] }]""", "failed_build")
    val testOutput = TestHelper.testMainException[RuntimeException](
      "test",
      "--engine", "docker",
      "--runner", "executable",
      newConfigFilePath
    )

    assert(testOutput.exceptionText.get == "Setup failed!")

    assert(testOutput.stdout.contains("Running tests in temporary directory: "))
    assert(testOutput.stdout.contains("ERROR! Setup failed!"))
    assert(!testOutput.stdout.contains("Cleaning up temporary directory"))

    checkTempDirAndRemove(testOutput.stdout, true)
  }

  test("Check config and resource files with spaces in the filename", DockerTest) {
    val newConfigFilePath = Paths.get(tempFolStr, "config with spaces.vsh.yaml")
    Files.copy(Paths.get(configFile), newConfigFilePath)
    val testOutput = TestHelper.testMain(
      "test",
      "--engine", "docker",
      "--runner", "executable",
      newConfigFilePath.toString()
    )

    assert(testOutput.stdout.contains("Running tests in temporary directory: "))
    assert(testOutput.stdout.contains("SUCCESS! All 2 out of 2 test scripts succeeded!"))
    assert(testOutput.stdout.contains("Cleaning up temporary directory"))

    checkTempDirAndRemove(testOutput.stdout, false)
  }

  test("Check config without native engine", DockerTest) {
    val newConfigFilePath = configDeriver.derive("""del(.engines[.type == "native"])""", "no_native_engine")
    val testOutput = TestHelper.testMain(
      "test",
      "--engine", "docker",
      "--runner", "executable",
      newConfigFilePath
    )

    assert(testOutput.stdout.contains("docker"))

    assert(testOutput.stdout.contains("Running tests in temporary directory: "))
    assert(testOutput.stdout.contains("SUCCESS! All 2 out of 2 test scripts succeeded!"))
    assert(testOutput.stdout.contains("Cleaning up temporary directory"))

    checkTempDirAndRemove(testOutput.stdout, false)
  }

  test("Check config without native engine, should pick first engine as default", DockerTest) {
    val newConfigFilePath = configDeriver.derive("""del(.engines[.type == "native"])""", "no_native_engine2")
    val testOutput = TestHelper.testMain(
      "test",
      "--runner", "executable",
      newConfigFilePath
    )

    assert(testOutput.stdout.contains("docker"))

    assert(testOutput.stdout.contains("Running tests in temporary directory: "))
    assert(testOutput.stdout.contains("SUCCESS! All 2 out of 2 test scripts succeeded!"))
    assert(testOutput.stdout.contains("Cleaning up temporary directory"))

    checkTempDirAndRemove(testOutput.stdout, false)
  }

  test("Check test resources are available during build", DockerTest) {
    val newConfigFilePath = configDeriver.derive(""".engines[.type == "docker" && !has(.id) ].test_setup := [{ type: "docker", copy: "resource2.txt /opt/resource2.txt" }, { type: "docker", run: '[ -f "/opt/resource2.txt" ]|| exit 8' }]""", "test_resources_during_build")
    val testText = TestHelper.testMain(
      "test",
      "--engine", "docker",
      newConfigFilePath
    )

    assert(testText.stdout.contains("Running tests in temporary directory: "))
    assert(testText.stdout.contains("SUCCESS! All 2 out of 2 test scripts succeeded!"))
    assert(testText.stdout.contains("Cleaning up temporary directory"))

    checkTempDirAndRemove(testText.stdout, false)
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
