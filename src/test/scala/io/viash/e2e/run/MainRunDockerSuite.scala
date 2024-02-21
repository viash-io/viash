package io.viash.e2e.run

import io.viash._

import java.nio.file.{Files, Paths, StandardCopyOption}

import io.viash.helpers.{IO, Exec}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite

import scala.reflect.io.Directory
import sys.process._

class MainRunDockerSuite extends AnyFunSuite with BeforeAndAfterAll {
  private val temporaryFolder = IO.makeTemp(s"viash_${this.getClass.getName}_")
  private val temporaryFolder2 = temporaryFolder.resolve("folder with spaces")

  private val configText = 
    """name: testing
      |arguments:
      |  - type: file
      |    name: --input
      |    required: true
      |  - type: file
      |    direction: output
      |    name: --output
      |    required: true
      |resources:
      |  - type: bash_script
      |    text: |
      |      cp -r "$par_input" "$par_output"
      |engines:
      |  - type: docker
      |    image: python:3.10-slim
      |""".stripMargin
  
  test("Check run with standard input and output folders", DockerTest) {
    val configFile = temporaryFolder.resolve("config.vsh.yaml")
    Files.write(configFile, configText.getBytes())

    val inputFile = temporaryFolder.resolve("some_input")
    Files.write(inputFile, "foo".getBytes())

    val outputFile = temporaryFolder.resolve("some_output")

    val runText = TestHelper.testMain(
      "run",
      configFile.toString(),
      "--",
      "--input", inputFile.toString(),
      "--output", outputFile.toString()
    )

    // assert(runText == "", "expecting the output to be empty")

    val outputFileText = Files.readString(outputFile)

    assert(outputFileText == "foo")
  }

  test("Check run with a folder containing a space", DockerTest) {

    temporaryFolder2.toFile.mkdir()

    val configFile = temporaryFolder2.resolve("config.vsh.yaml")
    Files.write(configFile, configText.getBytes())

    val inputFile = temporaryFolder2.resolve("some_input")
    Files.write(inputFile, "bar".getBytes())

    val outputFile = temporaryFolder2.resolve("some_output")

    val runText = TestHelper.testMain(
      "run",
      configFile.toString(),
      "--",
      "--input", inputFile.toString(),
      "--output", outputFile.toString()
    )

    // assert(runText == "", "expecting the output to be empty")

    val outputFileText = Files.readString(outputFile)

    assert(outputFileText == "bar")
  }

  override def afterAll(): Unit = {
    IO.deleteRecursively(temporaryFolder)
  }
}
