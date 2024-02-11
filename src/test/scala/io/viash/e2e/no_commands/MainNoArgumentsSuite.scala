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
    val (stdout, stderr, exitCode) = TestHelper.testMainWithStdErr()

    assert(exitCode > 0)
    assert(stderr.contains("Error: No subcommand was specified. See `viash --help` for more information."))
  }
}
