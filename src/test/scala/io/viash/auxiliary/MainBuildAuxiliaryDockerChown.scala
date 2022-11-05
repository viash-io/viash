package io.viash.auxiliary

import io.viash.{DockerTest, TestHelper}
import io.viash.helpers.{IO, Exec}
import io.viash.config.Config
import org.scalatest.{BeforeAndAfterAll, FunSuite}

import java.nio.file.{Files, Paths, StandardCopyOption}
import scala.io.Source

class MainBuildAuxiliaryDockerChown extends FunSuite with BeforeAndAfterAll {
  private val temporaryFolder = IO.makeTemp("viash_tester")
  private val tempFolStr = temporaryFolder.toString

  private val configDockerOptionsChownFile = getClass.getResource("/testbash/docker_options/config_chown.vsh.yaml").getPath
  private val configDockerOptionsChownTwoOutputFile = getClass.getResource("/testbash/docker_options/config_chown_two_output.vsh.yaml").getPath
  private val configDockerOptionsChownMultipleOutputFile = getClass.getResource("/testbash/docker_options/config_chown_multiple_output.vsh.yaml").getPath


  def dockerChownGetOwner(dockerId: String): String = {
    val localConfig = configDockerOptionsChownFile
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

  def dockerChownGetOwnerTwoOutputs(dockerId: String): (String,String) = {
    val localConfig = configDockerOptionsChownTwoOutputFile
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

  def dockerChownGetOwnerMultipleOutputs(dockerId: String): (String,String,String) = {
    val localConfig = configDockerOptionsChownMultipleOutputFile
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
    val owner = dockerChownGetOwner("chown_default")
    assert(owner.nonEmpty)
    assert(owner != "root")
  }

  test("Test default behaviour when chown is set to true", DockerTest) {
    val owner = dockerChownGetOwner("chown_true")
    assert(owner.nonEmpty)
    assert(owner != "root")
  }

  test("Test default behaviour when chown is set to false", DockerTest) {
    val owner = dockerChownGetOwner("chown_false")
    assert(owner == "root")
  }

  test("Test default behaviour when chown is not specified with two output files", DockerTest) {
    val owner = dockerChownGetOwnerTwoOutputs("two_chown_default")
    assert(owner._1.nonEmpty)
    assert(owner._2.nonEmpty)
    assert(owner._1 != "root")
    assert(owner._2 != "root")
  }

  test("Test default behaviour when chown is set to true with two output files", DockerTest) {
    val owner = dockerChownGetOwnerTwoOutputs("two_chown_true")
    assert(owner._1.nonEmpty)
    assert(owner._2.nonEmpty)
    assert(owner._1 != "root")
    assert(owner._2 != "root")
  }

  test("Test default behaviour when chown is set to false with two output files", DockerTest) {
    val owner = dockerChownGetOwnerTwoOutputs("two_chown_false")
    assert(owner._1 == "root")
    assert(owner._2 == "root")
  }

  test("Test default behaviour when chown is not specified with multiple output files", DockerTest) {
    val owner = dockerChownGetOwnerMultipleOutputs("multiple_chown_default")
    assert(owner._1.nonEmpty)
    assert(owner._2.nonEmpty)
    assert(owner._3.nonEmpty)
    assert(owner._1 != "root")
    assert(owner._2 != "root")
    assert(owner._3 != "root")
  }

  test("Test default behaviour when chown is set to true with multiple output files", DockerTest) {
    val owner = dockerChownGetOwnerMultipleOutputs("multiple_chown_true")
    assert(owner._1.nonEmpty)
    assert(owner._2.nonEmpty)
    assert(owner._3.nonEmpty)
    assert(owner._1 != "root")
    assert(owner._2 != "root")
    assert(owner._3 != "root")
  }

  test("Test default behaviour when chown is set to false with multiple output files", DockerTest) {
    val owner = dockerChownGetOwnerMultipleOutputs("multiple_chown_false")
    assert(owner._1 == "root")
    assert(owner._2 == "root")
    assert(owner._3 == "root")
  }

  override def afterAll() {
    IO.deleteRecursively(temporaryFolder)
  }
}