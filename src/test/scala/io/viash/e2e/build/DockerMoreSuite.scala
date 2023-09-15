package io.viash.e2e.build

import io.viash._

import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import java.nio.file.{Files, Paths, StandardCopyOption}
import io.viash.helpers.{IO, Exec, Logger}

import io.viash.config.Config

import scala.io.Source

class DockerMoreSuite extends AnyFunSuite with BeforeAndAfterAll {
  Logger.UseColorOverride.value = Some(false)
  // which platform to test
  private val configFile = getClass.getResource(s"/testbash/config.vsh.yaml").getPath

  private val temporaryFolder = IO.makeTemp("viash_tester")
  private val tempFolStr = temporaryFolder.toString
  private val temporaryConfigFolder = IO.makeTemp("viash_tester_configs")

  private val configDeriver = ConfigDeriver(Paths.get(configFile), temporaryConfigFolder)

  // parse functionality from file
  private val functionality = Config.read(configFile).functionality

  // check whether executable was created
  private val executable = Paths.get(tempFolStr, functionality.name).toFile
  private val execPathInDocker = Paths.get("/viash_automount", executable.getPath).toFile.toString


  test("Prepare base config derivation and verify", DockerTest) {
    val newConfigFilePath = configDeriver.derive(
      Nil,
      "commands_default"
    )
    
    val (stdout, _, _) = TestHelper.testMainWithStdErr(
      "build",
      "--engine", "docker",
      "--runner", "docker",
      "-o", tempFolStr,
      newConfigFilePath,
      "--setup", "alwaysbuild"
    )

    assert(stdout.matches("\\[notice\\] Building container 'testbash:0\\.1' with Dockerfile\\s*"), stdout)
  }

  test("Verify adding extra commands to verify", DockerTest) {
    val newConfigFilePath = configDeriver.derive(
      """.functionality.requirements := { commands: ["which", "bash", "ps", "grep"] }""",
      "commands_extra"
    )
    
    val (stdout, _, _) = TestHelper.testMainWithStdErr(
      "build",
      "--engine", "docker",
      "--runner", "docker",
      "-o", tempFolStr,
      newConfigFilePath,
      "--setup", "alwaysbuild"
    )

    assert(stdout.matches("\\[notice\\] Building container 'testbash:0\\.1' with Dockerfile\\s*"), stdout)
  }

  test("Verify base adding an extra required command that doesn't exist", DockerTest) {
    val newConfigFilePath = configDeriver.derive(
      """.functionality.requirements := { commands: ["which", "bash", "ps", "grep", "non_existing_command"] }""",
      "non_existing_command"
    )
    
    val stdout = TestHelper.testMain(
      "build",
      "--engine", "docker",
      "--runner", "docker",
      "-o", tempFolStr,
      newConfigFilePath,
      "--setup", "alwaysbuild"
    )

    assert(stdout.contains("[notice] Building container 'testbash:0.1' with Dockerfile"))
    assert(stdout.contains("[error] Docker container 'testbash:0.1' does not contain command 'non_existing_command'."))
  }

  test("Check deprecated warning", DockerTest) {
    val newConfigFilePath = configDeriver.derive(""".functionality.status := "deprecated"""", "deprecated")
    
    val (stdout, stderr, exitCode) = TestHelper.testMainWithStdErr(
      "build",
      "--engine", "docker",
      "--runner", "docker",
      "-o", tempFolStr,
      newConfigFilePath,
      "--setup", "alwaysbuild"
    )

    assert(stderr.contains("The status of the component 'testbash' is set to deprecated."))
    assert(exitCode == 0)
  }

  test("Check component works when multiple_sep is set to ;", DockerTest) {
    val newConfigFilePath = configDeriver.derive(
      """.functionality.argument_groups[true].arguments[.name == "--real_number"] := { "type": "double", "name": "--real_number", "multiple": true, "multiple_sep": ";", "min": 10, "max": 1000, "default": [10, 20, 30]}""",
      "multiple_sep"
    )
    
    val _ = TestHelper.testMainWithStdErr(
      "build",
      "--engine", "docker",
      "--runner", "docker",
      "-o", tempFolStr,
      newConfigFilePath,
      "--setup", "alwaysbuild"
    )

    val stdout =
      Exec.run(
        Seq(
          executable.toString,
          executable.toString,
          "--real_number", "123.456;123;789",
          "--whole_number", "789",
          "-s", "my$weird#string"
        )
      )
      
    assert(executable.exists)
    assert(executable.canExecute)

    assert(stdout.contains("""real_number: |123.456;123;789|"""))
  }


  override def afterAll(): Unit = {
    IO.deleteRecursively(temporaryFolder)
    IO.deleteRecursively(temporaryConfigFolder)
  }
}