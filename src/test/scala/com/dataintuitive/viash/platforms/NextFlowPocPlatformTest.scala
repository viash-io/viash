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

  def outputFileMatchChecker(output: String, headerKeyword: String, fileContentMatcher: String) = {
    val lines = output.split("\n").find(_.contains(headerKeyword))

    assert(lines.isDefined)
    val DebugRegex = s"$headerKeyword: \\[foo, (.*)\\]".r
    val DebugRegex(path) = lines.get

    val src = Source.fromFile(path)
    try {
      val step3Out = src.getLines.mkString
      assert(step3Out.matches(fileContentMatcher))
    } finally {
      src.close()
    }
  }

  // convert testbash
  test("Build pipeline components", DockerTest, NextFlowTest) {

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
      Seq("nextflow", "run", ".",
      "-main-script", "workflows/pipeline1/main.nf",
      "--input", "resources/*",
      "--publishDir", "output",
      "-entry", "base",
      ),
      new File(tempFolStr),
      "NXF_VER" -> "21.04.1"
    ).!!

    outputFileMatchChecker(output, "DEBUG6", "^11 .*$")

  }

  test("Run pipeline with components using map functionality", DockerTest, NextFlowTest) {

    import sys.process._
    val output = Process(
      Seq("nextflow", "run", ".",
      "-main-script", "workflows/pipeline1/main.nf",
      "--input", "resources/*",
      "--publishDir", "output",
      "-entry", "map_variant",
      ),
      new File(tempFolStr),
      "NXF_VER" -> "21.04.1"
    ).!!

    outputFileMatchChecker(output, "DEBUG4", "^11 .*$")

  }

  test("Run pipeline with components using mapData functionality", DockerTest, NextFlowTest) {

    import sys.process._
    val output = Process(
      Seq("nextflow", "run", ".",
      "-main-script", "workflows/pipeline1/main.nf",
      "--input", "resources/*",
      "--publishDir", "output",
      "-entry", "mapData_variant",
      ),
      new File(tempFolStr),
      "NXF_VER" -> "21.04.1"
    ).!!

    outputFileMatchChecker(output, "DEBUG4", "^11 .*$")

  }

  test("Run pipeline with debug = false", DockerTest, NextFlowTest) {

    import sys.process._
    val output = Process(
      Seq("nextflow", "run", ".",
        "-main-script", "workflows/pipeline1/main.nf",
        "--input", "resources/*",
        "--publishDir", "output",
        "-entry", "debug_variant",
        "--displayDebug", "false",
        ),
      new File(tempFolStr),
      "NXF_VER" -> "21.04.1"
    ).!!

    outputFileMatchChecker(output, "DEBUG4", "^11 .*$")

    val lines2 = output.split("\n").find(_.contains("process 'step3' output tuple"))
    assert(!lines2.isDefined)

  }

  test("Run pipeline with debug = true", DockerTest, NextFlowTest) {

    import sys.process._
    val output = Process(
      Seq("nextflow", "run", ".",
        "-main-script", "workflows/pipeline1/main.nf",
        "--input", "resources/*",
        "--publishDir", "output",
        "-entry", "debug_variant",
        "--displayDebug", "true",
        ),
      new File(tempFolStr),
      "NXF_VER" -> "21.04.1"
    ).!!

    outputFileMatchChecker(output, "DEBUG4", "^11 .*$")
    outputFileMatchChecker(output, "process 'step3' output tuple", "^11 .*$")

  }

  test("Run legacy pipeline", DockerTest, NextFlowTest) {

    import sys.process._
    val output = Process(
      Seq("nextflow", "run", ".",
      "-main-script", "workflows/pipeline2/main.nf",
      "--input", "resources/*",
      "--publishDir", "output",
      "-entry", "legacy_base",
      ),
      new File(tempFolStr),
      "NXF_VER" -> "21.04.1"
    ).!!

    outputFileMatchChecker(output, "DEBUG6", "^11 .*$")

  }

    test("Run legacy and poc combined pipeline", DockerTest, NextFlowTest) {

    import sys.process._
    val output = Process(
      Seq("nextflow", "run", ".",
      "-main-script", "workflows/pipeline2/main.nf",
      "--input", "resources/*",
      "--publishDir", "output",
      "-entry", "legacy_and_poc",
      ),
      new File(tempFolStr),
      "NXF_VER" -> "21.04.1"
    ).!!

    outputFileMatchChecker(output, "DEBUG6", "^11 .*$")

  }

  override def afterAll() {
    IO.deleteRecursively(temporaryFolder)
  }
}
