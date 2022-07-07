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

  def outputTupleProcessor(output: String, headerKeyword: String) = {
    val stdOutLines = output.split("\n")

    val DebugRegex = s"$headerKeyword: \\[(.*), \\[(.*)\\]\\]".r
    val debugPrints = stdOutLines.flatMap{ _ match {
      case DebugRegex(id, argStr) =>

        // turn argStr into Map[String, String]
        import io.circe.yaml.parser
        import io.circe.Json

        val input2 = "{" + argStr.replaceAll(":", ": ") + "}"

        val js = parser.parse(input2).right.get
        val js2 = js.mapObject(_.mapValues{v => v match {
          case a if a.isArray => Json.fromString(a.asArray.get.map(_.asString.get).mkString("[", ", ", "]"))
          case a if a.isString => Json.fromString(a.asString.get)
          case _ => Json.fromString(v.toString)
        }})

        val argMap = js2.as[Map[String, String]].right.get

        Some((id, argMap))
      case _ => None
    }}

    debugPrints
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

  test("Run config pipeline", NextFlowTest) {

    val (exitCode, stdOut, stdErr) = runNextflowProcess(
      Seq(
        "-main-script", "workflows/pipeline3/main.nf",
        "--id", "foo",
        "--input", "resources/lines3.txt",
        "--real_number", "10.5",
        "--whole_number", "10",
        "--str", "foo",
        "--multiple", "a:b:c:d",
        "--publishDir", "output",
        "-entry", "base",
      )
    )

    assert(exitCode == 0, s"\nexit code was $exitCode\nStd output:\n$stdOut\nStd error:\n$stdErr")

    val debugPrints = outputTupleProcessor(stdOut, "DEBUG")

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
    assert(fooDebugArgs.contains("multiple"))
    assert(fooDebugArgs("multiple") == "[a, b, c, d]")
  }

  val expectedFoo: Map[String, Option[String]] = Map(
    ("input", Some(".*/lines3.txt")),
    ("real_number", Some("10.5")),
    ("whole_number", Some("3")),
    ("str", Some("foo")),
    ("truth", Some("false")),
    ("falsehood", Some("true")),
    ("reality", None),
    ("optional", None),
    ("optional_with_default", Some("foo")),
    ("multiple", Some("[a, b, c]"))
  )
  val expectedBar: Map[String, Option[String]] = Map(
    ("input", Some(".*/lines5.txt")),
    ("real_number", Some("0.5")),
    ("whole_number", Some("10")),
    ("str", Some("foo")),
    ("truth", Some("false")),
    ("falsehood", Some("true")),
    ("reality", Some("true")),
    ("optional", Some("bar")),
    ("optional_with_default", Some("The default value.")),
    ("multiple", None)
  )

  def checkDebugArgs(id: String, debugArgs: Map[String, String], expectedValues: Map[String, Option[String]]) {
    for((name, value) <- expectedValues) {
      if (value != None) {
        assert(debugArgs.contains(name), s"$id - contains $name")
        if (name == "input")
          assert(debugArgs(name).matches(value.get), s"$id - match value $name")
        else
          assert(debugArgs(name) == value.get, s"$id - check value $name")
      } else {
        assert(!debugArgs.contains(name), s"$id - not contains $name")
      }
    }
  }

  test("Run config pipeline with yamlblob", NextFlowTest) {
    val fooArgs = "{id: foo, input: resources/lines3.txt, whole_number: 3, optional_with_default: foo, multiple: [a, b, c]}"
    val barArgs = "{id: bar, input: resources/lines5.txt, real_number: 0.5, optional: bar, reality: true}"

    val (exitCode, stdOut, stdErr) = runNextflowProcess(
      Seq(
        "-main-script", "workflows/pipeline3/main.nf",
        "--param_list", s"[$fooArgs, $barArgs]",
        "--real_number", "10.5",
        "--whole_number", "10",
        "--str", "foo",
        "--publishDir", "output",
        "-entry", "base",
      )
    )

    assert(exitCode == 0, s"\nexit code was $exitCode\nStd output:\n$stdOut\nStd error:\n$stdErr")
    
    val debugPrints = outputTupleProcessor(stdOut, "DEBUG")

    val fooDebug = debugPrints.find(_._1 == "foo")
    assert(fooDebug.isDefined)
    checkDebugArgs("foo", fooDebug.get._2, expectedFoo)

    val barDebug = debugPrints.find(_._1 == "bar")
    assert(barDebug.isDefined)
    checkDebugArgs("bar", barDebug.get._2, expectedBar)
  }

  test("Run config pipeline with yaml file", NextFlowTest) {
    val param_list_file = getClass.getResource("/testnextflowvdsl3/param_list_files/pipeline3.yaml").getPath
    val (exitCode, stdOut, stdErr) = runNextflowProcess(
      Seq(
        "-main-script", "workflows/pipeline3/main.nf",
        "--param_list", param_list_file,
        "--real_number", "10.5",
        "--whole_number", "10",
        "--str", "foo",
        "--publishDir", "output",
        "-entry", "base",
      )
    )

    assert(exitCode == 0, s"\nexit code was $exitCode\nStd output:\n$stdOut\nStd error:\n$stdErr")
    
    val debugPrints = outputTupleProcessor(stdOut, "DEBUG")

    val fooDebug = debugPrints.find(_._1 == "foo")
    assert(fooDebug.isDefined)
    checkDebugArgs("foo", fooDebug.get._2, expectedFoo)

    val barDebug = debugPrints.find(_._1 == "bar")
    assert(barDebug.isDefined)
    checkDebugArgs("bar", barDebug.get._2, expectedBar)
  }

  test("Run config pipeline with json file", NextFlowTest) {
    val param_list_file = getClass.getResource("/testnextflowvdsl3/param_list_files/pipeline3.json").getPath
    val (exitCode, stdOut, stdErr) = runNextflowProcess(
      Seq(
        "-main-script", "workflows/pipeline3/main.nf",
        "--param_list", param_list_file,
        "--real_number", "10.5",
        "--whole_number", "10",
        "--str", "foo",
        "--publishDir", "output",
        "-entry", "base",
      )
    )

    assert(exitCode == 0, s"\nexit code was $exitCode\nStd output:\n$stdOut\nStd error:\n$stdErr")
    
    val debugPrints = outputTupleProcessor(stdOut, "DEBUG")

    val fooDebug = debugPrints.find(_._1 == "foo")
    assert(fooDebug.isDefined)
    checkDebugArgs("foo", fooDebug.get._2, expectedFoo)

    val barDebug = debugPrints.find(_._1 == "bar")
    assert(barDebug.isDefined)
    checkDebugArgs("bar", barDebug.get._2, expectedBar)
  }

  // todo: try out with paramlist json
  // todo: try out with paramlist yaml
  // todo: try out with paramlist csv
  // todo: try out with paramlist asis

  override def afterAll() {
    IO.deleteRecursively(temporaryFolder)
  }
}
