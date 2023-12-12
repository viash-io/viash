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
  private val tempFolStr = temporaryFolder.toString
  private val tempFolFile = temporaryFolder.toFile

  // path to namespace components
  private val nextflowDir = getClass.getResource("/platforms/nextflow/").getPath
  private val functionDir = temporaryFolder.resolve("functions")
  private val workflowHelper = temporaryFolder.resolve("WorkflowHelper.nf")

  // copy resources to temporary folder so we can test in a clean environment
  IO.copyFolder(
    nextflowDir.toString,
    tempFolStr
  )
  IO.write(NextflowHelper.workflowHelper.toString, workflowHelper)
  
  // recursively search for files starting with 'test' and ending with '.nf' in 'functionDir: Path'
  // and return a list of paths to those files
  private val testFiles = Files.walk(functionDir)
    .iterator
    .asScala
    .filter(Files.isRegularFile(_))
    .filter(_.getFileName.toString.matches("^test.*\\.nf$"))

  // run all tests
  for (testFile <- testFiles) {
    test(s"Test ${testFile.getFileName.toString}", NextflowTest) {
      val (exitCode, stdOut, stdErr) = NextflowTestHelper.run(
        mainScript = testFile.toString,
        args = List(
          "--workflowHelper", workflowHelper.toString
        ),
        cwd = tempFolFile
      )

      assert(exitCode == 0, s"\nexit code was $exitCode\nStd output:\n$stdOut\nStd error:\n$stdErr")
    }
  }

  // delete temp files
  override def afterAll(): Unit = {
    IO.deleteRecursively(temporaryFolder)
  }
}
