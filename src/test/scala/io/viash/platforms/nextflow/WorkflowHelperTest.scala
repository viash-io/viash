package io.viash.platforms.nextflow

import io.viash.helpers.{IO, Logger}
import io.viash.{DockerTest, NextflowTest, TestHelper}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite

import java.io.File
import java.nio.file.{Files, Path, Paths}
import scala.io.Source

import java.io.IOException
import java.io.UncheckedIOException

class WorkflowHelperTest extends AnyFunSuite with BeforeAndAfterAll {
  Logger.UseColorOverride.value = Some(false)
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

  test("Build pipeline components", DockerTest, NextflowTest) {
    // build the nextflow containers
    val (_, _, _) = TestHelper.testMainWithStdErr(
      "ns", "build",
      "-s", srcPath,
      "-t", targetPath,
      "--setup", "cb"
    )
  }
  
  val expectedFoo: List[CheckArg] = List(
    MatchCheck("input", ".*/lines3.txt"),
    EqualsCheck("real_number", "10.5"),
    EqualsCheck("whole_number", "3"),
    EqualsCheck("str", "foo"),
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
    EqualsCheck("reality", "true"),
    EqualsCheck("optional", "bar"),
    EqualsCheck("optional_with_default", "The default value."),
    NotAvailCheck("multiple")
  )

  test("Run config pipeline", NextflowTest) {

    val (exitCode, stdOut, stdErr) = runNextflowProcess(
      mainScript = "workflows/pipeline3/main.nf",
      entry = Some("base"),
      args = List(
        "--id", "foo",
        "--input", "resources/lines3.txt",
        "--real_number", "10.5",
        "--whole_number", "3",
        "--str", "foo",
        "--optional_with_default", "foo",
        "--multiple", "a:b:c",
        "--publish_dir", "output",
      )
    )

    assert(exitCode == 0, s"\nexit code was $exitCode\nStd output:\n$stdOut\nStd error:\n$stdErr")

    val debugPrints = outputTupleProcessor(stdOut, "DEBUG")
    checkDebugArgs("foo", debugPrints, expectedFoo)
  }

  test("Run config pipeline with yamlblob", NextflowTest) {
    val fooArgs = "{id: foo, input: resources/lines3.txt, whole_number: 3, optional_with_default: foo, multiple: [a, b, c]}"
    val barArgs = "{id: bar, input: resources/lines5.txt, real_number: 0.5, optional: bar, reality: true}"

    val (exitCode, stdOut, stdErr) = runNextflowProcess(
      mainScript = "workflows/pipeline3/main.nf",
      entry = Some("base"),
      args = List(
        "--param_list", s"[$fooArgs, $barArgs]",
        "--real_number", "10.5",
        "--whole_number", "10",
        "--str", "foo",
        "--publish_dir", "output",
      )
    )

    assert(exitCode == 0, s"\nexit code was $exitCode\nStd output:\n$stdOut\nStd error:\n$stdErr")
    
    val debugPrints = outputTupleProcessor(stdOut, "DEBUG")
    checkDebugArgs("foo", debugPrints, expectedFoo)
    checkDebugArgs("bar", debugPrints, expectedBar)
    // Check location of resource file, vdsl3 makes it relative to the param_list file, yamlblob or asis can't do that so there it must be relative to the workflow
    assert(debugPrints.find(_._1 == "foo").get._2("input").endsWith(resourcesPath+"/lines3.txt"))
  }

  test("Run config pipeline with yaml file", NextflowTest) {
    val param_list_file = Paths.get(resourcesPath, "pipeline3.yaml").toFile.toString
    val (exitCode, stdOut, stdErr) = runNextflowProcess(
      mainScript = "workflows/pipeline3/main.nf",
      entry = Some("base"),
      args = List(
        "--param_list", param_list_file,
        "--real_number", "10.5",
        "--whole_number", "10",
        "--str", "foo",
        "--publish_dir", "output",
        )
    )

    assert(exitCode == 0, s"\nexit code was $exitCode\nStd output:\n$stdOut\nStd error:\n$stdErr")
    
    val debugPrints = outputTupleProcessor(stdOut, "DEBUG")
    checkDebugArgs("foo", debugPrints, expectedFoo)
    checkDebugArgs("bar", debugPrints, expectedBar)
    // Check location of resource file, vdsl3 makes it relative to the param_list file, yamlblob or asis can't do that so there it must be relative to the workflow
    assert(debugPrints.find(_._1 == "foo").get._2("input").endsWith(resourcesPath+"/lines3.txt"))
  }

test("Run config pipeline with yaml file passed as a relative path", NextflowTest) {
    val (exitCode, stdOut, stdErr) = runNextflowProcess(
      mainScript = "../workflows/pipeline3/main.nf",
      entry = Some("base"),
      args = List(
        "--param_list", "pipeline3.yaml",
        "--real_number", "10.5",
        "--whole_number", "10",
        "--str", "foo",
        "--publish_dir", "output",
        ),
      cwd = Paths.get(resourcesPath).toFile
    )

    assert(exitCode == 0, s"\nexit code was $exitCode\nStd output:\n$stdOut\nStd error:\n$stdErr")
    val debugPrints = outputTupleProcessor(stdOut, "DEBUG")
    checkDebugArgs("foo", debugPrints, expectedFoo)
    checkDebugArgs("bar", debugPrints, expectedBar)
    // Check location of resource file, vdsl3 makes it relative to the param_list file, yamlblob or asis can't do that so there it must be relative to the workflow
    assert(debugPrints.find(_._1 == "foo").get._2("input").endsWith(resourcesPath+"/lines3.txt"))
  }


  test("Run config pipeline with json file", NextflowTest) {
    val param_list_file = Paths.get(resourcesPath, "pipeline3.json").toFile.toString
    val (exitCode, stdOut, stdErr) = runNextflowProcess(
      mainScript = "workflows/pipeline3/main.nf",
      entry = Some("base"),
      args = List(
        "--param_list", param_list_file,
        "--real_number", "10.5",
        "--whole_number", "10",
        "--str", "foo",
        "--publish_dir", "output",
        )
    )

    assert(exitCode == 0, s"\nexit code was $exitCode\nStd output:\n$stdOut\nStd error:\n$stdErr")
    
    val debugPrints = outputTupleProcessor(stdOut, "DEBUG")
    checkDebugArgs("foo", debugPrints, expectedFoo)
    checkDebugArgs("bar", debugPrints, expectedBar)
    // Check location of resource file, vdsl3 makes it relative to the param_list file, yamlblob or asis can't do that so there it must be relative to the workflow
    assert(debugPrints.find(_._1 == "foo").get._2("input").endsWith(resourcesPath+"/lines3.txt"))
  }

  test("Run config pipeline with csv file", NextflowTest) {
    val param_list_file = Paths.get(resourcesPath, "pipeline3.csv").toFile.toString
    val (exitCode, stdOut, stdErr) = runNextflowProcess(
      mainScript = "workflows/pipeline3/main.nf",
      entry = Some("base"),
      args = List(
        "--param_list", param_list_file,
        "--real_number", "10.5",
        "--whole_number", "10",
        "--str", "foo",
        "--publish_dir", "output",
        )
    )

    assert(exitCode == 0, s"\nexit code was $exitCode\nStd output:\n$stdOut\nStd error:\n$stdErr")
    
    val debugPrints = outputTupleProcessor(stdOut, "DEBUG")
    checkDebugArgs("foo", debugPrints, expectedFoo)
    checkDebugArgs("bar", debugPrints, expectedBar)
    // Check location of resource file, vdsl3 makes it relative to the param_list file, yamlblob or asis can't do that so there it must be relative to the workflow
    assert(debugPrints.find(_._1 == "foo").get._2("input").endsWith(resourcesPath+"/lines3.txt"))
  }

  test("Run config pipeline asis, default nextflow implementation", NextflowTest) {
    val param_list_file = Paths.get(resourcesPath, "pipeline3.asis.yaml").toFile.toString
    val (exitCode, stdOut, stdErr) = runNextflowProcess(
      mainScript = "workflows/pipeline3/main.nf",
      entry = Some("base"),
      paramsFile = Some(param_list_file),
      args = List(
        "--real_number", "10.5",
        "--whole_number", "10",
        "--str", "foo",
        "--publish_dir", "output",
      )
    )

    assert(exitCode == 0, s"\nexit code was $exitCode\nStd output:\n$stdOut\nStd error:\n$stdErr")
    
    val debugPrints = outputTupleProcessor(stdOut, "DEBUG")
    checkDebugArgs("foo", debugPrints, expectedFoo)
    checkDebugArgs("bar", debugPrints, expectedBar)
    // Check location of resource file, vdsl3 makes it relative to the param_list file, yamlblob or asis can't do that so there it must be relative to the workflow
    assert(debugPrints.find(_._1 == "foo").get._2("input").endsWith(resourcesPath+"/lines3.txt"))
  }

  override def afterAll(): Unit = {
    IO.deleteRecursively(temporaryFolder)
  }
}
