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
  private val resourcesPath = Paths.get(tempFolStr, "resources").toFile.toString

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

  trait CheckArg {
    val name: String
    def assert(id: String, args: Map[String, String]): Unit
  }
  case class MatchCheck(name: String, s: String) extends CheckArg {
    import org.scalatest.Assertions.{assert => scassert}
    def assert(id: String, args: Map[String, String]) = {
      scassert(args.contains(name), s"$id : args should contain $name")
      scassert(args(name).matches(s), s"$id : args($name): ${args(name)} should match $s")
    }
  }
  case class EqualsCheck(name: String, s: String) extends CheckArg {
    import org.scalatest.Assertions.{assert => scassert}
    def assert(id: String, args: Map[String, String]) = {
      scassert(args.contains(name), s"$id : args should contain $name")
      scassert(args(name) == s, s"$id : args($name) should equal $s")
    }
  }
  case class NotAvailCheck(name: String) extends CheckArg {
    import org.scalatest.Assertions.{assert => scassert}
    def assert(id: String, args: Map[String, String]) = {
      scassert(!args.contains(name), s"$id : args should not contain $name")
    }
  }
  def checkDebugArgs(id: String, debugPrints: Array[(String, Map[String,String])], expectedValues: List[CheckArg]) {
    val idDebugPrints = debugPrints.find(_._1 == id)
    assert(idDebugPrints.isDefined)
    expectedValues.foreach(_.assert(id, idDebugPrints.get._2))
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

  val expectedFoo: List[CheckArg] = List(
    MatchCheck("input", ".*/lines3.txt"),
    EqualsCheck("real_number", "10.5"),
    EqualsCheck("whole_number", "3"),
    EqualsCheck("str", "foo"),
    EqualsCheck("truth", "false"),
    EqualsCheck("falsehood", "true"),
    NotAvailCheck("reality"),
    NotAvailCheck("optional"),
    EqualsCheck("optional_with_default", "foo"),
    EqualsCheck("multiple", "[a, b, c]")
  )
  val expectedBar: List[CheckArg] = List(
    MatchCheck("input", ".*/lines5.txt"),
    EqualsCheck("real_number", "0.5"),
    EqualsCheck("whole_number", "10"),
    EqualsCheck("str", "foo"),
    EqualsCheck("truth", "false"),
    EqualsCheck("falsehood", "true"),
    EqualsCheck("reality", "true"),
    EqualsCheck("optional", "bar"),
    EqualsCheck("optional_with_default", "The default value."),
    NotAvailCheck("multiple")
  )

  test("Run config pipeline", NextFlowTest) {

    val (exitCode, stdOut, stdErr) = runNextflowProcess(
      Seq(
        "-main-script", "workflows/pipeline3/main.nf",
        "--id", "foo",
        "--input", "resources/lines3.txt",
        "--real_number", "10.5",
        "--whole_number", "3",
        "--str", "foo",
        "--optional_with_default", "foo",
        "--multiple", "a:b:c",
        "--publishDir", "output",
        "-entry", "base",
      )
    )

    assert(exitCode == 0, s"\nexit code was $exitCode\nStd output:\n$stdOut\nStd error:\n$stdErr")

    val debugPrints = outputTupleProcessor(stdOut, "DEBUG")
    checkDebugArgs("foo", debugPrints, expectedFoo)
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
    checkDebugArgs("foo", debugPrints, expectedFoo)
    checkDebugArgs("bar", debugPrints, expectedBar)
    // Check location of resource file, vdsl3 makes it relative to the param_list file, yamlblob or asis can't do that so there it must be relative to the workflow
    assert(debugPrints.find(_._1 == "foo").get._2("input").equals(resourcesPath+"/lines3.txt"))
  }

  test("Run config pipeline with yaml file", NextFlowTest) {
    val param_list_file = Paths.get(resourcesPath, "pipeline3.yaml").toFile.toString
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
    checkDebugArgs("foo", debugPrints, expectedFoo)
    checkDebugArgs("bar", debugPrints, expectedBar)
    // Check location of resource file, vdsl3 makes it relative to the param_list file, yamlblob or asis can't do that so there it must be relative to the workflow
    assert(debugPrints.find(_._1 == "foo").get._2("input").equals(resourcesPath+"/lines3.txt"))
  }

  test("Run config pipeline with json file", NextFlowTest) {
    val param_list_file = Paths.get(resourcesPath, "pipeline3.json").toFile.toString
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
    checkDebugArgs("foo", debugPrints, expectedFoo)
    checkDebugArgs("bar", debugPrints, expectedBar)
    // Check location of resource file, vdsl3 makes it relative to the param_list file, yamlblob or asis can't do that so there it must be relative to the workflow
    assert(debugPrints.find(_._1 == "foo").get._2("input").equals(resourcesPath+"/lines3.txt"))
  }

  test("Run config pipeline with csv file", NextFlowTest) {
    val param_list_file = Paths.get(resourcesPath, "pipeline3.csv").toFile.toString
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
    checkDebugArgs("foo", debugPrints, expectedFoo)
    checkDebugArgs("bar", debugPrints, expectedBar)
    // Check location of resource file, vdsl3 makes it relative to the param_list file, yamlblob or asis can't do that so there it must be relative to the workflow
    assert(debugPrints.find(_._1 == "foo").get._2("input").equals(resourcesPath+"/lines3.txt"))
  }

  test("Run config pipeline asis, default nextflow implementation", NextFlowTest) {
    val param_list_file = Paths.get(resourcesPath, "pipeline3.asis.yaml").toFile.toString
    val (exitCode, stdOut, stdErr) = runNextflowProcess(
      Seq(
        "-main-script", "workflows/pipeline3/main.nf",
        "--real_number", "10.5",
        "--whole_number", "10",
        "--str", "foo",
        "--publishDir", "output",
        "-entry", "base",
        "-params-file", param_list_file
      )
    )

    assert(exitCode == 0, s"\nexit code was $exitCode\nStd output:\n$stdOut\nStd error:\n$stdErr")
    
    val debugPrints = outputTupleProcessor(stdOut, "DEBUG")
    checkDebugArgs("foo", debugPrints, expectedFoo)
    checkDebugArgs("bar", debugPrints, expectedBar)
    // Check location of resource file, vdsl3 makes it relative to the param_list file, yamlblob or asis can't do that so there it must be relative to the workflow
    assert(debugPrints.find(_._1 == "foo").get._2("input").equals(resourcesPath+"/lines3.txt"))
  }

  test("Run module as standalone", NextFlowTest) {
    val (exitCode, stdOut, stdErr) = runNextflowProcess(
      Seq(
        "-main-script", "target/nextflowvdsl3/step2/main.nf",
        "--input1", "resources/lines3.txt",
        "--input2", "resources/lines5.txt",
        "--publish_dir", "moduleOutput1"
      )
    )

    assert(exitCode == 0, s"\nexit code was $exitCode\nStd output:\n$stdOut\nStd error:\n$stdErr")
    
    val src = Source.fromFile(tempFolStr+"/moduleOutput1/run.step2.output1.txt")
    try {
      val moduleOut = src.getLines.mkString(",")
      assert(moduleOut.equals("one,two,three"))
    } finally {
      src.close()
    }
  }

  test("Run module as standalone, yamlblob", NextFlowTest) {
    val fooArgs = "{input1: resources/lines3.txt, input2: resources/lines5.txt}"
    val (exitCode, stdOut, stdErr) = runNextflowProcess(
      Seq(
        "-main-script", "target/nextflowvdsl3/step2/main.nf",
        "--param_list", s"[$fooArgs]",
        "--publish_dir", "moduleOutput2"
      )
    )

    assert(exitCode == 0, s"\nexit code was $exitCode\nStd output:\n$stdOut\nStd error:\n$stdErr")
    
    val src = Source.fromFile(tempFolStr+"/moduleOutput2/run.step2.output1.txt")
    try {
      val moduleOut = src.getLines.mkString(",")
      assert(moduleOut.equals("one,two,three"))
    } finally {
      src.close()
    }
  }

  test("Run module as standalone, test optional input", NextFlowTest) {

    Files.copy(Paths.get(resourcesPath, "lines5.txt"), Paths.get(resourcesPath, "lines5-bis.txt"))

    val (exitCode, stdOut, stdErr) = runNextflowProcess(
      Seq(
        "-main-script", "target/nextflowvdsl3/step2/main.nf",
        "--input1", "resources/lines3.txt",
        "--input2", "resources/lines5.txt",
        "--optional", "resources/lines5-bis.txt",
        "--publish_dir", "moduleOutput3"
      )
    )

    assert(exitCode == 0, s"\nexit code was $exitCode\nStd output:\n$stdOut\nStd error:\n$stdErr")
    
    val src = Source.fromFile(tempFolStr+"/moduleOutput3/run.step2.output1.txt")
    try {
      val moduleOut = src.getLines.mkString(",")
      assert(moduleOut.equals("one,two,three,1,2,3,4,5"))
    } finally {
      src.close()
    }
  }

  override def afterAll() {
    IO.deleteRecursively(temporaryFolder)
  }
}
