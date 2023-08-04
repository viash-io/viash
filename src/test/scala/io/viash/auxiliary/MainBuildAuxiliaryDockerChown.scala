package io.viash.auxiliary

import io.viash.{DockerTest, TestHelper}
import io.viash.helpers.{IO, Exec, Logger}
import io.viash.config.Config
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite

import java.nio.file.{Files, Paths, StandardCopyOption}
import scala.io.Source
import io.viash.ConfigDeriver

class MainBuildAuxiliaryDockerChown extends AnyFunSuite with BeforeAndAfterAll {
  Logger.UseColorOverride.value = Some(false)
  private val temporaryFolder = IO.makeTemp("viash_tester")
  private val tempFolStr = temporaryFolder.toString
  private val temporaryConfigFolder = IO.makeTemp(s"viash_${this.getClass.getName}_")

  private val configFile = getClass.getResource("/testbash/config.vsh.yaml").getPath
  private val configDeriver = ConfigDeriver(Paths.get(configFile), temporaryConfigFolder)

  def platformConfigMod(dockerId: String, chown: Option[Boolean]): String = {
    val extra = chown.map(b => s""", "chown": $b""" ).getOrElse("")
    s""".platforms := [{"type": "docker", "image": "bash:3.2", "id": "$dockerId"$extra}]"""
  }

  def dockerChownGetOwner(dockerId: String, chown: Option[Boolean]): String = {
    // val localConfig = configDeriver.derive(platformConfigMod(dockerId, chown), dockerId)
    val mods = List(
      """.functionality.resources[.type == "bash_script"].path := "docker_options/code.sh"""",
      platformConfigMod(dockerId, chown)
    )
    val localConfig = configDeriver.derive(mods, dockerId)
    val localFunctionality = Config.read(localConfig).functionality
    val localExecutable = Paths.get(tempFolStr, localFunctionality.name).toFile

    // prepare the environment
    TestHelper.testMain(
      "build",
      "-p", dockerId,
      "-o", tempFolStr,
      "--setup", "build",
      localConfig
    )

    assert(localExecutable.exists)
    assert(localExecutable.canExecute)

    // run the script
    val output = Paths.get(tempFolStr, s"output_" + dockerId + ".txt").toFile

    Exec.run(
      Seq(
        localExecutable.toString,
        localExecutable.toString,
        "--real_number", "10.5",
        "--whole_number=10",
        "-s", "a string with a few spaces",
        "--output", output.getPath
      )
    )

    assert(output.exists())

    val owner = Files.getOwner(output.toPath)
    owner.toString
  }

  def dockerChownGetOwnerTwoOutputs(dockerId: String, chown: Option[Boolean]): (String,String) = {
    val mods = List(
      """.functionality.argument_groups[.name == "Arguments"].arguments += {name: "--output2", type: "file", direction: "output"}""",
      """.functionality.resources[.type == "bash_script"].path := "docker_options/code_two_output.sh"""",
      platformConfigMod(dockerId, chown)
    )

    val localConfig = configDeriver.derive(mods, dockerId)
    val localFunctionality = Config.read(localConfig).functionality
    val localExecutable = Paths.get(tempFolStr, localFunctionality.name).toFile

    // prepare the environment
    TestHelper.testMain(
      "build",
      "-p", dockerId,
      "-o", tempFolStr,
      "--setup", "build",
      localConfig
    )

    assert(localExecutable.exists)
    assert(localExecutable.canExecute)

    // run the script
    val output = Paths.get(tempFolStr, "output_" + dockerId + ".txt").toFile
    val output2 = Paths.get(tempFolStr, "output_" + dockerId +"_2.txt").toFile

    val _ = Exec.run(
      Seq(
        localExecutable.toString,
        localExecutable.toString,
        "--real_number", "10.5",
        "--whole_number=10",
        "-s", "a string with a few spaces",
        "--output", output.getPath,
        "--output2", output2.getPath
      )
    )

    assert(output.exists())
    assert(output2.exists())

    val owner = Files.getOwner(output.toPath)
    val owner2 = Files.getOwner(output2.toPath)
    (owner.toString, owner2.toString)
  }

  def dockerChownGetOwnerMultipleOutputs(dockerId: String, chown: Option[Boolean]): (String,String,String) = {
    val mods = List(
      """del(.functionality.argument_groups[.name == "Arguments"].arguments[.name == "--multiple"])""",
      """del(.functionality.argument_groups[.name == "Arguments"].arguments[.name == "multiple_pos"])""",
      """.functionality.argument_groups[.name == "Arguments"].arguments[.name == "--output"].multiple := true""",
      """.functionality.argument_groups[.name == "Arguments"].arguments += {name: "output_pos", type: "file", direction: "output", multiple: true}""",
      """.functionality.resources[.type == "bash_script"].path := "docker_options/code_multiple_output.sh"""",
      platformConfigMod(dockerId, chown)
    )
    val localConfig = configDeriver.derive(mods, dockerId)
    val localFunctionality = Config.read(localConfig).functionality
    val localExecutable = Paths.get(tempFolStr, localFunctionality.name).toFile

    // prepare the environment
    TestHelper.testMain(
      "build",
      "-p", dockerId,
      "-o", tempFolStr,
      "--setup", "build",
      localConfig
    )

    assert(localExecutable.exists)
    assert(localExecutable.canExecute)

    // run the script
    val output = Paths.get(tempFolStr, "output_" + dockerId + ".txt").toFile
    val output2 = Paths.get(tempFolStr, "output_" + dockerId +"_2.txt").toFile
    val output3 = Paths.get(tempFolStr, "output_" + dockerId +"_3.txt").toFile

    Exec.run(
      Seq(
        localExecutable.toString,
        localExecutable.toString,
        "--real_number", "10.5",
        "--whole_number=10",
        "-s", "a string with a few spaces",
        "--output", output.getPath, output2.getPath, output3.getPath
      )
    )

    assert(output.exists())
    assert(output2.exists())
    assert(output3.exists())

    val owner = Files.getOwner(output.toPath)
    val owner2 = Files.getOwner(output2.toPath)
    val owner3 = Files.getOwner(output3.toPath)
    (owner.toString, owner2.toString, owner3.toString)
  }

  test("Test default behaviour when chown is not specified", DockerTest) {
    val owner = dockerChownGetOwner("chown_default", None)
    assert(owner.nonEmpty)
    assert(owner != "root")
  }

  test("Test default behaviour when chown is set to true", DockerTest) {
    val owner = dockerChownGetOwner("chown_true", Some(true))
    assert(owner.nonEmpty)
    assert(owner != "root")
  }

  test("Test default behaviour when chown is set to false", DockerTest) {
    val owner = dockerChownGetOwner("chown_false", Some(false))
    assert(owner == "root")
  }

  test("Test default behaviour when chown is not specified with two output files", DockerTest) {
    val owner = dockerChownGetOwnerTwoOutputs("two_chown_default", None)
    assert(owner._1.nonEmpty)
    assert(owner._2.nonEmpty)
    assert(owner._1 != "root")
    assert(owner._2 != "root")
  }

  test("Test default behaviour when chown is set to true with two output files", DockerTest) {
    val owner = dockerChownGetOwnerTwoOutputs("two_chown_true", Some(true))
    assert(owner._1.nonEmpty)
    assert(owner._2.nonEmpty)
    assert(owner._1 != "root")
    assert(owner._2 != "root")
  }

  test("Test default behaviour when chown is set to false with two output files", DockerTest) {
    val owner = dockerChownGetOwnerTwoOutputs("two_chown_false", Some(false))
    assert(owner._1 == "root")
    assert(owner._2 == "root")
  }

  test("Test default behaviour when chown is not specified with multiple output files", DockerTest) {
    val owner = dockerChownGetOwnerMultipleOutputs("multiple_chown_default", None)
    assert(owner._1.nonEmpty)
    assert(owner._2.nonEmpty)
    assert(owner._3.nonEmpty)
    assert(owner._1 != "root")
    assert(owner._2 != "root")
    assert(owner._3 != "root")
  }

  test("Test default behaviour when chown is set to true with multiple output files", DockerTest) {
    val owner = dockerChownGetOwnerMultipleOutputs("multiple_chown_true", Some(true))
    assert(owner._1.nonEmpty)
    assert(owner._2.nonEmpty)
    assert(owner._3.nonEmpty)
    assert(owner._1 != "root")
    assert(owner._2 != "root")
    assert(owner._3 != "root")
  }

  test("Test default behaviour when chown is set to false with multiple output files", DockerTest) {
    val owner = dockerChownGetOwnerMultipleOutputs("multiple_chown_false", Some(false))
    assert(owner._1 == "root")
    assert(owner._2 == "root")
    assert(owner._3 == "root")
  }

  override def afterAll(): Unit = {
    IO.deleteRecursively(temporaryFolder)
    IO.deleteRecursively(temporaryConfigFolder)
  }
}
