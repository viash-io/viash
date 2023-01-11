package io.viash.auxiliary

import io.viash.TestHelper
import org.scalatest.FunSuite


class MainTestAuxiliaryComputationalRequirements extends FunSuite {

  private val configFile = getClass.getResource("/testbash/auxiliary_computational_requirements/check_computational_requirements.vsh.yaml").getPath
  
  test("Check without computational requirements") {
    val output = TestHelper.testMain(
      "test",
      configFile
    )

    assert(output.contains("cpus unset"))
    assert(output.contains("memory unset"))
  }

  test("Check set cpus in CLI") {
    val output = TestHelper.testMain(
      "test",
      "--cpus", "2",
      configFile
    )

    assert(output.contains("cpus: 2"))
    assert(output.contains("memory unset"))
  }

  test("Check set memory in CLI") {
    val output = TestHelper.testMain(
      "test",
      "--memory", "2mb",
      configFile
    )

    assert(output.contains("cpus unset"))
    assert(output.contains("memory: 2mb"))
  }

}