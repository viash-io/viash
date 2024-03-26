package io.viash.e2e.ns_test

import io.viash._

import io.viash.helpers.{IO, Logger}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, OpenOption, Paths}
import scala.io.Source

class MainNSTestNativeSuite extends AnyFunSuite with BeforeAndAfterAll {
  Logger.UseColorOverride.value = Some(false)
  // path to namespace components
  private val nsPath = getClass.getResource("/testns/").getPath

  private val temporaryFolder = IO.makeTemp("viash_ns_test_tsv")
  private val tempFolStr = temporaryFolder.toString

  private val stepsSuccess = List(
    ("start", ""),
    // ("build_executable", raw"\s*0\s*\d+\s*SUCCESS"),
    (raw"test\.sh", raw"\s*0\s*\d+\s*SUCCESS")
  )

  private val stepsFailure = List(
    ("start", ""),
    // ("build_executable", raw"\s*0\s*\d+\s*SUCCESS"),
    (raw"test\.sh", raw"\s*0\s*\d+\s*SUCCESS"),
    (raw"test_div0\.sh", raw"\s*1\s*\d+\s*ERROR"),
  )

  private val stepsMissing = List(
    ("start", ""),
    //("build_executable", raw"\s*0\s*\d+\s*SUCCESS"),
    ("tests", raw"\s*-1\s*\d+\s*MISSING")
  )

  private val components = List(
    ("ns_add", stepsSuccess),
    ("ns_subtract", stepsSuccess),
    ("ns_multiply", stepsSuccess),
    ("ns_divide", stepsFailure),
    ("ns_power", stepsMissing)
  )


  test("Check namespace test output without working dir message") {
    val testOutput = TestHelper.testMain(
      "ns", "test",
      "--src", nsPath,
      "--keep", "false"
    )

    // Test inclusion of a header
    val regexHeader = s"^\\s*${ViashNamespace.columnHeaders.mkString("\\s*")}".r
    assert(regexHeader.findFirstIn(testOutput.stdout).isDefined, s"\nRegex: ${regexHeader.toString}; text: \n${testOutput.stdout}")

    for (
      (component, steps) <- components;
      (step, resultPattern) <- steps
    ) {
      val regex = s"""testns\\s*$component\\s*executable\\s*native\\s*$step$resultPattern""".r
      assert(regex.findFirstIn(testOutput.stdout).isDefined, s"\nRegex: '${regex.toString}'; text: \n${testOutput.stdout}")
    }

    val regexBuildError = raw"Reading file \'.*/src/ns_error/config\.vsh\.yaml\' failed".r
    assert(regexBuildError.findFirstIn(testOutput.stderr).isDefined, "Expecting to get an error because of an invalid yaml in ns_error")

    assert(testOutput.stderr.contains("The status of the component 'ns_power' is set to deprecated."))
  }

  test("Check namespace test output with working dir message") {
    val testOutput = TestHelper.testMain(
      "ns", "test",
      "--src", nsPath,
      "--keep", "true"
    )

    // Test inclusion of a header
    val regexHeader = s"^\\s*${ViashNamespace.columnHeaders.mkString("\\s*")}".r
    assert(regexHeader.findFirstIn(testOutput.stdout).isDefined, s"\nRegex: ${regexHeader.toString}; text: \n${testOutput.stdout}")

    val regexWdir = raw"The working directory for the namespace tests is [\w/]+[\r\n]{1,2}".r
    assert(regexWdir.findFirstIn(testOutput.stderr).isDefined, s"\nRegex: ${regexHeader.toString}; text: \n${testOutput.stderr}")

    for (
      (component, steps) <- components;
      (step, resultPattern) <- steps
    ) {
      val regex = s"""testns\\s*$component\\s*executable\\s*native\\s*$step$resultPattern""".r
      assert(regex.findFirstIn(testOutput.stdout).isDefined, s"\nRegex: '${regex.toString}'; text: \n${testOutput.stdout}")
    }

    val regexBuildError = raw"Reading file \'.*/src/ns_error/config\.vsh\.yaml\' failed".r
    assert(regexBuildError.findFirstIn(testOutput.stderr).isDefined, "Expecting to get an error because of an invalid yaml in ns_error")
  }
  
  test("Check namespace test output with working dir message using --just_generate") {
    val testOutput = TestHelper.testMain(
      "ns", "test",
      "--src", nsPath,
      "--keep", "true",
      "--just_generate"
    )

    // Test inclusion of a header
    val regexHeader = s"^\\s*${ViashNamespace.columnHeaders.mkString("\\s*")}".r
    assert(regexHeader.findFirstIn(testOutput.stdout).isDefined, s"\nRegex: ${regexHeader.toString}; text: \n${testOutput.stdout}")

    val regexWdir = raw"The working directory for the namespace tests is [\w/]+[\r\n]{1,2}".r
    assert(regexWdir.findFirstIn(testOutput.stderr).isDefined, s"\nRegex: ${regexHeader.toString}; text: \n${testOutput.stderr}")

    for (
      (component, steps) <- components;
      (step, resultPattern) <- steps
    ) {
      val regex = s"""testns\\s*$component\\s*executable\\s*native\\s*$step$resultPattern""".r
      assert(regex.findFirstIn(testOutput.stdout).isDefined, s"\nRegex: '${regex.toString}'; text: \n${testOutput.stdout}")
    }

    val regexBuildError = raw"Reading file \'.*/src/ns_error/config\.vsh\.yaml\' failed".r
    assert(regexBuildError.findFirstIn(testOutput.stderr).isDefined, "Expecting to get an error because of an invalid yaml in ns_error")
  }

  test("Check namespace test output with tsv option") {
    val log = Paths.get(tempFolStr, "log.tsv").toFile

    val testOutput = TestHelper.testMain(
      "ns", "test",
      "--tsv", log.toString,
      "--src", nsPath
    )

    val logSrc = Source.fromFile(log)
    try {
      val logLines = logSrc.mkString

      // Test inclusion of a header
      val regexHeader = s"^${ViashNamespace.columnHeaders.mkString("\\t")}".r
      assert(regexHeader.findFirstIn(logLines).isDefined, s"\nRegex: ${regexHeader.toString}; text: \n$logLines")

      for (
        (component, steps) <- components;
        (step, resultPattern) <- steps if step != "start"
      ) {
        // tsv doesn't output the "start" step, so ignore that
        val regex = s"""testns\\t$component\\texecutable\\tnative\\t$step$resultPattern""".r
        assert(regex.findFirstIn(logLines).isDefined, s"\nRegex: '${regex.toString}'; text: \n$logLines")
      }
    } finally {
      logSrc.close()
    }
  }

  test("Check namespace test output with tsv and append options") {
    val log = Paths.get(tempFolStr, "log_append.tsv").toFile

    val fileHeader = "Test header" + sys.props("line.separator")
    Files.write(log.toPath, fileHeader.getBytes(StandardCharsets.UTF_8))

    TestHelper.testMain(
      "ns", "test",
      "--tsv", log.toString,
      "--append",
      "--src", nsPath
    )

    val logSrc = Source.fromFile(log)
    try {
      val logLines = logSrc.mkString

      // Test inclusion of a header, header should not be present
      val regexHeader = s"${ViashNamespace.columnHeaders.mkString("\\t")}".r
      assert(!regexHeader.findFirstIn(logLines).isDefined, s"\nRegex: ${regexHeader.toString}; text: \n$logLines")
      val regexHeader2 = "^Test header".r
      assert(regexHeader2.findFirstIn(logLines).isDefined, s"\rRegex: ${regexHeader2.toString}; text: \r$logLines")

      for (
        (component, steps) <- components;
        (step, resultPattern) <- steps if step != "start"
      ) {
        // tsv doesn't output the "start" step, so ignore that
        val regex = s"""testns\\t$component\\texecutable\\tnative\\t$step$resultPattern""".r
        assert(regex.findFirstIn(logLines).isDefined, s"\nRegex: '${regex.toString}'; text: \n$logLines")
      }
    } finally {
      logSrc.close()
    }
  }

  test("Check namespace test output with tsv and append options without the output file exists") {
    val log = Paths.get(tempFolStr, "log_append_new.tsv").toFile

    TestHelper.testMain(
      "ns", "test",
      "--tsv", log.toString,
      "--append",
      "--src", nsPath
    )

    val logSrc = Source.fromFile(log)
    try {
      val logLines = logSrc.mkString

      // Test inclusion of a header, header *should* be added if the file didn't exist yet
      val regexHeader = s"${ViashNamespace.columnHeaders.mkString("\\t")}".r
      assert(regexHeader.findFirstIn(logLines).isDefined, s"\nRegex: ${regexHeader.toString}; text: \n$logLines")

      for (
        (component, steps) <- components;
        (step, resultPattern) <- steps if step != "start"
      ) {
        // tsv doesn't output the "start" step, so ignore that
        val regex = s"""testns\\t$component\\texecutable\\tnative\\t$step$resultPattern""".r
        assert(regex.findFirstIn(logLines).isDefined, s"\nRegex: '${regex.toString}'; text: \n$logLines")
      }
    } finally {
      logSrc.close()
    }
  }

  override def afterAll(): Unit = {
    IO.deleteRecursively(temporaryFolder)
  }
}
