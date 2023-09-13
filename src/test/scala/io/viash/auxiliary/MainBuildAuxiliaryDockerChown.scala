package io.viash.auxiliary

import io.viash.{DockerTest, TestHelper}
import io.viash.helpers.{IO, Exec, Logger}
import io.viash.config.Config
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite

import java.nio.file.{Files, Paths, StandardCopyOption}
import scala.io.Source
import io.viash.ConfigDeriver
import org.scalatest.ParallelTestExecution
import io.viash.exceptions.ConfigParserException

class MainBuildAuxiliaryDockerChown extends AnyFunSuite with BeforeAndAfterAll with ParallelTestExecution {
  Logger.UseColorOverride.value = Some(false)
  private val temporaryFolder = IO.makeTemp("viash_tester")
  private val tempFolStr = temporaryFolder.toString
  private val temporaryConfigFolder = IO.makeTemp(s"viash_${this.getClass.getName}_")

  private val configFile = getClass.getResource("/testbash/config.vsh.yaml").getPath
  private val configDeriver = ConfigDeriver(Paths.get(configFile), temporaryConfigFolder)

  val singleOutputmods = List(
    """.functionality.resources[.type == "bash_script"].path := "docker_options/code.sh"""",
  )

  val twoOutputsmods = List(
    """.functionality.argument_groups[.name == "Arguments"].arguments += {name: "--output2", type: "file", direction: "output"}""",
    """.functionality.resources[.type == "bash_script"].path := "docker_options/code_two_output.sh"""",
  )

  val multipleOutputsMods = List(
    """del(.functionality.argument_groups[.name == "Arguments"].arguments[.name == "--multiple"])""",
    """del(.functionality.argument_groups[.name == "Arguments"].arguments[.name == "multiple_pos"])""",
    """.functionality.argument_groups[.name == "Arguments"].arguments[.name == "--output"].multiple := true""",
    """.functionality.argument_groups[.name == "Arguments"].arguments += {name: "output_pos", type: "file", direction: "output", multiple: true}""",
    """.functionality.resources[.type == "bash_script"].path := "docker_options/code_multiple_output.sh"""",
  )

  def dockerChownGetOwner(mods: List[String], amount: Int, dockerId: String, chown: Option[Boolean]): List[String] = {
    assert(amount > 0)
    assert(amount < 4)

    val extra = chown.map(b => s""", "chown": $b""" ).getOrElse("")
    val platformMod = s""".platforms := [{"type": "docker", "image": "bash:3.2", "id": "$dockerId"$extra}]"""
    val modsWithPlatform = mods :+ platformMod
    val localConfig = configDeriver.derive(modsWithPlatform, dockerId)
    val localFunctionality = Config.read(localConfig).functionality
    val localExecutable = Paths.get(tempFolStr, localFunctionality.name).toFile

    // prepare the environment
    TestHelper.testMain(
      "build",
      "--engine", dockerId,
      "--executor", dockerId,
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

    val outputParams = amount match {
      case 1 => Seq("--output", output.getPath)
      case 2 => Seq("--output", output.getPath, "--output2", output2.getPath)
      case 3 => Seq("--output", output.getPath, output2.getPath, output3.getPath)
    }

    Exec.run(
      Seq(
        localExecutable.toString,
        localExecutable.toString,
        "--real_number", "10.5",
        "--whole_number=10",
        "-s", "a string with a few spaces",
      ) ++ outputParams
    )

    val outputList = List(output, output2, output3).take(amount)
    outputList.foreach(output => assert(output.exists()))
    outputList.map(file => Files.getOwner(file.toPath).toString)
  }

  test("Test when chown is not specified", DockerTest) {
    val owners = dockerChownGetOwner(singleOutputmods, 1, "chown_default", None)
    assert(owners.length == 1)
    owners.foreach(owner => {
      assert(owner.nonEmpty)
      assert(owner != "root")
    })
  }

  test("Test when chown is set to true", DockerTest) {
    val owners = dockerChownGetOwner(singleOutputmods, 1, "chown_true", Some(true))
    assert(owners.length == 1)
    owners.foreach(owner => {
      assert(owner.nonEmpty)
      assert(owner != "root")
    })
  }

  test("Test when chown is set to false", DockerTest) {
    // functionality not provided in executor, should throw exception
    assertThrows[ConfigParserException] {
      dockerChownGetOwner(singleOutputmods, 1, "chown_false", Some(false))
    }
  }

  test("Test behaviour with two output files", DockerTest) {
    val owners = dockerChownGetOwner(twoOutputsmods, 2, "two_chown_default", None)
    assert(owners.length == 2)
    owners.foreach(owner => {
      assert(owner.nonEmpty)
      assert(owner != "root")
    })
  }

  test("Test behaviour with multiple output files", DockerTest) {
    val owners = dockerChownGetOwner(multipleOutputsMods, 3, "multiple_chown_default", None)
    assert(owners.length == 3)
    owners.foreach(owner => {
      assert(owner.nonEmpty)
      assert(owner != "root")
    })
  }

  override def afterAll(): Unit = {
    IO.deleteRecursively(temporaryFolder)
    IO.deleteRecursively(temporaryConfigFolder)
  }
}
