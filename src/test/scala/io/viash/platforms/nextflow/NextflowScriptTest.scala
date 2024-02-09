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

import NextflowTestHelper._

/**
  * This test suite contains tests related to the NextflowScript class.
  * 
  * All workflows tested in this suite should be self-contained in that
  * they execute tests and do assertions within the same workflow. If
  * the test fails, the workflow should print a helpful error message and
  * exit with a non-zero exit code.
  */
class NextflowScriptTest extends AnyFunSuite with BeforeAndAfterAll {
  Logger.UseColorOverride.value = Some(false)
  // temporary folder to work in
  private val temporaryFolder = IO.makeTemp("viash_tester_nextflowvdsl3")
  private val tempFolStr = temporaryFolder.toString
  private val tempFolFile = temporaryFolder.toFile

  // path to namespace components
  private val rootPath = getClass.getResource("/testnextflowvdsl3/").getPath
  private val srcPath = Paths.get(tempFolStr, "src").toFile.toString
  private val targetPath = Paths.get(tempFolStr, "target").toFile.toString
  private val resourcesPath = Paths.get(tempFolStr, "resources").toFile.toString

  // copy resources to temporary folder so we can build in a clean environment
  for (resource <- List("src", "resources"))
    IO.copyFolder(
      Paths.get(rootPath, resource).toString,
      Paths.get(tempFolStr, resource).toString
    )

  test("Build pipeline components", DockerTest, NextflowTest) {
    // build the nextflow containers
    // TODO: use the correct CWD to build the pipeline to be ablke
    // to detect the correct path to the _viash.yaml file
    val (_, _, _) = TestHelper.testMainWithStdErr(
      "ns", "build",
      "-s", srcPath,
      "-t", targetPath,
      "--setup", "cb"
    )
  }

  test("Run config pipeline", NextflowTest) {
    val (exitCode, stdOut, stdErr) = NextflowTestHelper.run(
      mainScript = "target/nextflow/wf/main.nf",
      args = List(
        "--id", "foo",
        "--input1", "resources/lines*.txt",
        "--input2", "resources/lines3.txt",
        "--publish_dir", "output"
      ),
      cwd = tempFolFile
    )

    assert(exitCode == 0, s"\nexit code was $exitCode\nStd output:\n$stdOut\nStd error:\n$stdErr")
  }

  test("Run config pipeline with symlink", NextflowTest) {
    val newWorkflowPath = Paths.get(tempFolStr, "workflowsAsSymlink")
    Files.createDirectories(newWorkflowPath)
    val symlinkFolder = Paths.get(newWorkflowPath.toString(), "workflow")
    Files.createSymbolicLink(symlinkFolder, Paths.get(tempFolStr, "target/nextflow/wf/"))
    val (exitCode, stdOut, stdErr) = NextflowTestHelper.run(
      mainScript = "workflowsAsSymlink/workflow/main.nf",
      args = List(
        "--id", "foo",
        "--input1", "resources/lines*.txt",
        "--input2", "resources/lines3.txt",
        "--publish_dir", "output"
      ),
      cwd = tempFolFile
    )

    assert(exitCode == 0, s"\nexit code was $exitCode\nStd output:\n$stdOut\nStd error:\n$stdErr")
  }

  test("Run config pipeline with subworkflow dependency and symlinks", NextflowTest) {
    val newWorkflowPath = Paths.get(tempFolStr, "nestedWorkflowsAsSymlink")
    Files.createDirectories(newWorkflowPath)
    val symlinkFolder = Paths.get(newWorkflowPath.toString(), "workflow")
    Files.createSymbolicLink(symlinkFolder, Paths.get(tempFolStr, "target/nextflow/test_wfs/"))
    val (exitCode, stdOut, stdErr) = NextflowTestHelper.run(
      mainScript = "nestedWorkflowsAsSymlink/workflow/nested/main.nf",
      args = List(
        "--publish_dir", "output"
      ),
      cwd = tempFolFile
    )

    assert(exitCode == 0, s"\nexit code was $exitCode\nStd output:\n$stdOut\nStd error:\n$stdErr")
  }

  // TODO: use TestHelper.testMainWithStdErr instead of NextflowTestHelper.run; i.e. viash test
  test("Test workflow", DockerTest, NextflowTest) {
    val (exitCode, stdOut, stdErr) = NextflowTestHelper.run(
      mainScript = "target/nextflow/wf/main.nf",
      entry = Some("test_base"),
      args = List(
        "--rootDir", tempFolStr,
        "--publish_dir", "output"
      ),
      cwd = tempFolFile
    )

    assert(exitCode == 0, s"\nexit code was $exitCode\nStd output:\n$stdOut\nStd error:\n$stdErr")
  }
  
  test("Test fromState/toState", DockerTest, NextflowTest) {
    val (exitCode, stdOut, stdErr) = NextflowTestHelper.run(
      mainScript = "target/nextflow/test_wfs/fromstate_tostate/main.nf",
      args = List(
        "--publish_dir", "output"
      ),
      cwd = tempFolFile
    )

    assert(exitCode == 0, s"\nexit code was $exitCode\nStd output:\n$stdOut\nStd error:\n$stdErr")
  }

  test("Test filter/runIf", DockerTest, NextflowTest) {
    val (exitCode, stdOut, stdErr) = NextflowTestHelper.run(
      mainScript = "target/nextflow/test_wfs/filter_runif/main.nf",
      args = List(
        "--publish_dir", "output"
      ),
      cwd = tempFolFile
    )

    assert(exitCode == 0, s"\nexit code was $exitCode\nStd output:\n$stdOut\nStd error:\n$stdErr")
  }

  test("Test whether aliasing works", DockerTest, NextflowTest) {
    val (exitCode, stdOut, stdErr) = NextflowTestHelper.run(
      mainScript = "target/nextflow/test_wfs/alias/main.nf",
      args = List(
        "--publish_dir", "output"
      ),
      cwd = tempFolFile
    )

    assert(exitCode == 0, s"\nexit code was $exitCode\nStd output:\n$stdOut\nStd error:\n$stdErr")
    assert(stdOut.contains("base:step1_alias:proc"))
    assert(stdOut.contains("base:step1:proc"))
    assert(!stdOut.contains("Key for module 'step1' is duplicated"))
  }

  test("Test for concurrency issues", DockerTest, NextflowTest) {
    val (exitCode, stdOut, stdErr) = NextflowTestHelper.run(
      mainScript = "target/nextflow/test_wfs/concurrency/main.nf",
      args = List(
        "--publish_dir", "output"
      ),
      cwd = tempFolFile
    )

    assert(exitCode == 0, s"\nexit code was $exitCode\nStd output:\n$stdOut\nStd error:\n$stdErr")
  }

  test("Test runEach", DockerTest, NextflowTest) {
    val (exitCode, stdOut, stdErr) = NextflowTestHelper.run(
      mainScript = "target/nextflow/test_wfs/runeach/main.nf",
      args = List(
        "--publish_dir", "output"
      ),
      cwd = tempFolFile
    )

    assert(exitCode == 0, s"\nexit code was $exitCode\nStd output:\n$stdOut\nStd error:\n$stdErr")
  }


  test("Test nested workflows", DockerTest, NextflowTest) {
    val (exitCode, stdOut, stdErr) = NextflowTestHelper.run(
      mainScript = "target/nextflow/test_wfs/nested/main.nf",
      args = List(
        "--publish_dir", "output"
      ),
      cwd = tempFolFile
    )

    assert(exitCode == 0, s"\nexit code was $exitCode\nStd output:\n$stdOut\nStd error:\n$stdErr")
  } 

  test("Check whether --help is same as Viash's --help", NextflowTest) {
    // except that WorkflowHelper.nf will not print alternatives, and
    // will always prefix argument names with -- (so --foo, not -f or foo).

    // run WorkflowHelper's --help
    val (exitCode, stdOut1, stdErr1) = NextflowTestHelper.run(
      mainScript = "target/nextflow/wf/main.nf",
      args = List("--help"),
      quiet = true,
      cwd = tempFolFile
    )

    assert(exitCode == 0, s"\nexit code was $exitCode\nStd output:\n$stdOut1\nStd error:\n$stdErr1")
  }

  override def afterAll(): Unit = {
    IO.deleteRecursively(temporaryFolder)
  }
}
