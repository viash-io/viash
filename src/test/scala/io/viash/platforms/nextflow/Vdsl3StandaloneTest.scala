package io.viash.platforms.nextflow

import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite

import java.io.IOException
import java.io.UncheckedIOException
import java.io.File
import java.nio.file.{Files, Path, Paths}
import scala.io.Source
import scala.util.Using

import io.viash.helpers.{IO, Logger}
import io.viash.{DockerTest, NextflowTest, TestHelper}
import java.nio.charset.StandardCharsets

import NextflowTestHelper._

/**
  * Test suite for VDSL3 components as standalone Nextflow workflows.
  * Since these tests run workflows from the CLI, we need to check the
  * generated output inside the test suite.
  */
class Vdsl3StandaloneTest extends AnyFunSuite with BeforeAndAfterAll {
  Logger.UseColorOverride.value = Some(false)
  // temporary folder to work in
  private val temporaryFolder = IO.makeTemp("viash_tester_nextflowvdsl3")
  private val tempFolFile = temporaryFolder.toFile
  private val tempFolStr = temporaryFolder.toString

  // path to namespace components
  private val rootPath = getClass.getResource("/testnextflowvdsl3/").getPath
  private val srcPath = Paths.get(tempFolStr, "src").toFile.toString
  private val targetPath = Paths.get(tempFolStr, "target").toFile.toString
  private val resourcesPath = Paths.get(tempFolStr, "resources").toFile.toString
  private val workflowsPath = Paths.get(tempFolStr, "workflows").toFile.toString

  // copy resources to temporary folder so we can build in a clean environment
  for (resource <- List("src", "workflows", "resources"))
    IO.copyFolder(Paths.get(rootPath, resource).toString, Paths.get(tempFolStr, resource).toString)

  test("Build pipeline components", DockerTest, NextflowTest) {
    // build the nextflow containers
    val (_, _, _) = TestHelper.testMainWithStdErr(
      "ns", "build",
      "-s", srcPath,
      "-t", targetPath,
      "--setup", "cb"
    )
  }

  test("Simple run", NextflowTest) {
    val (exitCode, stdOut, stdErr) = NextflowTestHelper.run(
      mainScript = "target/nextflow/step2/main.nf",
      args = List(
        "--input1", "resources/lines3.txt",
        "--input2", "resources/lines5.txt",
        "--publish_dir", "moduleOutput1"
      ),
      cwd = tempFolFile
    )

    assert(exitCode == 0, s"\nexit code was $exitCode\nStd output:\n$stdOut\nStd error:\n$stdErr")
    
    val src = Source.fromFile(tempFolStr + "/moduleOutput1/run.step2.output1.txt")
    try {
      val moduleOut = src.getLines().mkString(",")
      assert(moduleOut.equals("one,two,three"))
    } finally {
      src.close()
    }
  }

  test("With id containing spaces and slashes", NextflowTest) {
    val (exitCode, stdOut, stdErr) = NextflowTestHelper.run(
      mainScript = "target/nextflow/step2/main.nf",
      args = List(
        "--id", "one two three/four five six/seven eight nine",
        "--input1", "resources/lines3.txt",
        "--input2", "resources/lines5.txt",
        "--publish_dir", "moduleOutput2"
      ),
      cwd = tempFolFile
    )

    assert(exitCode == 0, s"\nexit code was $exitCode\nStd output:\n$stdOut\nStd error:\n$stdErr")
    
    val src = Source.fromFile(tempFolStr + "/moduleOutput2/one two three/four five six/seven eight nine.step2.output1.txt")
    try {
      val moduleOut = src.getLines().mkString(",")
      assert(moduleOut.equals("one,two,three"))
    } finally {
      src.close()
    }
  }

  test("With yamlblob param_list", NextflowTest) {
    val paramListStr = "[{input1: resources/lines3.txt, input2: resources/lines5.txt}]"
    val (exitCode, stdOut, stdErr) = NextflowTestHelper.run(
      mainScript = "target/nextflow/step2/main.nf",
      args = List(
        "--param_list", paramListStr,
        "--publish_dir", "moduleOutput2"
      ),
      cwd = tempFolFile
    )

    assert(exitCode == 0, s"\nexit code was $exitCode\nStd output:\n$stdOut\nStd error:\n$stdErr")
    
    val src = Source.fromFile(tempFolStr + "/moduleOutput2/run.step2.output1.txt")
    try {
      val moduleOut = src.getLines().mkString(",")
      assert(moduleOut.equals("one,two,three"))
    } finally {
      src.close()
    }
  }

  test("With yaml param_list", NextflowTest) {
    val paramListPath = temporaryFolder.resolve("resources/param_list.yaml")
    val paramListStr = "- input1: lines3.txt\n  input2: lines5.txt"

    // create param list yaml file
    Files.write(paramListPath, paramListStr.getBytes(StandardCharsets.UTF_8))
    
    val (exitCode, stdOut, stdErr) = NextflowTestHelper.run(
      mainScript = "target/nextflow/step2/main.nf",
      args = List(
        "--param_list", paramListPath.toString(),
        "--publish_dir", "moduleOutput2"
      ),
      cwd = tempFolFile
    )

    assert(exitCode == 0, s"\nexit code was $exitCode\nStd output:\n$stdOut\nStd error:\n$stdErr")
    
    val src = Source.fromFile(tempFolStr + "/moduleOutput2/run.step2.output1.txt")
    try {
      val moduleOut = src.getLines().mkString(",")
      assert(moduleOut.equals("one,two,three"))
    } finally {
      src.close()
    }
  }

  test("With optional inputs", NextflowTest) {

    Files.copy(Paths.get(resourcesPath, "lines5.txt"), Paths.get(resourcesPath, "lines5-bis.txt"))

    val (exitCode, stdOut, stdErr) = NextflowTestHelper.run(
      mainScript = "target/nextflow/step2/main.nf",
      args = List(
        "--input1", "resources/lines3.txt",
        "--input2", "resources/lines5.txt",
        "--optional", "resources/lines5-bis.txt",
        "--publish_dir", "moduleOutput3"
      ),
      cwd = tempFolFile
    )

    assert(exitCode == 0, s"\nexit code was $exitCode\nStd output:\n$stdOut\nStd error:\n$stdErr")
    
    val src = Source.fromFile(tempFolStr + "/moduleOutput3/run.step2.output1.txt")
    try {
      val moduleOut = src.getLines().mkString(",")
      assert(moduleOut.equals("one,two,three,1,2,3,4,5"))
    } finally {
      src.close()
    }
  }


  test("Run multiple output test", NextflowTest) {
    val (exitCode, stdOut, stdErr) = NextflowTestHelper.run(
      mainScript = "target/nextflow/multiple_output/main.nf",
      args = List(
        "--id", "foo",
        "--input", "resources/lines*.txt",
        "--publish_dir", "multipleOutput"
      ),
      cwd = tempFolFile
    )

    assert(exitCode == 0, s"\nexit code was $exitCode\nStd output:\n$stdOut\nStd error:\n$stdErr")

    val expectedFiles =
      Map(
        "state" -> "state.yaml",
        "output_0" -> "output_0.txt", 
        "output_1" -> "output_1.txt"
      ).map{ case (id, suffix) =>
        val path = temporaryFolder.resolve("multipleOutput/foo.multiple_output." + suffix)
        (id, path)
      }

    // check if files exist
    for ((id, path) <- expectedFiles) {
      assert(Files.exists(path), s"File '$id' at path '$path' does not exist")
    }

    // check state content
    Using(Source.fromFile(expectedFiles("state").toFile())) { reader =>
      val stateTxt = reader.getLines().mkString("\n")
      val expectedState = """\
        |id: foo
        |output:
        |- !file 'foo.multiple_output.output_0.txt'
        |- !file 'foo.multiple_output.output_1.txt'
        |""".stripMargin
      assert(stateTxt == expectedState)
    }
  }


  test("Whether integers can be converted to doubles", NextflowTest) {
    val (exitCode, stdOut, stdErr) = NextflowTestHelper.run(
      mainScript = "target/nextflow/integer_as_double/main.nf",
      args = List(
        "--id", "foo",
        "--input", "resources/lines3.txt",
        "--double", "10", // this should be an integer
        "--publish_dir", "integerAsDouble"
      ),
      cwd = tempFolFile
    )

    assert(exitCode == 0, s"\nexit code was $exitCode\nStd output:\n$stdOut\nStd error:\n$stdErr")

    val expectedFiles =
      Map(
        "state" -> "state.yaml",
        "output" -> "output.txt", 
      ).map{ case (id, suffix) =>
        val path = temporaryFolder.resolve("integerAsDouble/foo.integer_as_double." + suffix)
        (id, path)
      }

    // check if files exist
    
    val src = Source.fromFile(tempFolStr + "/integerAsDouble/foo.integer_as_double.output.txt")
    try {
      val moduleOut = src.getLines().mkString(",")
      assert(moduleOut.equals("one,two,three,Double: 10.0"), s"Expected output 'one,two,three,10.0' but got '$moduleOut'")
    } finally {
      src.close()
    }
  }

  override def afterAll(): Unit = {
    IO.deleteRecursively(temporaryFolder)
  }
}
