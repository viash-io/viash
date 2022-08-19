package io.viash.platforms

import io.viash.helpers.IO
import io.viash.{DockerTest, NextFlowTest, TestHelper}
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

    val extraEnv_ = extraEnv :+ ( "NXF_VER" -> "21.04.1" )

    val exitCode = Process(command, cwd, extraEnv_ : _*).!(ProcessLogger(str => stdOut ++= s"$str\n", str => stdErr ++= s"$str\n"))

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
      mainScript = "workflows/pipeline1/main.nf",
      entry = Some("base"),
      args = List(
        "--input", "resources/*",
        "--publish_dir", "output",
      )
    )

    assert(exitCode == 0, s"\nexit code was $exitCode\nStd output:\n$stdOut\nStd error:\n$stdErr")
    outputFileMatchChecker(stdOut, "DEBUG6", "^11 .*$")
  }

  test("Run pipeline with components using map functionality", DockerTest, NextFlowTest) {

    val (exitCode, stdOut, stdErr) = runNextflowProcess(
      mainScript = "workflows/pipeline1/main.nf",
      entry = Some("map_variant"),
      args = List(
        "--input", "resources/*",
        "--publish_dir", "output",
      )
    )

    assert(exitCode == 0, s"\nexit code was $exitCode\nStd output:\n$stdOut\nStd error:\n$stdErr")
    outputFileMatchChecker(stdOut, "DEBUG4", "^11 .*$")
  }

  test("Run pipeline with components using mapData functionality", DockerTest, NextFlowTest) {

    val (exitCode, stdOut, stdErr) = runNextflowProcess(
      mainScript = "workflows/pipeline1/main.nf",
      entry = Some("mapData_variant"),
      args = List(
        "--input", "resources/*",
        "--publish_dir", "output",
      )
    )

    assert(exitCode == 0, s"\nexit code was $exitCode\nStd output:\n$stdOut\nStd error:\n$stdErr")
    outputFileMatchChecker(stdOut, "DEBUG4", "^11 .*$")
  }

  test("Run pipeline with debug = false", DockerTest, NextFlowTest) {

    val (exitCode, stdOut, stdErr) = runNextflowProcess(
      mainScript = "workflows/pipeline1/main.nf",
      entry = Some("debug_variant"),
      args = List(
        "--input", "resources/*",
        "--publish_dir", "output",
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
      mainScript = "workflows/pipeline1/main.nf",
      entry = Some("debug_variant"),
      args = List(
        "--input", "resources/*",
        "--publish_dir", "output",
        "--displayDebug", "true",
      )
    )

    assert(exitCode == 0, s"\nexit code was $exitCode\nStd output:\n$stdOut\nStd error:\n$stdErr")
    outputFileMatchChecker(stdOut, "DEBUG4", "^11 .*$")
    outputFileMatchChecker(stdOut, "process 'step3[^']*' output tuple", "^11 .*$")
  }

  test("Run legacy pipeline", DockerTest, NextFlowTest) {

    val (exitCode, stdOut, stdErr) = runNextflowProcess(
      mainScript = "workflows/pipeline2/main.nf",
      entry = Some("legacy_base"),
      args = List(
        "--input", "resources/*",
        "--publish_dir", "output",
      )
    )

    assert(exitCode == 0, s"\nexit code was $exitCode\nStd output:\n$stdOut\nStd error:\n$stdErr")
    outputFileMatchChecker(stdOut, "DEBUG6", "^11 .*$")
  }

  test("Run legacy and vdsl3 combined pipeline", DockerTest, NextFlowTest) {

    val (exitCode, stdOut, stdErr) = runNextflowProcess(
      mainScript = "workflows/pipeline2/main.nf",
      entry = Some("legacy_and_vdsl3"),
      args = List(
        "--input", "resources/*",
        "--publish_dir", "output",
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

  test("Run config pipeline", NextFlowTest) {

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

  test("Run config pipeline with yamlblob", NextFlowTest) {
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
    assert(debugPrints.find(_._1 == "foo").get._2("input").equals(resourcesPath+"/lines3.txt"))
  }

  test("Run config pipeline with yaml file", NextFlowTest) {
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
    assert(debugPrints.find(_._1 == "foo").get._2("input").equals(resourcesPath+"/lines3.txt"))
  }

  test("Run config pipeline with json file", NextFlowTest) {
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
    assert(debugPrints.find(_._1 == "foo").get._2("input").equals(resourcesPath+"/lines3.txt"))
  }

  test("Run config pipeline with csv file", NextFlowTest) {
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
    assert(debugPrints.find(_._1 == "foo").get._2("input").equals(resourcesPath+"/lines3.txt"))
  }

  test("Run config pipeline asis, default nextflow implementation", NextFlowTest) {
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
    assert(debugPrints.find(_._1 == "foo").get._2("input").equals(resourcesPath+"/lines3.txt"))
  }

  test("Run module as standalone", NextFlowTest) {
    val (exitCode, stdOut, stdErr) = runNextflowProcess(
      mainScript = "target/nextflowvdsl3/step2/main.nf",
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
      mainScript = "target/nextflowvdsl3/step2/main.nf",
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
      mainScript = "target/nextflowvdsl3/step2/main.nf",
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

  test("Check whether --help is same as Viash's --help", NextFlowTest) {
    // except that WorkflowHelper.nf will not print alternatives, and
    // will always prefix argument names with -- (so --foo, not -f or foo).

    // run WorkflowHelper's --help
    val (exitCode, stdOut1, stdErr1) = runNextflowProcess(
      mainScript = "workflows/utils/HelpViewer.nf",
      args = List(
        "--help",
        "--rootDir", tempFolStr,
        "--config", "workflows/pipeline3/config.vsh.yaml"
      ),
      quiet = true
    )

    assert(exitCode == 0, s"\nexit code was $exitCode\nStd output:\n$stdOut1\nStd error:\n$stdErr1")

    // explicitly remove defaults set by output files
    // these defaults make sense in nextflow but not in viash
    val correctedStdOut1 = stdOut1.replaceAll("        default: \\$id\\.\\$key\\.[^\n]*\n", "")
    // explicitly remove global arguments
    // these arguments make sense in nextflow but not in viash
    import java.util.regex.Pattern
    val regex = Pattern.compile("Nextflow input-output arguments:.*Arguments:", Pattern.DOTALL)
    val correctedStdOut2 = regex.matcher(correctedStdOut1).replaceAll("Arguments:")

    // run Viash's --help
    val (stdOut2, stdErr2) = TestHelper.testMainWithStdErr(
      "run", workflowsPath + "/pipeline3/config.vsh.yaml",
      "--", "--help"
    )

    // check if they are the same
    assert(correctedStdOut2 == stdOut2)
  }

  override def afterAll() {
    IO.deleteRecursively(temporaryFolder)
  }
}
