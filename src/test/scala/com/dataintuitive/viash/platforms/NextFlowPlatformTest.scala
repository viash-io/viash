package com.dataintuitive.viash.platforms

import com.dataintuitive.viash.{DockerTest, NextFlowTest, TestHelper}
import org.scalatest.FunSuite

import java.io.File
import java.nio.file.Paths
import scala.io.Source


class NextFlowPlatformTest extends FunSuite {
  // path to namespace components
  private val rootPath = getClass.getResource("/testnextflow/").getPath
  private val srcPath = Paths.get(rootPath, "src").toFile.toString
  private val targetPath = Paths.get(rootPath, "target").toFile.toString

  // convert testbash
  test("build pipeline components", DockerTest, NextFlowTest) {
    val (_, _) = TestHelper.testMainWithStdErr(
      "ns", "build",
      "-s", srcPath,
      "-t", targetPath
    )

    import sys.process._
    val output = Process(
      Seq("nextflow", "run", ".", "-main-script", "workflows/main.nf", "input", "resources/*", "--publishDir", "output"),
      new File(rootPath),
      "NXF_VER" -> "21.04.1"
    ).!!
    val lines = output.split("\n").find(_.contains("DEBUG7"))

    assert(lines.isDefined)
    val DebugRegex = "\\[DEBUG7, foo, (.*)\\]".r
    val DebugRegex(path) = lines.get

    val step3Out = Source.fromFile(path).getLines.mkString
    assert(step3Out.matches("^11 .*$"))

    // TODO: check other debug flags as well.
    // TODO: change step3 into something more interesting.
  }
}
