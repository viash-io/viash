package com.dataintuitive.viash

import org.scalatest.{BeforeAndAfterAll, FunSuite}

class MainNSTestNativeSuite extends FunSuite with BeforeAndAfterAll {
  // path to namespace components
  private val nsPath = getClass.getResource("/testns/").getPath

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
      ("build_executable","\\s*0\\s*0\\s*SUCCESS"),
      ("test\\.sh","\\s*0\\s*0\\s*SUCCESS")
    )

    // Test all 'normal' steps for components
    for (component ← components) {
      for ((step, resultPattern) ← steps) {
        val regex = s"""testns\\s*$component\\s*native\\s*$step$resultPattern""".r
        assert(regex.findFirstIn(testText).isDefined, s"-->${regex.toString}<-->$testText<--")
      }
    }

    // Check for the one failing test of ns_divide
    val regexFail = s"""testns\\s*ns_divide\\s*native\\s*test_div0\\.sh\\s*1\\s*0\\s*ERROR""".r
    assert(regexFail.findFirstIn(testText).isDefined, s"-->${regexFail.toString}<-->$testText<--")
  }

}
