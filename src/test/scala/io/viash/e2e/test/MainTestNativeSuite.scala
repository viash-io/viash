package io.viash.e2e.test

import io.viash._

import java.nio.file.{Files, Paths, StandardCopyOption}

import io.viash.helpers.{IO, Exec, Logger}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite

import scala.reflect.io.Directory
import sys.process._
import io.viash.exceptions.ConfigParserException
import io.viash.exceptions.MissingResourceFileException

class MainTestNativeSuite extends AnyFunSuite with BeforeAndAfterAll {
  Logger.UseColorOverride.value = Some(false)
  // default yaml
  private val configFile = getClass.getResource("/testbash/config.vsh.yaml").getPath

  private val temporaryFolder = IO.makeTemp(s"viash_${this.getClass.getName}_")
  private val tempFolStr = temporaryFolder.toString
  private val configDeriver = ConfigDeriver(Paths.get(configFile), temporaryFolder)

  private val configInvalidYamlFile = getClass.getResource("/testbash/invalid_configs/config_invalid_yaml.vsh.yaml").getPath

  test("Check standard test output for typical outputs") {
    val testOutput = TestHelper.testMain(
      "test",
      "--engine", "native",
      "--runner", "native",
      configFile
    )

    assert(testOutput.stdout.contains("Running tests in temporary directory: "))
    assert(testOutput.stdout.contains("SUCCESS! All 2 out of 2 test scripts succeeded!"))
    assert(testOutput.stdout.contains("Cleaning up temporary directory"))

    checkTempDirAndRemove(testOutput.stdout, false)
  }

  test("Check standard test output with trailing arguments") {
    val testOutput = TestHelper.testMain(
      "test",
      configFile,
      "--engine", "native",
      "--runner", "native"
    )

    assert(testOutput.stdout.contains("Running tests in temporary directory: "))
    assert(testOutput.stdout.contains("SUCCESS! All 2 out of 2 test scripts succeeded!"))
    assert(testOutput.stdout.contains("Cleaning up temporary directory"))

    checkTempDirAndRemove(testOutput.stdout, false)
  }

  test("Check standard test output with leading and trailing arguments") {
    val testOutput = TestHelper.testMain(
      "test",
      "--engine", "native",
      "--runner", "native",
      configFile,
      "-k", "false"
    )

    assert(testOutput.stdout.contains("Running tests in temporary directory: "))
    assert(testOutput.stdout.contains("SUCCESS! All 2 out of 2 test scripts succeeded!"))
    assert(testOutput.stdout.contains("Cleaning up temporary directory"))

    checkTempDirAndRemove(testOutput.stdout, false)
  }

  test("Verify base config derivation") {
    val newConfigFilePath = configDeriver.derive(Nil, "default_config")

    val testOutput = TestHelper.testMain(
      "test",
      "--engine", "native",
      "--runner", "native",
      newConfigFilePath
    )

    assert(testOutput.stdout.contains("Running tests in temporary directory: "))
    assert(testOutput.stdout.contains("SUCCESS! All 2 out of 2 test scripts succeeded!"))
    assert(testOutput.stdout.contains("Cleaning up temporary directory"))

    checkTempDirAndRemove(testOutput.stdout, false)
  }

  test("Check test output when no tests are specified in the functionality file") {
    val newConfigFilePath = configDeriver.derive("del(.functionality.test_resources)", "no_tests")
    val testOutput = TestHelper.testMain(
      "test",
      "--engine", "native",
      "--runner", "native",
      newConfigFilePath
    )

    assert(testOutput.stdout.contains("Running tests in temporary directory: "))
    assert(testOutput.stdout.contains("WARNING! No tests found!"))
    assert(testOutput.stdout.contains("Cleaning up temporary directory"))

    checkTempDirAndRemove(testOutput.stdout, false)
  }

  test("Check test output when a test fails") {
    val newConfigFilePath = configDeriver.derive(""".functionality.test_resources[.path == "tests/check_outputs.sh"].path := "tests/fail_failed_test.sh"""", "failed_test")
    val testOutput = TestHelper.testMainException[RuntimeException](
      "test",
      "--engine", "native",
      "--runner", "native",
      newConfigFilePath
    )

    assert(testOutput.stdout.contains("Running tests in temporary directory: "))
    assert(testOutput.stdout.contains("ERROR! Only 1 out of 2 test scripts succeeded!"))
    assert(!testOutput.stdout.contains("Cleaning up temporary directory"))

    checkTempDirAndRemove(testOutput.stdout, true)
  }

  test("Check test output when test doesn't exist") {
    val newConfigFilePath = configDeriver.derive(""".functionality.test_resources[.path == "tests/check_outputs.sh"].path := "tests/nonexistent_test.sh"""", "nonexisting_test")
    val testOutput = TestHelper.testMainException[RuntimeException](
      "test",
      "--engine", "native",
      "--runner", "native",
      newConfigFilePath
    )

    assert(testOutput.exceptionText.get == "Only 1 out of 2 test scripts succeeded!")

    assert(testOutput.stdout.contains("Running tests in temporary directory: "))
    assert(testOutput.stdout.contains("ERROR! Only 1 out of 2 test scripts succeeded!"))
    assert(!testOutput.stdout.contains("Cleaning up temporary directory"))

    checkTempDirAndRemove(testOutput.stdout, true)
  }

  test("Check config and resource files with spaces in the filename") {
    val newConfigFilePath = Paths.get(tempFolStr, "config with spaces.vsh.yaml")
    Files.copy(Paths.get(configFile), newConfigFilePath)
    val testOutput = TestHelper.testMain(
      "test",
      "--engine", "native",
      "--runner", "native",
      newConfigFilePath.toString()
    )

    assert(testOutput.stdout.contains("Running tests in temporary directory: "))
    assert(testOutput.stdout.contains("SUCCESS! All 2 out of 2 test scripts succeeded!"))
    assert(testOutput.stdout.contains("Cleaning up temporary directory"))

    checkTempDirAndRemove(testOutput.stdout, false)
  }

  test("Check config file without 'functionality' specified") {
    val newConfigFilePath = configDeriver.derive("""del(.functionality)""", "missing_functionality")
    val testOutput = TestHelper.testMainException[RuntimeException](
      "test",
      "--engine", "native",
      "--runner", "native",
      newConfigFilePath
    )

    assert(testOutput.exceptionText.get.contains("must be a yaml file containing a viash config."))
    assert(testOutput.stdout.isEmpty)
  }

  test("Check invalid runner type") {
    val newConfigFilePath = configDeriver.derive(""".runners += { type: "foo" }""", "invalid_runner_type")
    val testOutput = TestHelper.testMainException[ConfigParserException](
      "test",
      "--engine", "native",
      "--runner", "native",
      newConfigFilePath
    )

    assert(testOutput.exceptionText.get.contains("Type 'foo' is not recognised. Valid types are 'executable', and 'nextflow'."))
    assert(testOutput.exceptionText.get.contains(
      """{
        |  "type" : "foo"
        |}""".stripMargin))
    assert(testOutput.stdout.isEmpty)
  }

  test("Check invalid engine type") {
    val newConfigFilePath = configDeriver.derive(""".engines += { type: "foo" }""", "invalid_engine_type")
    val testOutput = TestHelper.testMainException[ConfigParserException](
      "test",
      "--engine", "native",
      "--runner", "native",
      newConfigFilePath
    )

    assert(testOutput.exceptionText.get.contains("Type 'foo' is not recognised. Valid types are 'docker', and 'native'."))
    assert(testOutput.exceptionText.get.contains(
      """{
        |  "type" : "foo"
        |}""".stripMargin))
    assert(testOutput.stdout.isEmpty)
  }

  test("Check invalid platform type") {
    val newConfigFilePath = configDeriver.derive(""".platforms := [{ type: "foo" }]""", "invalid_platform_type")
    val testOutput = TestHelper.testMainException[ConfigParserException](
      "test",
      "--engine", "native",
      "--runner", "native",
      newConfigFilePath
    )

    assert(testOutput.exceptionText.get.contains("Type 'foo' is not recognised. Valid types are 'docker', 'native', and 'nextflow'."))
    assert(testOutput.exceptionText.get.contains(
      """{
        |  "type" : "foo"
        |}""".stripMargin))
    assert(testOutput.stdout.isEmpty)
  }

  test("Check invalid field in runner") {
    val newConfigFilePath = configDeriver.derive(""".runners += { type: "executable", foo: "bar" }""", "invalid_runner_field")
    val testOutput = TestHelper.testMainException[ConfigParserException](
      "test",
      "--engine", "native",
      "--runner", "native",
      newConfigFilePath
    )

    assert(testOutput.exceptionText.get.contains("Invalid data fields for ExecutableRunner."))
    assert(testOutput.exceptionText.get.contains(
      """{
        |  "type" : "executable",
        |  "foo" : "bar"
        |}""".stripMargin))
    assert(testOutput.stdout.isEmpty)
  }

  test("Check invalid field in engine") {
    val newConfigFilePath = configDeriver.derive(""".engines += { type: "native", foo: "bar" }""", "invalid_engine_field")
    val testOutput = TestHelper.testMainException[ConfigParserException](
      "test",
      "--engine", "native",
      "--runner", "native",
      newConfigFilePath
    )

    assert(testOutput.exceptionText.get.contains("Invalid data fields for NativeEngine."))
    assert(testOutput.exceptionText.get.contains(
      """{
        |  "type" : "native",
        |  "foo" : "bar"
        |}""".stripMargin))
    assert(testOutput.stdout.isEmpty)
  }

  test("Check invalid field in platform") {
    val newConfigFilePath = configDeriver.derive(""".platforms := [{ type: "native", foo: "bar" }]""", "invalid_platform_field")
    val testOutput = TestHelper.testMainException[ConfigParserException](
      "test",
      "--engine", "native",
      "--runner", "native",
      newConfigFilePath
    )

    assert(testOutput.exceptionText.get.contains("Invalid data fields for NativePlatform."))
    assert(testOutput.exceptionText.get.contains(
      """{
        |  "type" : "native",
        |  "foo" : "bar"
        |}""".stripMargin))
    assert(testOutput.stdout.isEmpty)
  }

  test("Check valid viash config yaml but with wrong file extension") {
    val newConfigFilePath = Paths.get(tempFolStr, "config.txt")
    Files.copy(Paths.get(configFile), newConfigFilePath)
    val testOutput = TestHelper.testMainException[RuntimeException](
      "test",
      "--engine", "native",
      "--runner", "native",
      newConfigFilePath.toString()
    )

    assert(testOutput.exceptionText.get.contains("must be a yaml file containing a viash config."))
    assert(testOutput.stdout.isEmpty)
  }

  test("Check invalid viash config yaml") {
    val testOutput = TestHelper.testMainException[org.yaml.snakeyaml.parser.ParserException](
      "test",
      "--engine", "native",
      "--runner", "native",
      configInvalidYamlFile
    )

    assert(testOutput.exceptionText.get.contains("while parsing a flow mapping"))
    assert(testOutput.stdout.isEmpty)
  }

  test("Check output in case --keep true is specified") {
    val testOutput = TestHelper.testMain(
      "test",
      "--engine", "native",
      "--runner", "native",
      "-k", "true",
      configFile
    )

    assert(testOutput.stdout.contains("Running tests in temporary directory: "))
    assert(testOutput.stdout.contains("SUCCESS! All 2 out of 2 test scripts succeeded!"))
    assert(!testOutput.stdout.contains("Cleaning up temporary directory"))

    checkTempDirAndRemove(testOutput.stdout, true)
  }

  test("Check output in case --keep false is specified") {
    val testOutput = TestHelper.testMain(
      "test",
      "--engine", "native",
      "--runner", "native",
      "--keep", "false",
      configFile
    )

    assert(testOutput.stdout.contains("Running tests in temporary directory: "))
    assert(testOutput.stdout.contains("SUCCESS! All 2 out of 2 test scripts succeeded!"))
    assert(testOutput.stdout.contains("Cleaning up temporary directory"))

    checkTempDirAndRemove(testOutput.stdout, false)
  }

  test("Check test output when a test fails and --keep true is specified") {
    val newConfigFilePath = configDeriver.derive(""".functionality.test_resources[.path == "tests/check_outputs.sh"].path := "tests/fail_failed_test.sh"""", "failed_test_keep_true")
    val testOutput = TestHelper.testMainException[RuntimeException](
      "test",
      "--engine", "native",
      "--runner", "native",
      "-k", "true",
      newConfigFilePath
    )

    assert(testOutput.exceptionText.get == "Only 1 out of 2 test scripts succeeded!")

    assert(testOutput.stdout.contains("Running tests in temporary directory: "))
    assert(testOutput.stdout.contains("ERROR! Only 1 out of 2 test scripts succeeded!"))
    assert(!testOutput.stdout.contains("Cleaning up temporary directory"))

    checkTempDirAndRemove(testOutput.stdout, true)
  }

  test("Check test output when a test fails and --keep false is specified") {
    val newConfigFilePath = configDeriver.derive(
      """.functionality.test_resources[.path == "tests/check_outputs.sh"].path := "tests/fail_failed_test.sh"""",
      "failed_test_keep_false"
    )
    val testOutput = TestHelper.testMainException[RuntimeException](
      "test",
      "--engine", "native",
      "--runner", "native",
      "-k", "false",
      newConfigFilePath
    )

    assert(testOutput.exceptionText.get == "Only 1 out of 2 test scripts succeeded!")

    assert(testOutput.stdout.contains("Running tests in temporary directory: "))
    assert(testOutput.stdout.contains("ERROR! Only 1 out of 2 test scripts succeeded!"))
    assert(testOutput.stdout.contains("Cleaning up temporary directory"))

    checkTempDirAndRemove(testOutput.stdout, false)
  }

  test("Check deprecation warning") {
    val newConfigFilePath = configDeriver.derive(""".functionality.status := "deprecated"""", "deprecated")
    val testOutput = TestHelper.testMain(
      "test",
      "--engine", "native",
      "--runner", "native",
      newConfigFilePath
    )

    assert(testOutput.exitCode == Some(0))
    assert(testOutput.stdout.contains("Running tests in temporary directory: "))
    assert(testOutput.stdout.contains("SUCCESS! All 2 out of 2 test scripts succeeded!"))
    assert(testOutput.stdout.contains("Cleaning up temporary directory"))

    assert(testOutput.stderr.contains("The status of the component 'testbash' is set to deprecated."))
    
    checkTempDirAndRemove(testOutput.stdout, false)
  }

  test("Check standard test output with bad engine name") {
    val testOutput = TestHelper.testMainException[RuntimeException](
      "test",
      "--engine", "non_existing_engine",
      "--runner", "native",
      configFile
    )

    assert(testOutput.exceptionText.get == "no engine id matching regex 'non_existing_engine' could not be found in the config.")
    assert(testOutput.stdout.isEmpty)
  }

  test("Check standard test output with bad runner name") {
    val testOutput = TestHelper.testMainException[RuntimeException](
      "test",
      "--engine", "native",
      "--runner", "non_existing_runner",
      configFile
    )

    assert(testOutput.exceptionText.get == "no runner id matching regex 'non_existing_runner' could not be found in the config.")
    assert(testOutput.stdout.isEmpty)
  }

  test("Check standard test output with missing test resource") {
    val newConfigFilePath = configDeriver.derive(""".functionality.test_resources += { type: 'file', path: 'foobar.txt' }""", "deprecated")

    val testOutput = TestHelper.testMainException[MissingResourceFileException](
      "test",
      "--engine", "native",
      "--runner", "native",
      newConfigFilePath
    )

    assert(testOutput.exceptionText.get.matches("Missing resource .*foobar\\.txt as specified in .*"))
  }

  test("Check config without specifying an engine", NativeTest) {
    val testOutput = TestHelper.testMain(
      "test",
      "--runner", "native",
      configFile
    )

    assert(!testOutput.stdout.contains("docker"))

    assert(testOutput.stdout.contains("Running tests in temporary directory: "))
    assert(testOutput.stdout.contains("SUCCESS! All 2 out of 2 test scripts succeeded!"))
    assert(testOutput.stdout.contains("Cleaning up temporary directory"))

    checkTempDirAndRemove(testOutput.stdout, false)
  }

  test("Check config without any engines", NativeTest) {
    val newConfigFilePath = configDeriver.derive("""del(.engines)""", "no_engines")
    val testOutput = TestHelper.testMain(
      "test",
      "--runner", "native",
      newConfigFilePath
    )

    assert(!testOutput.stdout.contains("docker"))

    assert(testOutput.stdout.contains("Running tests in temporary directory: "))
    assert(testOutput.stdout.contains("SUCCESS! All 2 out of 2 test scripts succeeded!"))
    assert(testOutput.stdout.contains("Cleaning up temporary directory"))

    checkTempDirAndRemove(testOutput.stdout, false)
  }

  test("Check config without any engines or runners", NativeTest) {
    val newConfigFilePath = configDeriver.derive(List("""del(.engines)""", """del(.runners)"""), "no_engines_or_runners")
    val testOutput = TestHelper.testMain(
      "test",
      // "engine", "native",
      newConfigFilePath
    )

    assert(!testOutput.stdout.contains("docker"))

    assert(testOutput.stdout.contains("Running tests in temporary directory: "))
    assert(testOutput.stdout.contains("SUCCESS! All 2 out of 2 test scripts succeeded!"))
    assert(testOutput.stdout.contains("Cleaning up temporary directory"))

    checkTempDirAndRemove(testOutput.stdout, false)
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
