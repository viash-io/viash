package com.dataintuitive.viash

import com.dataintuitive.viash.helpers.IO
import org.scalatest.{BeforeAndAfterAll, FunSuite}

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, OpenOption, Paths}
import scala.io.Source

class MainNSTestNativeSuite extends FunSuite with BeforeAndAfterAll {
  // path to namespace components
  private val nsPath = getClass.getResource("/testns/").getPath

  private val temporaryFolder = IO.makeTemp("viash_ns_test_tsv")
  private val tempFolStr = temporaryFolder.toString

  // TODO: check 'build_executable' output when a docker component setup fails.

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


  test("Check namespace test output") {
    val testText = TestHelper.testMain(
      "ns", "test",
      "--src", nsPath
    )

    // Test inclusion of a header
    val regexHeader = raw"^\s*namespace\s*functionality\s*platform\s*test_name\s*exit_code\s*duration\s*result".r
    assert(regexHeader.findFirstIn(testText).isDefined, s"\nRegex: ${regexHeader.toString}; text: \n$testText")

    for ((component, steps) ← components) {
      for ((step, resultPattern) ← steps) {
        val regex = s"""testns\\s*$component\\s*native\\s*$step$resultPattern""".r
        assert(regex.findFirstIn(testText).isDefined, s"\nRegex: '${regex.toString}'; text: \n$testText")
      }
    }
  }

  test("Check namespace test output with tsv option") {
    val log = Paths.get(tempFolStr, "log.tsv").toFile

    val testText = TestHelper.testMain(
      "ns", "test",
      "--tsv", log.toString,
      "--src", nsPath
    )

    val logSrc = Source.fromFile(log)
    try {
      val logLines = logSrc.mkString

      // Test inclusion of a header
      val regexHeader = raw"^namespace\tfunctionality\tplatform\ttest_name\texit_code\tduration\tresult".r
      assert(regexHeader.findFirstIn(logLines).isDefined, s"\nRegex: ${regexHeader.toString}; text: \n$logLines")

      for ((component, steps) ← components) {
        for ((step, resultPattern) ← steps) {
          // tsv doesn't output the "start" step, so ignore that
          if (step != "start") {
            val regex = s"""testns\\t$component\\tnative\\t$step$resultPattern""".r
            assert(regex.findFirstIn(logLines).isDefined, s"\nRegex: '${regex.toString}'; text: \n$logLines")
          }
        }
      }
    } finally {
      logSrc.close()
    }
  }

  test("Check namespace test output with tsv and append options") {
    val log = Paths.get(tempFolStr, "log_append.tsv").toFile

    val fileHeader = "Test header" + sys.props("line.separator")
    Files.write(log.toPath, fileHeader.getBytes(StandardCharsets.UTF_8))

    val testText = TestHelper.testMain(
      "ns", "test",
      "--tsv", log.toString,
      "--append",
      "--src", nsPath
    )

    val logSrc = Source.fromFile(log)
    try {
      val logLines = logSrc.mkString

      // Test inclusion of a header, header should not be present
      val regexHeader = raw"namespace\tfunctionality\tplatform\ttest_name\texit_code\tduration\tresult".r
      assert(!regexHeader.findFirstIn(logLines).isDefined, s"\nRegex: ${regexHeader.toString}; text: \n$logLines")
      val regexHeader2 = "^Test header".r
      assert(regexHeader2.findFirstIn(logLines).isDefined, s"\rRegex: ${regexHeader2.toString}; text: \r$logLines")

      for ((component, steps) ← components) {
        for ((step, resultPattern) ← steps) {
          // tsv doesn't output the "start" step, so ignore that
          if (step != "start") {
            val regex = s"""testns\\t$component\\tnative\\t$step$resultPattern""".r
            assert(regex.findFirstIn(logLines).isDefined, s"\nRegex: '${regex.toString}'; text: \n$logLines")
          }
        }
      }
    } finally {
      logSrc.close()
    }
  }

  test("Check namespace test output with tsv and append options without the output file exists") {
    val log = Paths.get(tempFolStr, "log_append_new.tsv").toFile

    val testText = TestHelper.testMain(
      "ns", "test",
      "--tsv", log.toString,
      "--append",
      "--src", nsPath
    )

    val logSrc = Source.fromFile(log)
    try {
      val logLines = logSrc.mkString

      // Test inclusion of a header, header *should* be added if the file didn't exist yet
      val regexHeader = raw"namespace\tfunctionality\tplatform\ttest_name\texit_code\tduration\tresult".r
      assert(regexHeader.findFirstIn(logLines).isDefined, s"\nRegex: ${regexHeader.toString}; text: \n$logLines")

      for ((component, steps) ← components) {
        for ((step, resultPattern) ← steps) {
          // tsv doesn't output the "start" step, so ignore that
          if (step != "start") {
            val regex = s"""testns\\t$component\\tnative\\t$step$resultPattern""".r
            assert(regex.findFirstIn(logLines).isDefined, s"\nRegex: '${regex.toString}'; text: \n$logLines")
          }
        }
      }
    } finally {
      logSrc.close()
    }
  }

  override def afterAll() {
    IO.deleteRecursively(temporaryFolder)
  }
}
