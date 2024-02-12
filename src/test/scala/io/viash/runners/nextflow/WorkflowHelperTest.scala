package io.viash.runners.nextflow

import io.viash.helpers.{IO, Logger}
import io.viash.{DockerTest, NextflowTest, TestHelper}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite

import java.io.File
import java.nio.file.{Files, Path, Paths}
import scala.io.Source

import java.io.IOException
import java.io.UncheckedIOException

import NextflowTestHelper._

class WorkflowHelperTest extends AnyFunSuite with BeforeAndAfterAll {
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

  // copy resources to temporary folder so we can build in a clean environment
  for (resource <- List("src", "workflows", "resources"))
    IO.copyFolder(
      Paths.get(rootPath, resource).toString,
      Paths.get(tempFolStr, resource).toString
    )

  test("Build pipeline components", DockerTest, NextflowTest) {
    // build the nextflow containers
    TestHelper.testMain(
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

    val (exitCode, stdOut, stdErr) = NextflowTestHelper.run(
      mainScript = "workflows/pipeline3/main.nf",
      entry = Some("base"),
      args = List(
        "--id", "foo",
        "--input", "resources/lines3.txt",
        "--real_number", "10.5",
        "--whole_number", "3",
        "--str", "foo",
        "--optional_with_default", "foo",
        "--multiple", "a;b;c",
        "--publish_dir", "output"
      ),
      cwd = tempFolFile
    )

    assert(
      exitCode == 0,
      s"\nexit code was $exitCode\nStd output;\n$stdOut\nStd error;\n$stdErr"
    )

    val debugPrints = outputTupleProcessor(stdOut, "DEBUG")
    checkDebugArgs("foo", debugPrints, expectedFoo)
  }

  test("Run config pipeline with yamlblob", NextflowTest) {
    val fooArgs =
      "{id: foo, input: resources/lines3.txt, whole_number: 3, optional_with_default: foo, multiple: [a, b, c]}"
    val barArgs =
      "{id: bar, input: resources/lines5.txt, real_number: 0.5, optional: bar, reality: true}"

    val (exitCode, stdOut, stdErr) = NextflowTestHelper.run(
      mainScript = "workflows/pipeline3/main.nf",
      entry = Some("base"),
      args = List(
        "--param_list", s"[$fooArgs, $barArgs]",
        "--real_number", "10.5",
        "--whole_number", "10",
        "--str", "foo",
        "--publish_dir", "output",
      ),
      cwd = tempFolFile
    )

    assert(
      exitCode == 0,
      s"\nexit code was $exitCode\nStd output;\n$stdOut\nStd error;\n$stdErr"
    )
    
    val debugPrints = outputTupleProcessor(stdOut, "DEBUG")
    checkDebugArgs("foo", debugPrints, expectedFoo)
    checkDebugArgs("bar", debugPrints, expectedBar)
    // Check location of resource file, vdsl3 makes it relative to the param_list file, yamlblob or asis can't do that so there it must be relative to the workflow
    assert(
      debugPrints
        .find(_._1 == "foo")
        .get
        ._2("input")
        .endsWith(resourcesPath + "/lines3.txt")
    )
  }

  test("Run config pipeline with yaml file", NextflowTest) {
    val param_list_file =
      Paths.get(resourcesPath, "pipeline3.yaml").toFile.toString
    val (exitCode, stdOut, stdErr) = NextflowTestHelper.run(
      mainScript = "workflows/pipeline3/main.nf",
      entry = Some("base"),
      args = List(
        "--param_list", param_list_file,
        "--real_number", "10.5",
        "--whole_number", "10",
        "--str", "foo",
        "--publish_dir", "output",
      ),
      cwd = tempFolFile
    )

    assert(
      exitCode == 0,
      s"\nexit code was $exitCode\nStd output;\n$stdOut\nStd error;\n$stdErr"
    )
    
    val debugPrints = outputTupleProcessor(stdOut, "DEBUG")
    checkDebugArgs("foo", debugPrints, expectedFoo)
    checkDebugArgs("bar", debugPrints, expectedBar)
    // Check location of resource file, vdsl3 makes it relative to the param_list file, yamlblob or asis can't do that so there it must be relative to the workflow
    assert(
      debugPrints
        .find(_._1 == "foo")
        .get
        ._2("input")
        .endsWith(resourcesPath + "/lines3.txt")
    )
  }

  test(
    "Run config pipeline with yaml file passed as a relative path",
    NextflowTest
  ) {
    val (exitCode, stdOut, stdErr) = NextflowTestHelper.run(
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

    assert(
      exitCode == 0,
      s"\nexit code was $exitCode\nStd output;\n$stdOut\nStd error;\n$stdErr"
    )
    val debugPrints = outputTupleProcessor(stdOut, "DEBUG")
    checkDebugArgs("foo", debugPrints, expectedFoo)
    checkDebugArgs("bar", debugPrints, expectedBar)
    // Check location of resource file, vdsl3 makes it relative to the param_list file, yamlblob or asis can't do that so there it must be relative to the workflow
    assert(
      debugPrints
        .find(_._1 == "foo")
        .get
        ._2("input")
        .endsWith(resourcesPath + "/lines3.txt")
    )
  }

  test("Run config pipeline with json file", NextflowTest) {
    val param_list_file =
      Paths.get(resourcesPath, "pipeline3.json").toFile.toString
    val (exitCode, stdOut, stdErr) = NextflowTestHelper.run(
      mainScript = "workflows/pipeline3/main.nf",
      entry = Some("base"),
      args = List(
        "--param_list", param_list_file,
        "--real_number", "10.5",
        "--whole_number", "10",
        "--str", "foo",
        "--publish_dir", "output"
      ),
      cwd = tempFolFile
    )

    assert(
      exitCode == 0,
      s"\nexit code was $exitCode\nStd output;\n$stdOut\nStd error;\n$stdErr"
    )
    
    val debugPrints = outputTupleProcessor(stdOut, "DEBUG")
    checkDebugArgs("foo", debugPrints, expectedFoo)
    checkDebugArgs("bar", debugPrints, expectedBar)
    // Check location of resource file, vdsl3 makes it relative to the param_list file, yamlblob or asis can't do that so there it must be relative to the workflow
    assert(
      debugPrints
        .find(_._1 == "foo")
        .get
        ._2("input")
        .endsWith(resourcesPath + "/lines3.txt")
    )
  }

  test("Run config pipeline with csv file", NextflowTest) {
    val param_list_file =
      Paths.get(resourcesPath, "pipeline3.csv").toFile.toString
    val (exitCode, stdOut, stdErr) = NextflowTestHelper.run(
      mainScript = "workflows/pipeline3/main.nf",
      entry = Some("base"),
      args = List(
        "--param_list", param_list_file,
        "--real_number", "10.5",
        "--whole_number", "10",
        "--str", "foo",
        "--publish_dir", "output"
      ),
      cwd = tempFolFile
    )

    assert(
      exitCode == 0,
      s"\nexit code was $exitCode\nStd output;\n$stdOut\nStd error;\n$stdErr"
    )
    
    val debugPrints = outputTupleProcessor(stdOut, "DEBUG")
    checkDebugArgs("foo", debugPrints, expectedFoo)
    checkDebugArgs("bar", debugPrints, expectedBar)
    // Check location of resource file, vdsl3 makes it relative to the param_list file, yamlblob or asis can't do that so there it must be relative to the workflow
    assert(
      debugPrints
        .find(_._1 == "foo")
        .get
        ._2("input")
        .endsWith(resourcesPath + "/lines3.txt")
    )
  }

  test(
    "Run config pipeline asis, default nextflow implementation",
    NextflowTest
  ) {
    val param_list_file =
      Paths.get(resourcesPath, "pipeline3.asis.yaml").toFile.toString
    val (exitCode, stdOut, stdErr) = NextflowTestHelper.run(
      mainScript = "workflows/pipeline3/main.nf",
      entry = Some("base"),
      paramsFile = Some(param_list_file),
      args = List(
        "--real_number", "10.5",
        "--whole_number", "10",
        "--str", "foo",
        "--publish_dir", "output",
      ),
      cwd = tempFolFile
    )

    assert(
      exitCode == 0,
      s"\nexit code was $exitCode\nStd output;\n$stdOut\nStd error;\n$stdErr"
    )
    
    val debugPrints = outputTupleProcessor(stdOut, "DEBUG")
    checkDebugArgs("foo", debugPrints, expectedFoo)
    checkDebugArgs("bar", debugPrints, expectedBar)
    // Check location of resource file, vdsl3 makes it relative to the param_list file, yamlblob or asis can't do that so there it must be relative to the workflow
    assert(
      debugPrints
        .find(_._1 == "foo")
        .get
        ._2("input")
        .endsWith(resourcesPath + "/lines3.txt")
    )
  }

  override def afterAll(): Unit = {
    IO.deleteRecursively(temporaryFolder)
  }
}
