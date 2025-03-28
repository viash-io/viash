package io.viash.runners.nextflow

import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite

import java.io.File
import java.nio.file.{Files, Path, Paths}
import java.io.IOException
import java.io.UncheckedIOException

import scala.io.Source

import io.viash.helpers.{IO, Logger}
import io.viash.{DockerTest, NextflowTest, TestHelper}

import NextflowTestHelper._

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

  // copy resources to temporary folder so we can build in a clean environment
  for (resource <- List("src", "workflows", "resources"))
    IO.copyFolder(Paths.get(rootPath, resource).toString, Paths.get(tempFolStr, resource).toString)

  test("Build pipeline components", DockerTest, NextflowTest) {
    // build the nextflow containers
    TestHelper.testMain(
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


  override def afterAll(): Unit = {
    IO.deleteRecursively(temporaryFolder)
  }
}
