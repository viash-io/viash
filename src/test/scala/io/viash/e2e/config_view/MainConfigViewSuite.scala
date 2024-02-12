package io.viash.e2e.config_view

import io.viash._
import io.viash.helpers.Logger

import org.scalatest.funsuite.AnyFunSuite
import java.nio.file.Paths

class MainConfigViewSuite extends AnyFunSuite{
  Logger.UseColorOverride.value = Some(false)
  // path to namespace components
  private val configFile = getClass.getResource(s"/testbash/config.vsh.yaml").getPath
  private val workingDirAnonymizing = getClass.getResource("/testns/").getPath()
  private val configFileAnonymizing = getClass.getResource("/testns/src/ns_add/config.vsh.yaml").getPath

  test("viash config view local") {
    val testOutput = TestHelper.testMain(
      "config", "view",
      configFile
    )

    assert(testOutput.stdout.startsWith("name:"))
    assert(testOutput.stdout.contains("testbash"))
  }

  test("viash config view remote") {
    val testOutput = TestHelper.testMain(
      "config", "view",
      "https://raw.githubusercontent.com/viash-io/viash/develop_0_8/src/test/resources/testbash/config.vsh.yaml"
    )

    assert(testOutput.stdout.startsWith("functionality:"))
    assert(testOutput.stdout.contains("testbash"))
  }

  test("viash config view wit anonymized paths") {
    // Output is not anonymized when no package config is found, and that can't be found unless we pass the workingDir
    val testOutput = TestHelper.testMain(
      // workingDir = Some(Paths.get(workingDirAnonymizing)),
      "config", "view",
      configFileAnonymizing
    )

    assert(testOutput.stdout.contains(s"""config: "[anonymized]/config.vsh.yaml""""))
  }

  test("viash config view with relative anonymous paths") {
    val testOutput = TestHelper.testMain(
      workingDir = Some(Paths.get(workingDirAnonymizing)),
      "config", "view",
      configFileAnonymizing
    )

    assert(testOutput.stdout.contains("""config: "src/ns_add/config.vsh.yaml""""))
  }

}
