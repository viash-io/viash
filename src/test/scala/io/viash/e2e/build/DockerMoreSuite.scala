package io.viash.e2e.build

import io.viash._

import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import java.nio.file.{Files, Paths, StandardCopyOption, Path}
import io.viash.helpers.{IO, Exec, Logger}

import io.viash.config.Config

import scala.io.Source

class DockerMoreSuite extends AnyFunSuite with BeforeAndAfterAll {
  Logger.UseColorOverride.value = Some(false)
  // which config to test
  private val configFile = getClass.getResource(s"/testbash/config.vsh.yaml").getPath

  private val temporaryFolder = IO.makeTemp("viash_tester")
  private val tempFolStr = temporaryFolder.toString
  private val temporaryConfigFolder = IO.makeTemp("viash_tester_configs")

  private val configDeriver = ConfigDeriver(Paths.get(configFile), temporaryConfigFolder)

  // parse config from file
  private val config = Config.read(configFile)

  // check whether executable was created
  private val executable = Paths.get(tempFolStr, config.name).toFile
  private val execPathInDocker = Paths.get("/viash_automount", executable.getPath).toFile.toString


  test("Prepare base config derivation and verify", DockerTest) {
    val newConfigFilePath = configDeriver.derive(
      Nil,
      "commands_default"
    )
    
    val testOutput = TestHelper.testMain(
      "build",
      "--engine", "docker",
      "--runner", "executable",
      "-o", tempFolStr,
      newConfigFilePath,
      "--setup", "alwaysbuild"
    )

    assert(testOutput.stdout.matches("\\[notice\\] Building container 'testbash:0\\.1' with Dockerfile\\s*"), testOutput.stdout)
  }

  test("Verify adding extra commands to verify", DockerTest) {
    val newConfigFilePath = configDeriver.derive(
      """.requirements := { commands: ["which", "bash", "ps", "grep"] }""",
      "commands_extra"
    )
    
    val testOutput = TestHelper.testMain(
      "build",
      "--engine", "docker",
      "--runner", "executable",
      "-o", tempFolStr,
      newConfigFilePath,
      "--setup", "alwaysbuild"
    )

    assert(testOutput.stdout.matches("\\[notice\\] Building container 'testbash:0\\.1' with Dockerfile\\s*"), testOutput.stdout)
  }

  test("Verify base adding an extra required command that doesn't exist", DockerTest) {
    val newConfigFilePath = configDeriver.derive(
      """.requirements := { commands: ["which", "bash", "ps", "grep", "non_existing_command"] }""",
      "non_existing_command"
    )
    
    val testOutput = TestHelper.testMain(
      "build",
      "--engine", "docker",
      "--runner", "executable",
      "-o", tempFolStr,
      newConfigFilePath,
      "--setup", "alwaysbuild"
    )

    assert(testOutput.stdout.contains("[notice] Building container 'testbash:0.1' with Dockerfile"))
    assert(testOutput.stdout.contains("[error] Docker container 'testbash:0.1' does not contain command 'non_existing_command'."))
  }

  test("Check deprecated warning", DockerTest) {
    val newConfigFilePath = configDeriver.derive(""".status := "deprecated"""", "deprecated")
    
    val testOutput = TestHelper.testMain(
      "build",
      "--engine", "docker",
      "--runner", "executable",
      "-o", tempFolStr,
      newConfigFilePath,
      "--setup", "alwaysbuild"
    )

    assert(testOutput.stderr.contains("The status of the component 'testbash' is set to deprecated."))
    assert(testOutput.exitCode == Some(0))
  }

  test("Check component works when multiple_sep is set to ;", DockerTest) {
    val newConfigFilePath = configDeriver.derive(
      """.argument_groups[true].arguments[.name == "--real_number"] := { "type": "double", "name": "--real_number", "multiple": true, "multiple_sep": ";", "min": 10, "max": 1000, "default": [10, 20, 30]}""",
      "multiple_sep"
    )
    
    val _ = TestHelper.testMain(
      "build",
      "--engine", "docker",
      "--runner", "executable",
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

  test("Check docker image id", DockerTest) {
    val newConfigFilePath = configDeriver.derive(Nil, "build_docker_id")
    val testText = TestHelper.testMain(
      "build",
      "--engine", "docker",
      "--runner", "executable",
      "-o", tempFolStr,
      newConfigFilePath,
      "--setup", "alwaysbuild"
    )

    assert(executable.exists)
    assert(executable.canExecute)

    // read built files and get docker image ids
    def getDockerId(builtScriptPath: Path): Option[String] = {
      val dockerImageIdRegex = ".*\\sVIASH_DOCKER_IMAGE_ID='(.[^']*)'.*".r
      val content = IO.read(builtScriptPath.toUri())

      content.replaceAll("\n", "") match {
        case dockerImageIdRegex(id) => Some(id)
        case _ => None
      }
    }

    val dockerId = getDockerId(executable.toPath())

    assert(dockerId == Some("testbash:0.1"))
  }

  test("Check docker image id with custom docker_registry", DockerTest) {
    val newConfigFilePath = configDeriver.derive(""".links := {docker_registry: "foo.bar"}""", "build_docker_id_custom_registry")
    val testText = TestHelper.testMain(
      "build",
      "--engine", "docker",
      "--runner", "executable",
      "-o", tempFolStr,
      newConfigFilePath,
      "--setup", "alwaysbuild"
    )

    assert(executable.exists)
    assert(executable.canExecute)

    // read built files and get docker image ids
    def getDockerId(builtScriptPath: Path): Option[String] = {
      val dockerImageIdRegex = ".*\\sVIASH_DOCKER_IMAGE_ID='(.[^']*)'.*".r
      val content = IO.read(builtScriptPath.toUri())

      content.replaceAll("\n", "") match {
        case dockerImageIdRegex(id) => Some(id)
        case _ => None
      }
    }

    val dockerId = getDockerId(executable.toPath())

    assert(dockerId == Some("foo.bar/testbash:0.1"))
  }

  override def afterAll(): Unit = {
    IO.deleteRecursively(temporaryFolder)
    IO.deleteRecursively(temporaryConfigFolder)
  }
}