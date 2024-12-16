package io.viash.auxiliary

import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import java.nio.file.Paths

import io.viash.config.Config

import scala.io.Source
import io.viash.helpers.{IO, Exec, Logger}
import io.viash.TestHelper
import io.viash.helpers.data_structures._

class MainBuildAuxiliaryNativeUnknownParameter extends AnyFunSuite with BeforeAndAfterAll {
  Logger.UseColorOverride.value = Some(false)
  // which config to test
  private val configFile = getClass.getResource("/testbash/config.vsh.yaml").getPath

  private val temporaryFolder = IO.makeTemp("viash_tester")
  private val tempFolStr = temporaryFolder.toString

  // parse config from file
  private val config = Config.read(configFile)

  // built script that we'll be running
  private val executable = Paths.get(tempFolStr, config.name).toFile

  private val unknownArgumentWarning = """\[warning\] .* looks like a parameter but is not a defined parameter and will instead be treated as a positional argument\. Use --help to get more information on the parameters\.""".r

  // convert testbash
  test("viash can create the executable") {
    TestHelper.testMain(
      "build",
      "--engine", "native",
      "--runner", "executable",
      "-o", tempFolStr,
      configFile
    )

    assert(executable.exists)
    assert(executable.canExecute)
  }

  test("Check whether the executable can run") {
    Exec.run(
      Seq(executable.toString, "--help")
    )
  }

  test("Check whether particular keywords can be found in the usage") {
    val stdout =
      Exec.run(
        Seq(executable.toString, "--help")
      )

    val stripAll = (s : String) => s.replaceAll(raw"\s+", " ").trim

    config.allArguments.foreach(arg => {
      for (opt <- arg.alternatives; value <- opt)
        assert(stdout.contains(value))
      for (description <- arg.description) {
        assert(stripAll(stdout).contains(stripAll(description)))
      }
    })

    assert(unknownArgumentWarning.findFirstIn(stdout).isEmpty)
  }

  test("Check normal call with known parameters") {
    val out = Exec.runCatch(
      Seq(
        executable.toString,
        executable.toString,
        "--real_number", "10.5",
        "--whole_number=10",
        "-s", "a string with a few spaces",
      )
    )

    assert(out.exitValue == 0)
    assert(unknownArgumentWarning.findFirstIn(out.output).isEmpty)
  }

  test("Check call with unknown --foo parameter") {
    val out = Exec.runCatch(
      Seq(
        executable.toString,
        executable.toString,
        "--real_number", "10.5",
        "--whole_number=10",
        "-s", "a string with a few spaces",
        "--foo"
      )
    )

    assert(out.exitValue == 0)
    assert(unknownArgumentWarning.findFirstIn(out.output).isDefined)
  }

  test("Check call with unknown -foo parameter") {
    val out = Exec.runCatch(
      Seq(
        executable.toString,
        executable.toString,
        "--real_number", "10.5",
        "--whole_number=10",
        "-s", "a string with a few spaces",
        "-foo"
      )
    )

    assert(out.exitValue == 0)
    assert(unknownArgumentWarning.findFirstIn(out.output).isDefined)
  }

  test("Check call with unknown foo parameter, expecting processing as positional arguments") {
    val out = Exec.runCatch(
      Seq(
        executable.toString,
        executable.toString,
        "--real_number", "10.5",
        "--whole_number=10",
        "-s", "a string with a few spaces",
        "foo"
      )
    )

    assert(out.exitValue == 0)
    assert(unknownArgumentWarning.findFirstIn(out.output).isEmpty)
  }

  override def afterAll(): Unit = {
    IO.deleteRecursively(temporaryFolder)
  }
}