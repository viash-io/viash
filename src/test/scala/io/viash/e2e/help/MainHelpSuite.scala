package io.viash.e2e.help

import io.viash._

import org.scalatest.funsuite.AnyFunSuite
import io.viash.exceptions.ExitException

class MainHelpSuite extends AnyFunSuite{
  // path to namespace components
  private val configFile = getClass.getResource(s"/testbash/config.vsh.yaml").getPath

  test("viash config view default functionality without help") {
    val stdout = TestHelper.testMain(
      "config", "view",
      configFile
    )

    assert(stdout.startsWith("functionality:"))
    assert(stdout.contains("testbash"))
  }

  // We still can't test this properly as Scallop just exits hard
  // test("viash config view default functionality leading help") {
  //   val res = TestHelper.testMainException2[ExitException](
  //     "config", "view",
  //     "--help"
  //   )

  //   assert(res.output.startsWith("viash config view"))
  //   assert(!res.output.contains("testbash"))
  // }

  test("viash config view default functionality trailing help") {
    val output = TestHelper.testMainException[ExitException](
      "config", "view",
      configFile,
      "--help"
    )

    assert(output.startsWith("viash config view"))
    assert(!output.contains("testbash"))
  }

  test("viash config view default functionality trailing help after platform argument") {
    val output = TestHelper.testMainException[ExitException](
      "config", "view",
      configFile,
      "--platform", "native",
      "--help"
    )

    assert(output.startsWith("viash config view"))
    assert(!output.contains("testbash"))
  }

  test("viash config view default functionality trailing help before platform argument") {
    val output = TestHelper.testMainException[ExitException](
      "config", "view",
      configFile,
      "--help",
      "--platform", "native"
    )

    assert(output.startsWith("viash config view"))
    assert(!output.contains("testbash"))
  }


  test("viash config view default functionality with --help as platform argument") {
    val output = TestHelper.testMainException[RuntimeException](
      "config", "view",
      configFile,
      "--platform", "--help"
    )

    assert(!output.contains("viash config view"))
  }

}
