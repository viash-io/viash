package io.viash.e2e.build

import io.viash._

import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import java.nio.file.Paths

import io.viash.config.Config

import scala.io.Source
import io.viash.helpers.{IO, Exec, Logger}
import io.viash.exceptions.ConfigParserException
import java.nio.file.Files
import io.viash.helpers.data_structures._

class NativeSuite extends AnyFunSuite with BeforeAndAfterAll {
  Logger.UseColorOverride.value = Some(false)
  // which configs to test
  private val configFile = getClass.getResource(s"/testbash/config.vsh.yaml").getPath
  private val configDeprecatedArgumentGroups = getClass.getResource(s"/testbash/config_deprecated_argument_groups.vsh.yaml").getPath
  private val configFunctionalityFile = getClass.getResource(s"/testbash/config_with_functionality.vsh.yaml").getPath

  private val temporaryFolder = IO.makeTemp("viash_tester")
  private val tempFolStr = temporaryFolder.toString

  private val temporaryConfigFolder = IO.makeTemp(s"viash_${this.getClass.getName}_")
  private val configDeriver = ConfigDeriver(Paths.get(configFile), temporaryConfigFolder)

  // parse config from file
  private val config = Config.read(configFile)

  // check whether executable was created
  private val executable = Paths.get(tempFolStr, config.name).toFile

  // convert testbash
  test("viash can create an executable") {
    TestHelper.testMain(
      "build",
      "--engine", "native",
      "--runner", "executable",
      "-o", tempFolStr,
      configFile
    )

    assert(executable.exists)
    assert(executable.canExecute)
  }

  test("Check whether the executable can run") {
    Exec.run(
      Seq(executable.toString, "--help")
    )
  }

  test("Check whether particular keywords can be found in the usage") {
    val stdout =
      Exec.run(
        Seq(executable.toString, "--help")
      )

    val stripAll = (s : String) => s.replaceAll(raw"\s+", " ").trim

    config.allArguments.foreach(arg => {
      for (opt <- arg.alternatives; value <- opt)
        assert(stdout.contains(value))
      for (description <- arg.description) {
        assert(stripAll(stdout).contains(stripAll(description)))
      }
    })
  }

  test("Check whether output is correctly created") {
    val output = Paths.get(tempFolStr, "output.txt").toFile
    val log = Paths.get(tempFolStr, "log.txt").toFile

    Exec.run(
      Seq(
        executable.toString,
        executable.toString,
        "--real_number", "10.5",
        "--whole_number=10",
        "-s", "a string with a few spaces",
        "a", "b", "c",
        "--truth",
        "--output", output.toString,
        "--log", log.toString,
        "--optional", "foo",
        "--optional_with_default", "bar",
        "--multiple", "foo",
        "--multiple=bar",
        "d", "e", "f"
      )
    )

    assert(output.exists())
    assert(log.exists())

    val outputSrc = Source.fromFile(output)
    try {
      val outputLines = outputSrc.mkString
      assert(outputLines.contains(s"""input: |${executable.toString}|"""))
      assert(outputLines.contains("""real_number: |10.5|"""))
      assert(outputLines.contains("""whole_number: |10|"""))
      assert(outputLines.contains("""s: |a string with a few spaces|"""))
      assert(outputLines.contains("""truth: |true|"""))
      assert(outputLines.contains(s"""output: |${output.toString}|"""))
      assert(outputLines.contains(s"""log: |${log.toString}|"""))
      assert(outputLines.contains("""optional: |foo|"""))
      assert(outputLines.contains("""optional_with_default: |bar|"""))
      assert(outputLines.contains("""multiple: |foo;bar|"""))
      assert(outputLines.contains("""multiple_pos: |a;b;c;d;e;f|"""))
      val regex = s"""meta_resources_dir: \\|.*$tempFolStr\\|""".r
      assert(regex.findFirstIn(outputLines).isDefined)
    } finally {
      outputSrc.close()
    }

    val logSrc = Source.fromFile(log)
    try {
      val logLines = logSrc.mkString
      assert(logLines.contains("INFO: Parsed input arguments"))
    } finally {
      logSrc.close()
    }
  }

  test("Alternative params") {
    val stdout =
      Exec.run(
        Seq(
          executable.toString,
          executable.toString,
          "--real_number", "123.456",
          "--whole_number", "789",
          "-s", "my$weird#string"
        )
      )

    assert(stdout.contains(s"""input: |${executable.toString}|"""))
    assert(stdout.contains("""real_number: |123.456|"""))
    assert(stdout.contains("""whole_number: |789|"""))
    assert(stdout.contains("""s: |my$weird#string|"""))
    assert(stdout.contains("""truth: |false|"""))
    assert(stdout.contains("""optional: ||"""))
    assert(stdout.contains("""optional_with_default: |The default value.|"""))
    assert(stdout.contains("""multiple: ||"""))
    assert(stdout.contains("""multiple_pos: ||"""))
    val regex = s"""meta_resources_dir: \\|.*$tempFolStr\\|""".r
    assert(regex.findFirstIn(stdout).isDefined)

    assert(stdout.contains("INFO: Parsed input arguments"))
  }

  test("Repeated regular arguments are not allowed") {
    val out = Exec.runCatch(
      Seq(
        executable.toString,
        executable.toString,
        "--real_number", "123.456",
        "--whole_number", "789",
        "-s", "my$weird#string",
        "--whole_number", "123",
      )
    )

    assert(out.exitValue == 1)
    assert(out.output.contains("[error] Bad arguments for option '--whole_number': '789' & '123' - you should provide exactly one argument for this option."))
  }

  test("Repeated flag arguments are not allowed") {
    val out = Exec.runCatch(
      Seq(
        executable.toString,
        executable.toString,
        "--real_number", "123.456",
        "--whole_number", "789",
        "-s", "my$weird#string",
        "--falsehood",
        "--falsehood"
      )
    )
    assert(out.exitValue == 1)
    assert(out.output.contains("[error] Bad arguments for option '--falsehood': 'false' & '' - you should provide exactly one argument for this option."))
  }

  test("Repeated arguments with --multiple defined are allowed") {
    val out = Exec.runCatch(
      Seq(
        executable.toString,
        executable.toString,
        "--real_number", "123.456",
        "--whole_number", "789",
        "-s", "my$weird#string",
        "--multiple", "foo",
        "--multiple", "bar"
      )
    )
    
    assert(out.exitValue == 0)
    assert(out.output.contains("multiple: |foo;bar|"))
  }

  test("when --runner is omitted, the system should run as native") {
    val newConfigFilePath = configDeriver.derive("""del(.runners)""", "no_runner")
    val testText = TestHelper.testMain(
      "build",
      "-o", tempFolStr,
      newConfigFilePath
    )

    assert(executable.exists)
    assert(executable.canExecute)

    Exec.run(
      Seq(executable.toString, "--help")
    )
  }

  test("Test whether a config with a platform specified gives a deprecation warning") {
    val newConfigFilePath = configDeriver.derive(""".platforms := [{ type: "native" }]""", "deprecated_platform")
    val testOutput = TestHelper.testMain(
      "build",
      "-o", tempFolStr,
      newConfigFilePath
    )

    val testRegex = "Warning: .platforms is deprecated: Use 'engines' and 'runners' instead.".r
    assert(testRegex.findFirstIn(testOutput.stderr).isDefined, testOutput)
  }

  test("Test Viash still supports the deprecated functionality structure and gives a deprecated warning") {
    val testOutput = TestHelper.testMain(
      "build",
      "-o", tempFolStr,
      configFunctionalityFile
    )

    val testRegex = "Warning: Functionality is deprecated: Functionality level is deprecated, all functionality fields are now located on the top level of the config file.".r
    assert(testRegex.findFirstIn(testOutput.stderr).isDefined, testOutput)
  }

  test("Test whether setting an internalFunctionality field throws an error") {
    val newConfigFilePath = configDeriver.derive(""".argument_groups[.name == "First group"].arguments[.name == "input"].dest := "foo"""", "set_internal_functionality")

    val testOutput = TestHelper.testMainException[ConfigParserException](
      "build",
      "-o", tempFolStr,
      newConfigFilePath
    )

    assert(testOutput.stderr.contains("Error: .argument_groups.arguments.dest is internal functionality."))
  }

  test("Test config without a main script") {
    val testOutput = TestHelper.testMain(
      "build",
      "-o", tempFolStr,
      configFile,
      "-c", ".resources := []"
    )

    assert(testOutput.stderr.contains("Warning: no resources specified!"))
  }

  test("Check data in build_info for executable runners") {
    val newConfigFilePath = configDeriver.derive(Nil, "build_info_native")
    Files.write(temporaryConfigFolder.resolve("_viash.yaml"), Array.emptyByteArray)

    val outputFolder = temporaryConfigFolder.resolve("output_build_info_native")
    
    val testOutput = TestHelper.testMain(
      workingDir = Some(temporaryConfigFolder),
      "build",
      "-o", outputFolder.toString,
      "--runner", "executable",
      newConfigFilePath
    )

    val executable = outputFolder.resolve("testbash").toFile
    val buildConfig = outputFolder.resolve(".config.vsh.yaml").toFile

    assert(executable.exists())
    assert(buildConfig.exists())

    val buildConfigFile = Source.fromFile(buildConfig)
    try {
      val buildOutput = buildConfigFile.mkString
      assert(buildOutput.contains("runner: \"executable\""))
      assert(buildOutput.contains("engine: \"native|docker|throwawayimage\""))
      assert(buildOutput.contains("executable: \"output_build_info_native/testbash\""))
    } finally {
      buildConfigFile.close()
    }
  }

  test("Check data in build_info for nextflow runners") {
    val newConfigFilePath = configDeriver.derive(Nil, "build_info_nextflow")
    Files.write(temporaryConfigFolder.resolve("_viash.yaml"), Array.emptyByteArray)

    val outputFolder = temporaryConfigFolder.resolve("output_build_info_nextflow")
    
    val testOutput = TestHelper.testMain(
      workingDir = Some(temporaryConfigFolder),
      "build",
      "-o", outputFolder.toString,
      "--runner", "nextflow",
      newConfigFilePath
    )

    val executable = outputFolder.resolve("main.nf").toFile
    val buildConfig = outputFolder.resolve(".config.vsh.yaml").toFile

    assert(executable.exists())
    assert(buildConfig.exists())

    val buildConfigFile = Source.fromFile(buildConfig)
    try {
      val buildOutput = buildConfigFile.mkString
      assert(buildOutput.contains("runner: \"nextflow\""))
      assert(buildOutput.contains("engine: \"native|docker|throwawayimage\""))
      assert(buildOutput.contains("executable: \"output_build_info_nextflow/main.nf\""))
    } finally {
      buildConfigFile.close()
    }
  }

  override def afterAll(): Unit = {
    IO.deleteRecursively(temporaryFolder)
    IO.deleteRecursively(temporaryConfigFolder)
  }
}
