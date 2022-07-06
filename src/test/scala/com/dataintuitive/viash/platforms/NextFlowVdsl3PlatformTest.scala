package com.dataintuitive.viash.platforms

import com.dataintuitive.viash.helpers.IO
import com.dataintuitive.viash.{DockerTest, NextFlowTest, TestHelper}
import org.scalatest.{BeforeAndAfterAll, FunSuite}

import java.io.File
import java.nio.file.{Files, Path, Paths}
import scala.io.Source

import java.io.IOException
import java.io.UncheckedIOException

class NextFlowVdsl3PlatformTest extends FunSuite with BeforeAndAfterAll {
  // temporary folder to work in
  private val temporaryFolder = IO.makeTemp("viash_tester_nextflowvdsl3")
  private val tempFolStr = temporaryFolder.toString

  // path to namespace components
  private val rootPath = getClass.getResource("/testnextflowvdsl3/").getPath
  private val srcPath = Paths.get(tempFolStr, "src").toFile.toString
  private val targetPath = Paths.get(tempFolStr, "target").toFile.toString

  def outputFileMatchChecker(output: String, headerKeyword: String, fileContentMatcher: String) = {
    val DebugRegex = s"$headerKeyword: \\[foo, (.*)\\]".r

    val lines = output.split("\n").find(DebugRegex.findFirstIn(_).isDefined)

    assert(lines.isDefined)
    val DebugRegex(path) = lines.get

    val src = Source.fromFile(path)
    try {
      val step3Out = src.getLines.mkString
      assert(step3Out.matches(fileContentMatcher))
    } finally {
      src.close()
    }
  }

  // Wrapper function to make logging of processes easier, provide default command to run nextflow from . directory
  // TODO: consider reading nextflow dot files and provide extra info of which workflow step fails and how
  def runNextflowProcess(variableCommand: Seq[String], cwd: File = new File(tempFolStr), extraEnv: Seq[(String, String)] = Nil): (Int, String, String) = {

    import sys.process._

    val stdOut = new StringBuilder
    val stdErr = new StringBuilder

    val fixedCommand = Seq("nextflow", "run", ".")

    val extraEnv_ = extraEnv :+ ( "NXF_VER" -> "21.04.1" )

    val exitCode = Process(fixedCommand ++ variableCommand, cwd, extraEnv_ : _*).!(ProcessLogger(str => stdOut ++= s"$str\n", str => stdErr ++= s"$str\n"))

    (exitCode, stdOut.toString, stdErr.toString)
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

    val (exitCode, stdOut, stdErr) = runNextflowProcess(
      Seq(
      "-main-script", "workflows/pipeline1/main.nf",
      "--input", "resources/*",
      "--publishDir", "output",
      "-entry", "base",
      )
    )

    assert(exitCode == 0, s"\nexit code was $exitCode\nStd output:\n$stdOut\nStd error:\n$stdErr")
    outputFileMatchChecker(stdOut, "DEBUG6", "^11 .*$")
  }

  test("Run pipeline with components using map functionality", DockerTest, NextFlowTest) {

    val (exitCode, stdOut, stdErr) = runNextflowProcess(
      Seq(
      "-main-script", "workflows/pipeline1/main.nf",
      "--input", "resources/*",
      "--publishDir", "output",
      "-entry", "map_variant",
      )
    )

    assert(exitCode == 0, s"\nexit code was $exitCode\nStd output:\n$stdOut\nStd error:\n$stdErr")
    outputFileMatchChecker(stdOut, "DEBUG4", "^11 .*$")
  }

  test("Run pipeline with components using mapData functionality", DockerTest, NextFlowTest) {

    val (exitCode, stdOut, stdErr) = runNextflowProcess(
      Seq(
      "-main-script", "workflows/pipeline1/main.nf",
      "--input", "resources/*",
      "--publishDir", "output",
      "-entry", "mapData_variant",
      )
    )

    assert(exitCode == 0, s"\nexit code was $exitCode\nStd output:\n$stdOut\nStd error:\n$stdErr")
    outputFileMatchChecker(stdOut, "DEBUG4", "^11 .*$")
  }

  test("Run pipeline with debug = false", DockerTest, NextFlowTest) {

    val (exitCode, stdOut, stdErr) = runNextflowProcess(
      Seq(
        "-main-script", "workflows/pipeline1/main.nf",
        "--input", "resources/*",
        "--publishDir", "output",
        "-entry", "debug_variant",
        "--displayDebug", "false",
        )
    )

    assert(exitCode == 0, s"\nexit code was $exitCode\nStd output:\n$stdOut\nStd error:\n$stdErr")
    outputFileMatchChecker(stdOut, "DEBUG4", "^11 .*$")

    val lines2 = stdOut.split("\n").find(_.contains("process 'step3' output tuple"))
    assert(!lines2.isDefined)

  }

  test("Run pipeline with debug = true", DockerTest, NextFlowTest) {

    val (exitCode, stdOut, stdErr) = runNextflowProcess(
      Seq(
        "-main-script", "workflows/pipeline1/main.nf",
        "--input", "resources/*",
        "--publishDir", "output",
        "-entry", "debug_variant",
        "--displayDebug", "true",
        )
    )

    assert(exitCode == 0, s"\nexit code was $exitCode\nStd output:\n$stdOut\nStd error:\n$stdErr")
    outputFileMatchChecker(stdOut, "DEBUG4", "^11 .*$")
    outputFileMatchChecker(stdOut, "process 'step3[^']*' output tuple", "^11 .*$")
  }

  test("Run legacy pipeline", DockerTest, NextFlowTest) {

    val (exitCode, stdOut, stdErr) = runNextflowProcess(
      Seq(
        "-main-script", "workflows/pipeline2/main.nf",
        "--input", "resources/*",
        "--publishDir", "output",
        "-entry", "legacy_base",
        )
    )

    assert(exitCode == 0, s"\nexit code was $exitCode\nStd output:\n$stdOut\nStd error:\n$stdErr")
    outputFileMatchChecker(stdOut, "DEBUG6", "^11 .*$")
  }

    test("Run legacy and vdsl3 combined pipeline", DockerTest, NextFlowTest) {

    val (exitCode, stdOut, stdErr) = runNextflowProcess(
      Seq(
        "-main-script", "workflows/pipeline2/main.nf",
        "--input", "resources/*",
        "--publishDir", "output",
        "-entry", "legacy_and_vdsl3",
        )
    )

    assert(exitCode == 0, s"\nexit code was $exitCode\nStd output:\n$stdOut\nStd error:\n$stdErr")
    outputFileMatchChecker(stdOut, "DEBUG6", "^11 .*$")
  }

  test("Run config pipeline", DockerTest, NextFlowTest) {

    val (exitCode, stdOut, stdErr) = runNextflowProcess(
      Seq(
        "-main-script", "workflows/pipeline3/main.nf",
        "--id", "foo",
        "--input", "resources/lines3.txt",
        "--real_number", "10.5",
        "--whole_number", "10",
        "--str", "foo",
        "--publishDir", "output",
        "-entry", "base",
      )
    )

    assert(exitCode == 0, s"\nexit code was $exitCode\nStd output:\n$stdOut\nStd error:\n$stdErr")
    
    val stdOutLines = stdOut.split("\n")

    val DebugRegex = s"DEBUG: \\[(.*), \\[(.*)\\]\\]".r
    val debugPrints = stdOutLines.filter(DebugRegex.findFirstIn(_).isDefined).map{
      case DebugRegex(id, argStr) => 
        val argMap = argStr.split(", ").flatMap{ entry =>
          val spl = entry.split(":")
          if (spl.length == 2) {
            Some((spl(0), spl(1)))
          } else {
            None
          }
        }.toMap
        (id, argMap)
    }

    val fooDebug = debugPrints.find(_._1 == "foo")
    assert(fooDebug.isDefined)
    val fooDebugArgs = fooDebug.get._2
    assert(fooDebugArgs.contains("input"))
    assert(fooDebugArgs("input").matches(".*/lines3.txt"))
    assert(fooDebugArgs.contains("real_number"))
    assert(fooDebugArgs("real_number") == "10.5")
    assert(fooDebugArgs.contains("whole_number"))
    assert(fooDebugArgs("whole_number") == "10")
    assert(fooDebugArgs.contains("str"))
    assert(fooDebugArgs("str") == "foo")
    assert(fooDebugArgs.contains("truth"))
    assert(fooDebugArgs("truth") == "false")
    assert(fooDebugArgs.contains("falsehood"))
    assert(fooDebugArgs("falsehood") == "true")
    assert(!fooDebugArgs.contains("reality"))
    // assert(fooDebugArgs("reality") == "null")
    assert(!fooDebugArgs.contains("optional"))
    // assert(fooDebugArgs("optional") == "null")
    assert(fooDebugArgs.contains("optional_with_default"))
    assert(fooDebugArgs("optional_with_default") == "The default value.")
    assert(!fooDebugArgs.contains("multiple"))
    // assert(fooDebugArgs("multiple") == "null")
  }

  // todo: try out with paramlist json
  // todo: try out with paramlist yaml
  // todo: try out with paramlist csv
  // todo: try out with paramlist yamlblob
  // todo: try out with paramlist asis

  override def afterAll() {
    IO.deleteRecursively(temporaryFolder)
  }
}
