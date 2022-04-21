package com.dataintuitive.viash.platforms

import com.dataintuitive.viash.helpers.IO
import com.dataintuitive.viash.{DockerTest, NextFlowTest, TestHelper}
import org.scalatest.{BeforeAndAfterAll, FunSuite}

import java.io.File
import java.nio.file.{Files, Path, Paths}
import scala.io.Source

import java.io.IOException
import java.io.UncheckedIOException

class NextFlowPocPlatformTest extends FunSuite with BeforeAndAfterAll {
  // temporary folder to work in
  private val temporaryFolder = IO.makeTemp("viash_tester_nextflowpoc")
  private val tempFolStr = temporaryFolder.toString

  // path to namespace components
  private val rootPath = getClass.getResource("/testnextflowpoc/").getPath
  private val srcPath = Paths.get(tempFolStr, "src").toFile.toString
  private val targetPath = Paths.get(tempFolStr, "target").toFile.toString

  // convert testbash
  test("build pipeline components", DockerTest, NextFlowTest) {

    // copy resources to temporary folder so we can build in a clean environment
    for (resource <- List("src", "workflows", "resources"))
      TestHelper.copyFolder(Paths.get(rootPath, resource).toString, Paths.get(tempFolStr, resource).toString)

    // build the nextflow containers
    val (_, _) = TestHelper.testMainWithStdErr(
      "ns", "build",
      "-s", srcPath,
      "-t", targetPath,
      "--setup", "cb"
    )
  }

  test("Run pipeline", DockerTest, NextFlowTest) {

    import sys.process._
    val output = Process(
      Seq("nextflow", "run", ".", "-main-script", "workflows/pipeline1/main.nf", "--input", "resources/*", "--publishDir", "output"),
      new File(tempFolStr),
      "NXF_VER" -> "21.04.1"
    ).!!
    val lines = output.split("\n").find(_.contains("DEBUG6"))

    assert(lines.isDefined)
    val DebugRegex = "DEBUG6: \\[foo, (.*)\\]".r
    val DebugRegex(path) = lines.get

    val src = Source.fromFile(path)
    try {
      val step3Out = src.getLines.mkString
      assert(step3Out.matches("^11 .*$"))
    } finally {
      src.close()
    }
    // TODO: check other debug flags as well.
    // TODO: change step3 into something more interesting.
  }

  test("Run pipeline with components using map functionality", DockerTest, NextFlowTest) {

    import sys.process._
    val output = Process(
      Seq("nextflow", "run", ".", "-main-script", "workflows/pipeline2/main.nf", "--input", "resources/*", "--publishDir", "output"),
      new File(tempFolStr),
      "NXF_VER" -> "21.04.1"
    ).!!
    val lines = output.split("\n").find(_.contains("DEBUG4"))

    assert(lines.isDefined)
    val DebugRegex = "DEBUG4: \\[foo, (.*)\\]".r
    val DebugRegex(path) = lines.get

    val src = Source.fromFile(path)
    try {
      val step3Out = src.getLines.mkString
      assert(step3Out.matches("^11 .*$"))
    } finally {
      src.close()
    }
    // TODO: check other debug flags as well.
    // TODO: change step3 into something more interesting.
  }

  test("Run pipeline with components using mapData functionality", DockerTest, NextFlowTest) {

    import sys.process._
    val output = Process(
      Seq("nextflow", "run", ".", "-main-script", "workflows/pipeline3/main.nf", "--input", "resources/*", "--publishDir", "output"),
      new File(tempFolStr),
      "NXF_VER" -> "21.04.1"
    ).!!
    val lines = output.split("\n").find(_.contains("DEBUG4"))

    assert(lines.isDefined)
    val DebugRegex = "DEBUG4: \\[foo, (.*)\\]".r
    val DebugRegex(path) = lines.get

    val src = Source.fromFile(path)
    try {
      val step3Out = src.getLines.mkString
      assert(step3Out.matches("^11 .*$"))
    } finally {
      src.close()
    }
    // TODO: check other debug flags as well.
    // TODO: change step3 into something more interesting.
  }

  test("Run pipeline with debug = false", DockerTest, NextFlowTest) {

    import sys.process._
    val output = Process(
      Seq("nextflow", "run", ".",
        "-main-script", "workflows/pipeline4/main.nf",
        "--input", "resources/*",
        "--publishDir", "output",
        "--displayDebug", "false",
        ),
      new File(tempFolStr),
      "NXF_VER" -> "21.04.1"
    ).!!
    val lines = output.split("\n").find(_.contains("DEBUG4"))

    assert(lines.isDefined)
    val DebugRegex = "DEBUG4: \\[foo, (.*)\\]".r
    val DebugRegex(path) = lines.get

    val src = Source.fromFile(path)
    try {
      val step3Out = src.getLines.mkString
      assert(step3Out.matches("^11 .*$"))
    } finally {
      src.close()
    }

    val lines2 = output.split("\n").find(_.contains("process 'step3' output tuple"))

    assert(!lines2.isDefined)

  }

  test("Run pipeline with debug = true", DockerTest, NextFlowTest) {

    import sys.process._
    val output = Process(
      Seq("nextflow", "run", ".",
        "-main-script", "workflows/pipeline4/main.nf",
        "--input", "resources/*",
        "--publishDir", "output",
        "--displayDebug", "true",
        ),
      new File(tempFolStr),
      "NXF_VER" -> "21.04.1"
    ).!!
    val lines = output.split("\n").find(_.contains("DEBUG4"))

    assert(lines.isDefined)
    val DebugRegex = "DEBUG4: \\[foo, (.*)\\]".r
    val DebugRegex(path) = lines.get

    val src = Source.fromFile(path)
    try {
      val step3Out = src.getLines.mkString
      assert(step3Out.matches("^11 .*$"))
    } finally {
      src.close()
    }

    val lines2 = output.split("\n").find(_.contains("process 'step3' output tuple"))

    assert(lines2.isDefined)
    val DebugRegex2 = "process 'step3' output tuple: \\[foo, (.*)\\]".r
    val DebugRegex2(path2) = lines2.get

    val src2 = Source.fromFile(path2)
    try {
      val step3Out2 = src2.getLines.mkString
      assert(step3Out2.matches("^11 .*$"))
    } finally {
      src2.close()
    }

  }

  override def afterAll() {
    IO.deleteRecursively(temporaryFolder)
  }
}
