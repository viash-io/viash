package io.viash.platforms.nextflow

import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite

import java.io.File
import java.nio.file.{Files, Path, Paths}
import java.io.IOException
import java.io.UncheckedIOException

import scala.io.Source

import io.viash.helpers.{IO, Logger}
import io.viash.{DockerTest, NextflowTest, TestHelper}
import io.viash.NextflowTestHelper

class Vdsl3ModuleTest extends AnyFunSuite with BeforeAndAfterAll {
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

  def outputFileMatchChecker(output: String, headerKeyword: String, fileContentMatcher: String) = {
    val DebugRegex = s"$headerKeyword: \\[foo, (.*)\\]".r

    val lines = output.split("\n").find(DebugRegex.findFirstIn(_).isDefined)

    assert(lines.isDefined)
    val DebugRegex(path) = lines.get

    val src = Source.fromFile(path)
    try {
      val step3Out = src.getLines().mkString
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

        val js = parser.parse(input2).toOption.get
        val js2 = js.mapObject(_.mapValues{v => v match {
          case a if a.isArray => Json.fromString(a.asArray.get.map(_.asString.get).mkString("[", ", ", "]"))
          case a if a.isString => Json.fromString(a.asString.get)
          case _ => Json.fromString(v.toString)
        }})

        val argMap = js2.as[Map[String, String]].toOption.get

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
  def checkDebugArgs(id: String, debugPrints: Array[(String, Map[String,String])], expectedValues: List[CheckArg]): Unit = {
    val idDebugPrints = debugPrints.find(_._1 == id)
    assert(idDebugPrints.isDefined)
    expectedValues.foreach(_.assert(id, idDebugPrints.get._2))
  }

  // convert testbash

  // copy resources to temporary folder so we can build in a clean environment
  for (resource <- List("src", "workflows", "resources"))
    IO.copyFolder(Paths.get(rootPath, resource).toString, Paths.get(tempFolStr, resource).toString)

  test("Build pipeline components", DockerTest, NextflowTest) {
    // build the nextflow containers
    val (_, _, _) = TestHelper.testMainWithStdErr(
      "ns", "build",
      "--runner", "nextflow",
      "-s", srcPath,
      "-t", targetPath,
      "--setup", "cb"
    )
  }
  
  test("Run pipeline", DockerTest, NextflowTest) {
    val (exitCode, stdOut, stdErr) = NextflowTestHelper.run(
      mainScript = "workflows/pipeline1/main.nf",
      entry = Some("base"),
      args = List("--publish_dir", "output"),
      cwd = tempFolFile
    )

    assert(exitCode == 0, s"\nexit code was $exitCode\nStd output:\n$stdOut\nStd error:\n$stdErr")
    outputFileMatchChecker(stdOut, "DEBUG6", "^11 .*$")

    // check whether step3's debug printing was triggered
    outputFileMatchChecker(stdOut, "process 'step3[^']*' output tuple", "^11 .*$")

    // check whether step2's debug printing was not triggered
    val lines2 = stdOut.split("\n").find(_.contains("process 'step2' output tuple"))
    assert(!lines2.isDefined)
  }

  test("Test map/mapData/id arguments", DockerTest, NextflowTest) {

    val (exitCode, stdOut, stdErr) = NextflowTestHelper.run(
      mainScript = "workflows/pipeline1/main.nf",
      entry = Some("test_map_mapdata_mapid_arguments"),
      args = List("--publish_dir", "output"),
      cwd = tempFolFile
    )

    assert(exitCode == 0, s"\nexit code was $exitCode\nStd output:\n$stdOut\nStd error:\n$stdErr")
  }

  test("Test fromState/toState arguments", DockerTest, NextflowTest) {

    val (exitCode, stdOut, stdErr) = NextflowTestHelper.run(
      mainScript = "workflows/pipeline1/main.nf",
      entry = Some("test_fromstate_tostate_arguments"),
      args = List("--publish_dir", "output"),
      cwd = tempFolFile
    )

    assert(exitCode == 0, s"\nexit code was $exitCode\nStd output:\n$stdOut\nStd error:\n$stdErr")
  }

  test("Check whether --help is same as Viash's --help", NextflowTest) {
    // except that WorkflowHelper.nf will not print alternatives, and
    // will always prefix argument names with -- (so --foo, not -f or foo).

    // run WorkflowHelper's --help
    val (exitCode, stdOut1, stdErr1) = NextflowTestHelper.run(
      mainScript = "workflows/pipeline3/main.nf",
      entry = Some("base"),
      args = List("--help"),
      quiet = true,
      cwd = tempFolFile
    )

    assert(exitCode == 0, s"\nexit code was $exitCode\nStd output:\n$stdOut1\nStd error:\n$stdErr1")

    // explicitly remove defaults set by output files
    // these defaults make sense in nextflow but not in viash
    val correctedStdOut1 = stdOut1.replaceAll("        default: \\$id\\.\\$key\\.[^\n]*\n", "")
    // explicitly remove global arguments
    // these arguments make sense in nextflow but not in viash
    import java.util.regex.Pattern
    val regex = Pattern.compile("\nNextflow input-output arguments:.*", Pattern.DOTALL)
    val correctedStdOut2 = regex.matcher(correctedStdOut1).replaceAll("")

    // run Viash's --help
    val (stdOut2, stdErr2, exitCode2) = TestHelper.testMainWithStdErr(
      "run", workflowsPath + "/pipeline3/config.vsh.yaml",
      "--", "--help"
    )

    assert(exitCode2 == 0)

    // check if they are the same
    assert(correctedStdOut2 == stdOut2)
  }

  override def afterAll(): Unit = {
    IO.deleteRecursively(temporaryFolder)
  }
}
