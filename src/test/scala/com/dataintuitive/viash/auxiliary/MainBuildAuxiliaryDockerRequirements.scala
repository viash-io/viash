package com.dataintuitive.viash.auxiliary

import com.dataintuitive.viash.config.Config
import com.dataintuitive.viash.helpers._
import com.dataintuitive.viash.{DockerTest, TestHelper}
import org.scalatest.{BeforeAndAfterAll, FunSuite}

import java.nio.file.{Files, Paths}
import scala.io.Source

class MainBuildAuxiliaryDockerRequirements extends FunSuite with BeforeAndAfterAll {
  private val temporaryFolder = IO.makeTemp("viash_tester")
  private val tempFolStr = temporaryFolder.toString

  private val configRequirementsFile = getClass.getResource(s"/testbash/auxiliary_requirements/config_requirements.vsh.yaml").getPath
  private val functionalityRequirements = Config.read(configRequirementsFile, applyPlatform = false).functionality
  private val executableRequirementsFile = Paths.get(tempFolStr, functionalityRequirements.name).toFile

  test("check base image for apk still does not contain the fortune package", DockerTest) {
    TestHelper.testMain(
      "build",
      "-p", "viash_requirement_apk_base",
      "-o", tempFolStr,
      "--setup", "build",
      configRequirementsFile
    )

    assert(executableRequirementsFile.exists)
    assert(executableRequirementsFile.canExecute)

    val output = Exec.run2(
      Seq(
        executableRequirementsFile.toString,
        "--which", "fortune"
      )
    )

    assert(output.output == "")
  }

  test("check docker requirements using apk to add the fortune package", DockerTest) {
    // remove docker if it exists
    removeDockerImage("viash_requirement_apk")
    assert(!checkDockerImageExists("viash_requirement_apk"))

    // build viash wrapper with --setup
    TestHelper.testMain(
      "build",
      "-p", "viash_requirement_apk",
      "-o", tempFolStr,
      "--setup", "build",
      configRequirementsFile
    )

    // verify docker exists
    assert(checkDockerImageExists("viash_requirement_apk"))

    assert(executableRequirementsFile.exists)
    assert(executableRequirementsFile.canExecute)

    val output = Exec.run2(
      Seq(
        executableRequirementsFile.toString,
        "--which", "fortune"
      )
    )

    assert(output.output == "/usr/bin/fortune\n")

    // Tests finished, remove docker image
    removeDockerImage("viash_requirement_apk")
  }

  test("check base image for apt still does not contain the cowsay package", DockerTest) {
    TestHelper.testMain(
      "build",
      "-p", "viash_requirement_apt_base",
      "-o", tempFolStr,
      "--setup", "build",
      configRequirementsFile
    )

    assert(executableRequirementsFile.exists)
    assert(executableRequirementsFile.canExecute)

    val output = Exec.run2(
      Seq(
        executableRequirementsFile.toString,
        "--which", "cowsay"
      )
    )

    assert(output.output == "")
  }

  test("check docker requirements using apt to add the cowsay package", DockerTest) {
    // remove docker if it exists
    removeDockerImage("viash_requirement_apt")
    assert(!checkDockerImageExists("viash_requirement_apt"))

    // build viash wrapper with --setup
    val _ = TestHelper.testMain(
      "build",
      "-p", "viash_requirement_apt",
      "-o", tempFolStr,
      "--setup", "build",
      configRequirementsFile
    )

    // verify docker exists
    assert(checkDockerImageExists("viash_requirement_apt"))

    assert(executableRequirementsFile.exists)
    assert(executableRequirementsFile.canExecute)

    val output = Exec.run2(
      Seq(
        executableRequirementsFile.toString,
        "--file", "/usr/games/cowsay"
      )
    )

    assert(output.output == "/usr/games/cowsay exists.\n")

    // Tests finished, remove docker image
    removeDockerImage("viash_requirement_apt")
  }

  def checkDockerImageExists(name: String): Boolean = {
    val out = Exec.run2(
      Seq("docker", "images", name)
    )

    // print(out)
    val regex = s"$name\\s*latest".r

    regex.findFirstIn(out.output).isDefined
  }

  def removeDockerImage(name: String): Unit = {
    Exec.run2(
      Seq("docker", "rmi", name, "-f")
    )
  }

  override def afterAll() {
    IO.deleteRecursively(temporaryFolder)
  }
}