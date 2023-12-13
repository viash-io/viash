package io.viash.platforms.nextflow

import io.viash.helpers.{IO, Logger}
import io.viash.{DockerTest, NextflowTest, TestHelper}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite

import java.io.File
import java.nio.file.{Files, Path, Paths}
import scala.jdk.CollectionConverters._

import NextflowTestHelper._

class NextflowHelperFunctionTest extends AnyFunSuite with BeforeAndAfterAll {

  Logger.UseColorOverride.value = Some(false)
  // temporary folder to work in
  private val temporaryFolder = IO.makeTemp("nextflow_helper_function_test")

  // some paths
  private val testDir = temporaryFolder.resolve("test_resources")
  private val workflowHelper = temporaryFolder.resolve("WorkflowHelper.nf")

  // path to namespace components
  private val nextflowDir = Paths.get(getClass.getResource("/platforms/nextflow/").getPath)

  // copy resources to temporary folder so we can test in a clean environment
  IO.copyFolder(nextflowDir, testDir)
  IO.write(NextflowHelper.workflowHelper.toString, workflowHelper)
  
  // recursively search for files starting with 'test' and ending with '.nf' in 'functionDir: Path'
  // and return a list of paths to those files
  private val testFiles = Files.walk(testDir)
    .iterator
    .asScala
    .filter(Files.isRegularFile(_))
    .filter(_.getFileName.toString.matches("^test.*\\.nf$"))
  
  // run all tests
  for (testFile <- testFiles) {
    test(s"Testing ${testFile}", NextflowTest) {
      val (exitCode, stdOut, stdErr) = NextflowTestHelper.run(
        mainScript = testFile.toString,
        args = List(
          "--workflowHelper", workflowHelper.toString
        ),
        cwd = temporaryFolder.toFile()
      )

      assert(exitCode == 0, s"\nexit code was $exitCode\nStd output:\n$stdOut\nStd error:\n$stdErr")
    }
  }

  // delete temp files
  override def afterAll(): Unit = {
    IO.deleteRecursively(temporaryFolder)
  }
}
