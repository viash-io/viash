package io.viash.auxiliary

import io.viash.config.Config
import io.viash.helpers.{IO, Exec}
import io.viash.{DockerTest, TestHelper}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.FixtureAnyFunSuite

import java.nio.file.{Files, Paths}
import scala.io.Source
import io.viash.ConfigDeriver

class MainBuildAuxiliaryDockerRequirements extends FixtureAnyFunSuite with BeforeAndAfterAll {
  private val temporaryFolder = IO.makeTemp("viash_tester")
  private val tempFolStr = temporaryFolder.toString
  private val temporaryConfigFolder = IO.makeTemp("viash_tester_configs")

  private val configRequirementsFile = getClass.getResource(s"/testbash/auxiliary_requirements/config_requirements.vsh.yaml").getPath
  private val functionalityRequirements = Config.read(configRequirementsFile).functionality
  private val executableRequirementsFile = Paths.get(tempFolStr, functionalityRequirements.name).toFile

  private val configDeriver = ConfigDeriver(Paths.get(configRequirementsFile), temporaryConfigFolder)

  private val dockerTag = "viash_requirements_testbench"

  case class FixtureParam()

  // Fixture will remove the docker image before starting and remove it again after finishing
  def withFixture(test: OneArgTest) = {
    // remove docker if it exists
    removeDockerImage(dockerTag)
    assert(!checkDockerImageExists(dockerTag))

    val theFixture = FixtureParam()

    val outcome = withFixture(test.toNoArgTest(theFixture)) // "loan" the fixture to the test

    // Tests finished, remove docker image
    removeDockerImage(dockerTag)

    outcome
  }

  test("setup; check base image for apk still does not contain the fortune package", DockerTest) { f =>
    val newConfigFilePath = configDeriver.derive(
      s""".platforms := [{ "type": "docker", "image": "bash:3.2", "target_image": "$dockerTag" }]""",
      "apk_base"
    )

    TestHelper.testMain(
      "build",
      "-p", "docker",
      "-o", tempFolStr,
      "--setup", "build",
      newConfigFilePath
    )

    assert(executableRequirementsFile.exists)
    assert(executableRequirementsFile.canExecute)

    val output = Exec.runCatch(
      Seq(
        executableRequirementsFile.toString,
        "--which", "fortune"
      )
    )

    assert(output.output == "")
  }

  test("setup; check docker requirements using apk to add the fortune package", DockerTest) { f =>
    val newConfigFilePath = configDeriver.derive(
      s""".platforms := [{ "type": "docker", "image": "bash:3.2", "target_image": "$dockerTag", "setup": [{ "type": "apk", "packages": ["fortune"] }] }]""",
      "apk_fortune"
    )

    // build viash wrapper with --setup
    TestHelper.testMain(
      "build",
      "-o", tempFolStr,
      "--setup", "build",
      newConfigFilePath
    )

    // verify docker exists
    assert(checkDockerImageExists(dockerTag))

    assert(executableRequirementsFile.exists)
    assert(executableRequirementsFile.canExecute)

    val output = Exec.runCatch(
      Seq(
        executableRequirementsFile.toString,
        "--which", "fortune"
      )
    )

    assert(output.output == "/usr/bin/fortune\n")
  }

  test("setup; check docker requirements using apk but with an empty list", DockerTest) { f =>
    val newConfigFilePath = configDeriver.derive(
      s""".platforms := [{ "type": "docker", "image": "bash:3.2", "target_image": "$dockerTag", "setup": [{ "type": "apk", "packages": [] }] }]""",
      "apk_empty"
    )

    // build viash wrapper with --setup
    TestHelper.testMain(
      "build",
      "-o", tempFolStr,
      "--setup", "build",
      newConfigFilePath
    )

    // verify docker exists
    assert(checkDockerImageExists(dockerTag))

    assert(executableRequirementsFile.exists)
    assert(executableRequirementsFile.canExecute)

    val output = Exec.runCatch(
      Seq(
        executableRequirementsFile.toString,
        "--which", "fortune"
      )
    )

    assert(output.output == "")
  }

  test("setup; check base image for apt still does not contain the cowsay package", DockerTest) { f =>
    val newConfigFilePath = configDeriver.derive(
      s""".platforms := [{ "type": "docker", "image": "debian:bullseye-slim", "target_image": "$dockerTag" }]""",
      "apt_base"
    )

    TestHelper.testMain(
      "build",
      "-o", tempFolStr,
      "--setup", "build",
      newConfigFilePath
    )

    assert(executableRequirementsFile.exists)
    assert(executableRequirementsFile.canExecute)

    val output = Exec.runCatch(
      Seq(
        executableRequirementsFile.toString,
        "--which", "cowsay"
      )
    )

    assert(output.output == "")
  }

  test("setup; check docker requirements using apt to add the cowsay package", DockerTest) { f =>
    val newConfigFilePath = configDeriver.derive(
      s""".platforms := [{ "type": "docker", "image": "debian:bullseye-slim", "target_image": "$dockerTag", "setup": [{ "type": "apt", "packages": ["cowsay"] }] }]""",
      "apt_cowsay"
    )

    // build viash wrapper with --setup
    val _ = TestHelper.testMain(
      "build",
      "-o", tempFolStr,
      "--setup", "build",
      newConfigFilePath
    )

    // verify docker exists
    assert(checkDockerImageExists(dockerTag))

    assert(executableRequirementsFile.exists)
    assert(executableRequirementsFile.canExecute)

    val output = Exec.runCatch(
      Seq(
        executableRequirementsFile.toString,
        "--file", "/usr/games/cowsay"
      )
    )

    assert(output.output == "/usr/games/cowsay exists.\n")
  }

  test("setup; check docker requirements using apt but with an empty list", DockerTest) { f =>
    val newConfigFilePath = configDeriver.derive(
      s""".platforms := [{ "type": "docker", "image": "debian:bullseye-slim", "target_image": "$dockerTag", "setup": [{ "type": "apt", "packages": [] }] }]""",
      "apt_empty"
    )

    TestHelper.testMain(
      "build",
      "-o", tempFolStr,
      "--setup", "build",
      newConfigFilePath
    )

    assert(executableRequirementsFile.exists)
    assert(executableRequirementsFile.canExecute)

    val output = Exec.runCatch(
      Seq(
        executableRequirementsFile.toString,
        "--which", "cowsay"
      )
    )

    assert(output.output == "")
  }

  test("setup; check base image for yum still does not contain the which package", DockerTest) { f =>
    val newConfigFilePath = configDeriver.derive(
      s""".platforms := [{ "type": "docker", "image": "centos:centos7", "target_image": "$dockerTag" }]""",
      "yum_base"
    )

    TestHelper.testMain(
      "build",
      "-o", tempFolStr,
      "--setup", "build",
      newConfigFilePath
    )

    assert(executableRequirementsFile.exists)
    assert(executableRequirementsFile.canExecute)

    val output = Exec.runCatch(
      Seq(
        executableRequirementsFile.toString,
        "--which", "which"
      )
    )

    assert(output.output.contains("line 25: which: command not found"))
  }

  test("setup; check docker requirements using yum to add the which package", DockerTest) { f =>
    val newConfigFilePath = configDeriver.derive(
      s""".platforms := [{ "type": "docker", "image": "centos:centos7", "target_image": "$dockerTag", "setup": [{ "type": "yum", "packages": ["which"] }] }]""",
      "yum_which"
    )

    // build viash wrapper with --setup
    val _ = TestHelper.testMain(
      "build",
      "-o", tempFolStr,
      "--setup", "build",
      newConfigFilePath
    )

    // verify docker exists
    assert(checkDockerImageExists(dockerTag))

    assert(executableRequirementsFile.exists)
    assert(executableRequirementsFile.canExecute)

    val output = Exec.runCatch(
      Seq(
        executableRequirementsFile.toString,
        "--which", "which"
      )
    )

    assert(output.output == "/usr/bin/which\n")
  }

  test("setup; check docker requirements using yum but with an empty list", DockerTest) { f =>
    val newConfigFilePath = configDeriver.derive(
      s""".platforms := [{ "type": "docker", "image": "centos:centos7", "target_image": "$dockerTag", "setup": [{ "type": "yum", "packages": [] }] }]""",
      "apt_empty"
    )

    TestHelper.testMain(
      "build",
      "-o", tempFolStr,
      "--setup", "build",
      newConfigFilePath
    )

    assert(executableRequirementsFile.exists)
    assert(executableRequirementsFile.canExecute)

    val output = Exec.runCatch(
      Seq(
        executableRequirementsFile.toString,
        "--which", "which"
      )
    )

    assert(output.output.contains("line 25: which: command not found"))
  }

  test("test_setup; check the fortune package isn't added for the build option", DockerTest) { f =>
    val newConfigFilePath = configDeriver.derive(
      s""".platforms := [{ "type": "docker", "image": "bash:3.2", "target_image": "$dockerTag", "test_setup": [{ "type": "apk", "packages": ["fortune"] }] }]""",
      "apk_test_fortune_build"
    )

    TestHelper.testMain(
      "build",
      "-o", tempFolStr,
      "--setup", "build",
      newConfigFilePath
    )

    assert(executableRequirementsFile.exists)
    assert(executableRequirementsFile.canExecute)

    val output = Exec.runCatch(
      Seq(
        executableRequirementsFile.toString,
        "--which", "fortune"
      )
    )

    assert(output.output == "")
  }

  test("test_setup; check the fortune package is added for the test option", DockerTest) { f =>
    val newConfigFilePath = configDeriver.derive(
      s""".platforms := [{ "type": "docker", "image": "bash:3.2", "target_image": "$dockerTag", "test_setup": [{ "type": "apk", "packages": ["fortune"] }] }]""",
      "apk_test_fortune_test"
    )

    val testText = TestHelper.testMain(
      "test",
      newConfigFilePath
    )

    assert(testText.contains("Running tests in temporary directory: "))
    assert(testText.contains("SUCCESS! All 1 out of 1 test scripts succeeded!"))
    assert(testText.contains("Cleaning up temporary directory"))
  }

  test("test_setup; check the fortune package is not added for the test option when not specified", DockerTest) { f =>
    val newConfigFilePath = configDeriver.derive(
      s""".platforms := [{ "type": "docker", "image": "bash:3.2", "target_image": "$dockerTag" }]""",
      "apk_base_test"
    )

    val testOutput = TestHelper.testMainException2[RuntimeException](
      "test",
      "-k", "false",
      newConfigFilePath
    )

    assert(testOutput.exceptionText == "Only 0 out of 1 test scripts succeeded!")

    assert(testOutput.output.contains("Running tests in temporary directory: "))
    assert(testOutput.output.contains("ERROR! Only 0 out of 1 test scripts succeeded!"))
    assert(testOutput.output.contains("Cleaning up temporary directory"))
  }

  def checkDockerImageExists(name: String): Boolean = {
    val out = Exec.runCatch(
      Seq("docker", "images", name)
    )

    // print(out)
    val regex = s"$name\\s*latest".r

    regex.findFirstIn(out.output).isDefined
  }

  def removeDockerImage(name: String): Unit = {
    Exec.runCatch(
      Seq("docker", "rmi", name, "-f")
    )
  }

  override def afterAll(): Unit = {
    IO.deleteRecursively(temporaryFolder)
    IO.deleteRecursively(temporaryConfigFolder)
  }
}