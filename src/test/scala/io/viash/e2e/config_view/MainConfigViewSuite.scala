package io.viash.e2e.config_view

import io.viash._
import io.viash.helpers.Logger

import org.scalatest.funsuite.AnyFunSuite

class MainConfigViewSuite extends AnyFunSuite{
  Logger.UseColorOverride.value = Some(false)
  // path to namespace components
  private val configFile = getClass.getResource(s"/testbash/config.vsh.yaml").getPath



  test("viash config view local") {
    val testOutput = TestHelper.testMain(
      "config", "view",
      configFile
    )

    assert(testOutput.stdout.startsWith("functionality:"))
    assert(testOutput.stdout.contains("testbash"))
  }

  test("viash config view remote") {
    val testOutput = TestHelper.testMain(
      "config", "view",
      "https://raw.githubusercontent.com/viash-io/viash/develop/src/test/resources/testbash/config.vsh.yaml"
    )

    assert(testOutput.stdout.startsWith("functionality:"))
    assert(testOutput.stdout.contains("testbash"))
  }

}
