package io.viash.e2e.no_commands

import io.viash._
import io.viash.helpers.Logger
import org.scalatest.funsuite.AnyFunSuite
import io.viash.exceptions.ExitException

class MainNoArgumentsSuite extends AnyFunSuite{
  Logger.UseColorOverride.value = Some(false)
  // path to namespace components
  private val configFile = getClass.getResource(s"/testbash/config.vsh.yaml").getPath

  test("viash without arguments produces helpful error message") {
    val testOutput = TestHelper.testMain()

    assert(testOutput.exitCode.isDefined)
    assert(testOutput.exitCode.get > 0)
    assert(testOutput.stderr.contains("Error: No subcommand was specified. See `viash --help` for more information."))
  }
}
