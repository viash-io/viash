package io.viash.e2e.test

import io.viash._

import java.nio.file.{Files, Paths, StandardCopyOption}

import io.viash.helpers.{IO, Exec, Logger}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite

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
      "--runner", "executable",
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
      "--runner", "executable"
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
      "--runner", "executable",
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
      "--runner", "executable",
      newConfigFilePath
    )

    assert(testOutput.stdout.contains("Running tests in temporary directory: "))
    assert(testOutput.stdout.contains("SUCCESS! All 2 out of 2 test scripts succeeded!"))
    assert(testOutput.stdout.contains("Cleaning up temporary directory"))

    checkTempDirAndRemove(testOutput.stdout, false)
  }

  test("Check test output when no tests are specified in the component config") {
    val newConfigFilePath = configDeriver.derive("del(.test_resources)", "no_tests")
    val testOutput = TestHelper.testMain(
      "test",
      "--engine", "native",
      "--runner", "executable",
      newConfigFilePath
    )

    assert(testOutput.stdout.contains("Running tests in temporary directory: "))
    assert(testOutput.stdout.contains("WARNING! No tests found!"))
    assert(testOutput.stdout.contains("Cleaning up temporary directory"))

    checkTempDirAndRemove(testOutput.stdout, false)
  }

  test("Check test output when a test fails") {
    val newConfigFilePath = configDeriver.derive(""".test_resources[.path == "tests/check_outputs.sh"].path := "tests/fail_failed_test.sh"""", "failed_test")
    val testOutput = TestHelper.testMainException[RuntimeException](
      "test",
      "--engine", "native",
      "--runner", "executable",
      newConfigFilePath
    )

    assert(testOutput.stdout.contains("Running tests in temporary directory: "))
    assert(testOutput.stdout.contains("ERROR! Only 1 out of 2 test scripts succeeded!"))
    assert(!testOutput.stdout.contains("Cleaning up temporary directory"))

    checkTempDirAndRemove(testOutput.stdout, true)
  }

  test("Check test output when test doesn't exist") {
    val newConfigFilePath = configDeriver.derive(""".test_resources[.path == "tests/check_outputs.sh"].path := "tests/nonexistent_test.sh"""", "nonexisting_test")
    val testOutput = TestHelper.testMainException[RuntimeException](
      "test",
      "--engine", "native",
      "--runner", "executable",
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
      "--runner", "executable",
      newConfigFilePath.toString()
    )

    assert(testOutput.stdout.contains("Running tests in temporary directory: "))
    assert(testOutput.stdout.contains("SUCCESS! All 2 out of 2 test scripts succeeded!"))
    assert(testOutput.stdout.contains("Cleaning up temporary directory"))

    checkTempDirAndRemove(testOutput.stdout, false)
  }

  test("Check config file without 'name' specified") {
    // remove 'name' and any fields that contain 'name'
    val newConfigFilePath = configDeriver.derive("""del(.name); del(.argument_groups); del(.authors)""", "missing_name")
    val testOutput = TestHelper.testMainException[RuntimeException](
      "test",
      "--engine", "native",
      "--runner", "executable",
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
      "--runner", "executable",
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
      "--runner", "executable",
      newConfigFilePath
    )

    assert(testOutput.exceptionText.get.contains("Type 'foo' is not recognised. Valid types are 'docker', and 'native'."))
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
      "--runner", "executable",
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
      "--runner", "executable",
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

  test("Check valid viash config yaml but with wrong file extension") {
    val newConfigFilePath = Paths.get(tempFolStr, "config.txt")
    Files.copy(Paths.get(configFile), newConfigFilePath)
    val testOutput = TestHelper.testMainException[RuntimeException](
      "test",
      "--engine", "native",
      "--runner", "executable",
      newConfigFilePath.toString()
    )

    assert(testOutput.exceptionText.get.contains("must be a yaml file containing a viash config."))
    assert(testOutput.stdout.isEmpty)
  }

  test("Check invalid viash config yaml") {
    val testOutput = TestHelper.testMainException[org.yaml.snakeyaml.parser.ParserException](
      "test",
      "--engine", "native",
      "--runner", "executable",
      configInvalidYamlFile
    )

    assert(testOutput.exceptionText.get.contains("while parsing a block collection"), testOutput.exceptionText)
    assert(testOutput.stdout.isEmpty)
  }

  test("Check output in case --keep true is specified") {
    val testOutput = TestHelper.testMain(
      "test",
      "--engine", "native",
      "--runner", "executable",
      "-k", "true",
      configFile
    )

    assert(testOutput.stdout.contains("Running tests in temporary directory: "))
    assert(testOutput.stdout.contains("SUCCESS! All 2 out of 2 test scripts succeeded!"))
    assert(!testOutput.stdout.contains("Cleaning up temporary directory"))

    checkTempDirAndRemove(testOutput.stdout, true)
  }

  test("Check output in case --dry_run is specified") {
    val testOutput = TestHelper.testMain(
      "test",
      "--engine", "native",
      "--runner", "executable",
      "--dry_run",
      configFile
    )
    assert(testOutput.stdout.contains("Running tests in temporary directory: "))
    assert(testOutput.stdout.contains("Running dummy test script"))
    assert(testOutput.stdout.contains("SUCCESS! All 2 out of 2 test scripts succeeded!"))
    assert(testOutput.stdout.contains("Cleaning up temporary directory"))

    checkTempDirAndRemove(testOutput.stdout, false)
  }

  test("Check output in case --keep false is specified") {
    val testOutput = TestHelper.testMain(
      "test",
      "--engine", "native",
      "--runner", "executable",
      "--keep", "false",
      configFile
    )

    assert(testOutput.stdout.contains("Running tests in temporary directory: "))
    assert(testOutput.stdout.contains("SUCCESS! All 2 out of 2 test scripts succeeded!"))
    assert(testOutput.stdout.contains("Cleaning up temporary directory"))

    checkTempDirAndRemove(testOutput.stdout, false)
  }

  test("Check test output when a test fails and --keep true is specified") {
    val newConfigFilePath = configDeriver.derive(""".test_resources[.path == "tests/check_outputs.sh"].path := "tests/fail_failed_test.sh"""", "failed_test_keep_true")
    val testOutput = TestHelper.testMainException[RuntimeException](
      "test",
      "--engine", "native",
      "--runner", "executable",
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
      """.test_resources[.path == "tests/check_outputs.sh"].path := "tests/fail_failed_test.sh"""",
      "failed_test_keep_false"
    )
    val testOutput = TestHelper.testMainException[RuntimeException](
      "test",
      "--engine", "native",
      "--runner", "executable",
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
    val newConfigFilePath = configDeriver.derive(""".status := "deprecated"""", "deprecated")
    val testOutput = TestHelper.testMain(
      "test",
      "--engine", "native",
      "--runner", "executable",
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
      "--runner", "executable",
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
    val newConfigFilePath = configDeriver.derive(""".test_resources += { type: 'file', path: 'foobar.txt' }""", "deprecated")

    val testOutput = TestHelper.testMainException[MissingResourceFileException](
      "test",
      "--engine", "native",
      "--runner", "executable",
      newConfigFilePath
    )

    assert(testOutput.exceptionText.get.matches("Missing resource .*foobar\\.txt as specified in .*"))
  }

  test("Check config without specifying an engine", NativeTest) {
    val testOutput = TestHelper.testMain(
      "test",
      "--runner", "executable",
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
      "--runner", "executable",
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

  test("Check standard test output with deterministic build folder that exists but is empty") {
    // create a new unique temporary folder
    val temporaryFolder = IO.makeTemp(s"viash_${this.getClass.getName}_")
    val tempFolStr = temporaryFolder.getFileName().toString()

    val testOutput = TestHelper.testMain(
      "test",
      "--engine", "native",
      "--deterministic_working_directory", tempFolStr,
      configFile
    )

    assert(testOutput.stdout.contains(s"Running tests in temporary directory: '${temporaryFolder}'"))
    assert(testOutput.stdout.contains("SUCCESS! All 2 out of 2 test scripts succeeded!"))
    assert(testOutput.stdout.contains("Cleaning up temporary directory"))

    checkTempDirAndRemove(testOutput.stdout, false, tempFolStr)
  }

  test("Check standard test output with deterministic build folder that doesn't exist yet") {
    // create a new unique temporary folder to know what folder name to use
    val temporaryFolder = IO.makeTemp(s"viash_${this.getClass.getName}_")
    temporaryFolder.toFile().delete()
    val tempFolStr = temporaryFolder.getFileName().toString()

    val testOutput = TestHelper.testMain(
      "test",
      "--engine", "native",
      "--deterministic_working_directory", tempFolStr,
      configFile
    )

    assert(testOutput.stdout.contains(s"Running tests in temporary directory: '${temporaryFolder}'"))
    assert(testOutput.stdout.contains("SUCCESS! All 2 out of 2 test scripts succeeded!"))
    assert(testOutput.stdout.contains("Cleaning up temporary directory"))

    checkTempDirAndRemove(testOutput.stdout, false, tempFolStr)
  }

  test("Check standard test output with deterministic build folder that isn't empty") {
    // create a new unique temporary folder to know what folder name to use
    val temporaryFolder = IO.makeTemp(s"viash_${this.getClass.getName}_")
    IO.write("foo", temporaryFolder.resolve("foo.txt"))
    val tempFolStr = temporaryFolder.getFileName().toString()

    val testOutput = TestHelper.testMainException[RuntimeException](
      "test",
      "--engine", "native",
      "--deterministic_working_directory", tempFolStr,
      configFile
    )

    assert(testOutput.exceptionText.get.contains(s"${temporaryFolder} already exists and is not empty."))

    assert(testOutput.stdout.isEmpty())
    assert(testOutput.stderr.isEmpty())
  }

  /**
   * Searches the output generated by Main.main() during tests for the temporary directory name and verifies if it still exists or not.
   * If directory was expected to be present and actually is present, it will be removed.
   * @param testText the text generated by Main.main()
   * @param expectDirectoryExists expect the directory to be present or not
   * @return
   */
  def checkTempDirAndRemove(testText: String, expectDirectoryExists: Boolean, testDirName: String = "viash_test_testbash"): Unit = {
    // Get temporary directory
    val FolderRegex = ".*Running tests in temporary directory: '([^']*)'.*".r

    val tempPathStr = testText.replaceAll("\n", "") match {
      case FolderRegex(path) => path
      case _ => ""
    }

    assert(tempPathStr.contains(s"${IO.tempDir}/$testDirName"))

    val tempPath = Paths.get(tempPathStr)

    if (expectDirectoryExists) {
      // Check temporary directory is still present
      assert(Files.exists(tempPath))
      assert(Files.isDirectory(tempPath))

      // Remove the temporary directory
      IO.deleteRecursively(tempPath)
    }

    // folder should always have been removed at this stage
    assert(!Files.exists(tempPath))
  }

  override def afterAll(): Unit = {
    IO.deleteRecursively(temporaryFolder)
  }
}
