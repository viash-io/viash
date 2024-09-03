package io.viash.e2e.help

import io.viash._
import io.viash.helpers.Logger
import org.scalatest.funsuite.AnyFunSuite
import io.viash.exceptions.ExitException

class MainHelpSuite extends AnyFunSuite{
  Logger.UseColorOverride.value = Some(false)
  // path to namespace components
  private val configFile = getClass.getResource(s"/testbash/config.vsh.yaml").getPath

  test("viash config view default config without help") {
    val testOutput = TestHelper.testMain(
      "config", "view",
      configFile
    )

    assert(testOutput.stdout.startsWith("name:"))
    assert(testOutput.stdout.contains("testbash"))
  }

  test("viash config view with leading help") {
    val testOutput = TestHelper.testMainException[ExitException](
      "config", "view",
      "--help"
    )

    assert(testOutput.stdout.startsWith("viash config view"))
    assert(!testOutput.stdout.contains("testbash"))
  }

  test("viash config view with trailing help") {
    val testOutput = TestHelper.testMainException[ExitException](
      "config", "view",
      configFile,
      "--help"
    )

    assert(testOutput.stdout.startsWith("viash config view"))
    assert(!testOutput.stdout.contains("testbash"))
  }

  test("viash config view with trailing help after platform argument") {
    val testOutput = TestHelper.testMainException[ExitException](
      "config", "view",
      configFile,
      "--runner", "executable",
      "--help"
    )

    assert(testOutput.stdout.startsWith("viash config view"))
    assert(!testOutput.stdout.contains("testbash"))
  }

  test("viash config view with trailing help before platform argument") {
    val testOutput = TestHelper.testMainException[ExitException](
      "config", "view",
      configFile,
      "--help",
      "--runner", "executable"
    )

    assert(testOutput.stdout.startsWith("viash config view"))
    assert(!testOutput.stdout.contains("testbash"))
  }


  test("viash config view with --help as runner argument") {
    val testOutput = TestHelper.testMainException[RuntimeException](
      "config", "view",
      configFile,
      "--runner", "--help"
    )

    assert(!testOutput.stdout.contains("viash config view"))
  }

}
