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

    // TODO: add back checks?

    // outputFileMatchChecker(stdOut, "DEBUG6", "^11 .*$")

    // // check whether step3's debug printing was triggered
    // outputFileMatchChecker(stdOut, "process 'step3[^']*' output tuple", "^11 .*$")

    // // check whether step2's debug printing was not triggered
    // val lines2 = stdOut.split("\n").find(_.contains("process 'step2' output tuple"))
    // assert(!lines2.isDefined)
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

    // // explicitly remove defaults set by output files
    // // these defaults make sense in nextflow but not in viash
    // val correctedStdOut1 = stdOut1.replaceAll("        default: \\$id\\.\\$key\\.[^\n]*\n", "")
    // // explicitly remove global arguments
    // // these arguments make sense in nextflow but not in viash
    // import java.util.regex.Pattern
    // val regex = Pattern.compile("\nNextflow input-output arguments:.*", Pattern.DOTALL)
    // val correctedStdOut2 = regex.matcher(correctedStdOut1).replaceAll("")

    // // run Viash's --help
    // val (stdOut2, stdErr2, exitCode2) = TestHelper.testMainWithStdErr(
    //   "run", srcPath + "/wf/config.vsh.yaml",
    //   "--", "--help"
    // )

    // assert(exitCode2 == 0)

    // // check if they are the same
    // assert(correctedStdOut2 == stdOut2)
  }


  override def afterAll(): Unit = {
    IO.deleteRecursively(temporaryFolder)
  }
}
