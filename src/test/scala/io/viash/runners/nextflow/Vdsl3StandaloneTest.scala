package io.viash.runners.nextflow

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
    TestHelper.testMain(
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

  test("With output id and key keywords", NextflowTest) {
    val (exitCode, stdOut, stdErr) = NextflowTestHelper.run(
      mainScript = "target/nextflow/step2/main.nf",
      args = List(
        "--id", "foo",
        "--input1", "resources/lines3.txt",
        "--input2", "resources/lines5.txt",
        "--output1", "$id.${id}.$key.${key}.txt",
        // "--output2", "$foo$bar.txt", // can't do this (yet) because of Nextflow doesn't support '$' yet.
        "--publish_dir", "moduleOutput2"
      ),
      cwd = tempFolFile
    )

    assert(exitCode == 0, s"\nexit code was $exitCode\nStd output:\n$stdOut\nStd error:\n$stdErr")
    assert(Files.exists(Paths.get(tempFolStr + "/moduleOutput2/foo.foo.step2.step2.txt")))
    // assert(Files.exists(Paths.get(tempFolStr + "/moduleOutput2/$foo$bar.txt")))
    
    val src1 = Source.fromFile(tempFolStr + "/moduleOutput2/foo.foo.step2.step2.txt")
    // val src2 = Source.fromFile(tempFolStr + "/moduleOutput2/$foo$bar.txt")
    try {
      val moduleOut1 = src1.getLines().mkString(",")
      // val moduleOut2 = src2.getLines().mkString(",")
      assert(moduleOut1.equals("one,two,three"))
      // assert(moduleOut2.equals("1,2,3,4,5"))
    } finally {
      src1.close()
      // src2.close()
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
    // create params.yaml file
    val paramsPath = temporaryFolder.resolve("params.yaml")
    val paramsStr = """id: foo
      |input: resources/lines3.txt
      |double: 10
      |publish_dir: integerAsDouble
      |""".stripMargin
    Files.write(paramsPath, paramsStr.getBytes(StandardCharsets.UTF_8))

    val (exitCode, stdOut, stdErr) = NextflowTestHelper.run(
      mainScript = "target/nextflow/integer_as_double/main.nf",
      args = Nil,
      paramsFile = Some(paramsPath.toString()),
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

  test("Output directory slashes", NextflowTest) {
    val (exitCode, stdOut, stdErr) = NextflowTestHelper.run(
      mainScript = "target/nextflow/output_directory/main.nf",
      args = List(
        "--output", "$id.$key.foo/",
        "--publish_dir", "moduleOutputDir1"
      ),
      cwd = tempFolFile
    )

    assert(exitCode == 0, s"\nexit code was $exitCode\nStd output:\n$stdOut\nStd error:\n$stdErr")
    
    val src = Source.fromFile(tempFolStr + "/moduleOutputDir1/run.output_directory.foo/test1.txt")

    try {
      val moduleOut = src.getLines().mkString(",")
      assert(moduleOut.equals("test123"))
    } finally {
      src.close()
    }
  }

  test("Optional output with must_exist: false does not get a fallback default", NextflowTest) {
    val (exitCode, stdOut, stdErr) = NextflowTestHelper.run(
      mainScript = "target/nextflow/optional_output/main.nf",
      args = List(
        "--publish_dir", "moduleOutputOptional1"
      ),
      cwd = tempFolFile
    )

    assert(exitCode == 0, s"\nexit code was $exitCode\nStd output:\n$stdOut\nStd error:\n$stdErr")

    val outputPath = temporaryFolder.resolve("moduleOutputOptional1/run.optional_output.output.txt")
    assert(Files.exists(outputPath), s"Expected output file $outputPath to exist")
    val moduleOut = Using.resource(Source.fromFile(outputPath.toFile))(_.getLines().mkString(","))
    assert(moduleOut == "optional_output: unset", s"Expected the script not to receive a value for --optional_output, got '$moduleOut'")

    val published = temporaryFolder.resolve("moduleOutputOptional1").toFile.listFiles().map(_.getName).toList
    assert(!published.exists(_.contains("optional_output.optional")), s"Did not expect an optional output to be published, got: $published")
  }

  test("Optional output with must_exist: false is created when passed explicitly", NextflowTest) {
    val (exitCode, stdOut, stdErr) = NextflowTestHelper.run(
      mainScript = "target/nextflow/optional_output/main.nf",
      args = List(
        "--optional_output", "$id.$key.optional_output.txt",
        "--publish_dir", "moduleOutputOptional2"
      ),
      cwd = tempFolFile
    )

    assert(exitCode == 0, s"\nexit code was $exitCode\nStd output:\n$stdOut\nStd error:\n$stdErr")

    val outputPath = temporaryFolder.resolve("moduleOutputOptional2/run.optional_output.output.txt")
    val moduleOut = Using.resource(Source.fromFile(outputPath.toFile))(_.getLines().mkString(","))
    assert(moduleOut == "optional_output: set", s"Expected the script to receive a value for --optional_output, got '$moduleOut'")

    val optionalPath = temporaryFolder.resolve("moduleOutputOptional2/run.optional_output.optional_output.txt")
    assert(Files.exists(optionalPath), s"Expected optional output file $optionalPath to exist")
    val optionalOut = Using.resource(Source.fromFile(optionalPath.toFile))(_.getLines().mkString(","))
    assert(optionalOut == "optional content")
  }

  test("Optional multiple output with must_exist: false does not get a fallback default", NextflowTest) {
    val (exitCode, stdOut, stdErr) = NextflowTestHelper.run(
      mainScript = "target/nextflow/optional_output_multiple/main.nf",
      args = List(
        "--publish_dir", "moduleOutputOptionalMultiple1"
      ),
      cwd = tempFolFile
    )

    assert(exitCode == 0, s"\nexit code was $exitCode\nStd output:\n$stdOut\nStd error:\n$stdErr")

    val outputPath = temporaryFolder.resolve("moduleOutputOptionalMultiple1/run.optional_output_multiple.output.txt")
    assert(Files.exists(outputPath), s"Expected output file $outputPath to exist")
    val moduleOut = Using.resource(Source.fromFile(outputPath.toFile))(_.getLines().mkString(","))
    assert(moduleOut == "optional_outputs: unset", s"Expected the script not to receive a value for --optional_outputs, got '$moduleOut'")

    val published = temporaryFolder.resolve("moduleOutputOptionalMultiple1").toFile.listFiles().map(_.getName).toList
    assert(!published.exists(_.contains("optional_outputs")), s"Did not expect optional outputs to be published, got: $published")
  }

  test("Optional multiple output with must_exist: false is created when passed explicitly", NextflowTest) {
    val (exitCode, stdOut, stdErr) = NextflowTestHelper.run(
      mainScript = "target/nextflow/optional_output_multiple/main.nf",
      args = List(
        "--optional_outputs", "$id.$key.optional_outputs_*.txt",
        "--publish_dir", "moduleOutputOptionalMultiple2"
      ),
      cwd = tempFolFile
    )

    assert(exitCode == 0, s"\nexit code was $exitCode\nStd output:\n$stdOut\nStd error:\n$stdErr")

    val outputPath = temporaryFolder.resolve("moduleOutputOptionalMultiple2/run.optional_output_multiple.output.txt")
    val moduleOut = Using.resource(Source.fromFile(outputPath.toFile))(_.getLines().mkString(","))
    assert(moduleOut == "optional_outputs: set", s"Expected the script to receive a value for --optional_outputs, got '$moduleOut'")

    for (i <- 0 to 1) {
      val optionalPath = temporaryFolder.resolve(s"moduleOutputOptionalMultiple2/run.optional_output_multiple.optional_outputs_$i.txt")
      assert(Files.exists(optionalPath), s"Expected optional output file $optionalPath to exist")
      val optionalOut = Using.resource(Source.fromFile(optionalPath.toFile))(_.getLines().mkString(","))
      assert(optionalOut == s"optional content $i")
    }
  }

  override def afterAll(): Unit = {
    IO.deleteRecursively(temporaryFolder)
  }
}
