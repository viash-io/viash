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

  test("Check namespace test output") {
    val testText = TestHelper.testMain(
      Array(
        "ns", "test",
        "--src", nsPath
      ))

    val components = List(
      "ns_add",
      "ns_subtract",
      "ns_multiply",
      "ns_divide"
    )

    val steps = List(
      ("start",""),
      ("build_executable","\\s*0\\s*\\d+\\s*SUCCESS"),
      ("test\\.sh","\\s*0\\s*\\d+\\s*SUCCESS")
    )

    // Test all 'normal' steps for components
    for (component ← components) {
      for ((step, resultPattern) ← steps) {
        val regex = s"""testns\\s*$component\\s*native\\s*$step$resultPattern""".r
        assert(regex.findFirstIn(testText).isDefined, s"\nRegex: '${regex.toString}'; text: \n$testText")
      }
    }

    // Check for the one failing test of ns_divide
    val regexFail = s"""testns\\s*ns_divide\\s*native\\s*test_div0\\.sh\\s*1\\s*\\d+\\s*ERROR""".r
    assert(regexFail.findFirstIn(testText).isDefined, s"\nRegex: '${regexFail.toString}'; text: \n$testText")
  }

  test("Check namespace test output with tsv option") {
    val log = Paths.get(tempFolStr, "log.tsv").toFile

    val testText = TestHelper.testMain(
      Array(
        "ns", "test",
        "--tsv", log.toString,
        "--src", nsPath
      ))

    val components = List(
      "ns_add",
      "ns_subtract",
      "ns_multiply",
      "ns_divide"
    )

    val steps = List(
      ("build_executable","\\t0\\t\\d+\\tSUCCESS"),
      ("test\\.sh","\\t0\\t\\d+\\tSUCCESS")
    )

    val logSrc = Source.fromFile(log)
    try {
      val logLines = logSrc.mkString

      // Test inclusion of a header
      val regexHeader = "^namespace\\tfunctionality\\tplatform\\ttest_name\\texit_code\\tduration\\tresult".r
      assert(regexHeader.findFirstIn(logLines).isDefined, s"\nRegex: ${regexHeader.toString}; text: \n$logLines")

      // Test all 'normal' steps for components
      for (component ← components) {
        for ((step, resultPattern) ← steps) {
          val regex = s"""testns\\t$component\\tnative\\t$step$resultPattern""".r
          assert(regex.findFirstIn(logLines).isDefined, s"\nRegex: '${regex.toString}'; text: \n$logLines")
        }
      }

      // Check for the one failing test of ns_divide
      val regexFail = s"""testns\\tns_divide\\tnative\\ttest_div0\\.sh\\t1\\t\\d+\\tERROR""".r
      assert(regexFail.findFirstIn(logLines).isDefined, s"\nRegex: '${regexFail.toString}'; text: \n$logLines")
    } finally {
      logSrc.close()
    }
  }

  test("Check namespace test output with tsv and append options") {
    val log = Paths.get(tempFolStr, "log_append.tsv").toFile

    val fileHeader = "Test header" + sys.props("line.separator")
    Files.write(log.toPath, fileHeader.getBytes(StandardCharsets.UTF_8))

    val testText = TestHelper.testMain(
      Array(
        "ns", "test",
        "--tsv", log.toString,
        "--append",
        "--src", nsPath
      ))

    val components = List(
      "ns_add",
      "ns_subtract",
      "ns_multiply",
      "ns_divide"
    )

    val steps = List(
      ("build_executable","\\t0\\t\\d+\\tSUCCESS"),
      ("test\\.sh","\\t0\\t\\d+\\tSUCCESS")
    )

    val logSrc = Source.fromFile(log)
    try {
      val logLines = logSrc.mkString

      // Test inclusion of a header
      val regexHeader = "namespace\\tfunctionality\\tplatform\\ttest_name\\texit_code\\tduration\\tresult".r
      assert(!regexHeader.findFirstIn(logLines).isDefined, s"\nRegex: ${regexHeader.toString}; text: \n$logLines")
      val regexHeader2 = "^Test header".r
      assert(regexHeader2.findFirstIn(logLines).isDefined, s"\rRegex: ${regexHeader2.toString}; text: \r$logLines")

      // Test all 'normal' steps for components
      for (component ← components) {
        for ((step, resultPattern) ← steps) {
          val regex = s"""testns\\t$component\\tnative\\t$step$resultPattern""".r
          assert(regex.findFirstIn(logLines).isDefined, s"\nRegex: '${regex.toString}'; text: \n$logLines")
        }
      }

      // Check for the one failing test of ns_divide
      val regexFail = s"""testns\\tns_divide\\tnative\\ttest_div0\\.sh\\t1\\t\\d+\\tERROR""".r
      assert(regexFail.findFirstIn(logLines).isDefined, s"\nRegex: '${regexFail.toString}'; text: \n$logLines")
    } finally {
      logSrc.close()
    }
  }

  override def afterAll() {
    IO.deleteRecursively(temporaryFolder)
  }
}
