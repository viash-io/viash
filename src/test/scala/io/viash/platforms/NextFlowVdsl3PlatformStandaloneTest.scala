package io.viash.platforms

import io.viash.helpers.IO
import io.viash.{DockerTest, NextFlowTest, TestHelper}
import org.scalatest.{BeforeAndAfterAll, FunSuite}

import java.io.File
import java.nio.file.{Files, Path, Paths}
import scala.io.Source

import java.io.IOException
import java.io.UncheckedIOException

class NextFlowVdsl3PlatformStandaloneTest extends FunSuite with BeforeAndAfterAll {
  // temporary folder to work in
  private val temporaryFolder = IO.makeTemp("viash_tester_nextflowvdsl3")
  private val tempFolStr = temporaryFolder.toString

  // path to namespace components
  private val rootPath = getClass.getResource("/testnextflowvdsl3/").getPath
  private val srcPath = Paths.get(tempFolStr, "src").toFile.toString
  private val targetPath = Paths.get(tempFolStr, "target").toFile.toString
  private val resourcesPath = Paths.get(tempFolStr, "resources").toFile.toString
  private val workflowsPath = Paths.get(tempFolStr, "workflows").toFile.toString

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
  def runNextflowProcess(
    mainScript: String,
    args: List[String],
    entry: Option[String] = None,
    paramsFile: Option[String] = None,
    cwd: File = new File(tempFolStr), 
    extraEnv: Seq[(String, String)] = Nil,
    quiet: Boolean = false
  ): (Int, String, String) = {

    import sys.process._

    val stdOut = new StringBuilder
    val stdErr = new StringBuilder

    val command = 
      "nextflow" :: 
        { if (quiet) List("-q") else Nil } ::: 
        "run" :: "." ::
        "-main-script" :: mainScript ::
        { if (entry.isDefined) List("-entry", entry.get) else Nil } :::
        { if (paramsFile.isDefined) List("-params-file", paramsFile.get) else Nil } :::
        args

    val extraEnv_ = extraEnv :+ ( "NXF_VER" -> "22.04.5")

    val exitCode = Process(command, cwd, extraEnv_ : _*).!(ProcessLogger(str => stdOut ++= s"$str\n", str => stdErr ++= s"$str\n"))

    (exitCode, stdOut.toString, stdErr.toString)
  }

  // convert testbash

  // copy resources to temporary folder so we can build in a clean environment
  for (resource <- List("src", "workflows", "resources"))
    TestHelper.copyFolder(Paths.get(rootPath, resource).toString, Paths.get(tempFolStr, resource).toString)

  test("Build pipeline components", DockerTest, NextFlowTest) {
    // build the nextflow containers
    val (_, _) = TestHelper.testMainWithStdErr(
      "ns", "build",
      "-s", srcPath,
      "-t", targetPath,
      "--setup", "cb"
    )
  }

  test("Run module as standalone", NextFlowTest) {
    val (exitCode, stdOut, stdErr) = runNextflowProcess(
      mainScript = "target/nextflow/step2/main.nf",
      args = List(
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
      mainScript = "target/nextflow/step2/main.nf",
      args = List(
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
      mainScript = "target/nextflow/step2/main.nf",
      args = List(
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
