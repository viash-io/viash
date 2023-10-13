package io.viash.e2e.ns_exec

import io.viash._

import io.viash.helpers.{IO, Logger}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, OpenOption, Paths}
import scala.io.Source

class MainNSExecNativeSuite extends AnyFunSuite with BeforeAndAfterAll {
  Logger.UseColorOverride.value = Some(false)
  // path to namespace components
  private val nsPath = getClass.getResource("/testns/src/").getPath

  private val components = List(
    "ns_add",
    "ns_subtract",
    "ns_multiply",
    "ns_divide",
    "ns_power",
  )

  private val temporaryFolder = IO.makeTemp("viash_ns_exec")
  private val tempFolStr = temporaryFolder.toString

  test("Check whether ns exec \\; works") {
    val testOutput = TestHelper.testMain(
        "ns", "exec",
        "--src", nsPath,
        "--apply_runner",
        "--apply_engine",
        "echo _{functionality-name}_ -{dir}- !{path}! ~{engine}~ ={namespace}=+\\;"
      )
    val stdout = testOutput.stdout.replaceAll(nsPath, "src/")
    val stderr = testOutput.stderr.replaceAll(nsPath, "src/")

    for (component <- components) {
      val regexCommand = s"""\\+ echo _${component}_ -src/$component/?- !src/$component/config.vsh.yaml! ~native~ =testns=""".r
      assert(regexCommand.findFirstIn(stderr).isDefined, s"\nRegex: $regexCommand; text: \n$stderr")
      val outputCommand = s"""_${component}_ -src/$component/?- !src/$component/config.vsh.yaml! ~native~ =testns=""".r
      assert(outputCommand.findFirstIn(stdout).isDefined, s"\nRegex: $outputCommand; text: \n$stdout")
    }
  }

  test("Check whether ns exec + works") {
    val testOutput = TestHelper.testMain(
      "ns", "exec",
      "--src", nsPath,
      "echo {path} +"
    )
    val stdout = testOutput.stdout.replaceAll(nsPath, "src/")
    val stderr = testOutput.stderr.replaceAll(nsPath, "src/")

    // can't guarantee order of components
    val regexCommand = s"""\\+ echo src/[^/]*/config.vsh.yaml src/[^/]*/config.vsh.yaml src/[^/]*/config.vsh.yaml src/[^/]*/config.vsh.yaml""".r
    assert(regexCommand.findFirstIn(stderr).isDefined, s"\nRegex: $regexCommand; text: \n$stderr")
    val outputCommand = s"""src/[^/]*/config.vsh.yaml src/[^/]*/config.vsh.yaml src/[^/]*/config.vsh.yaml src/[^/]*/config.vsh.yaml""".r
    assert(outputCommand.findFirstIn(stdout).isDefined, s"\nRegex: $outputCommand; text: \n$stdout")
  }

  override def afterAll(): Unit = {
    IO.deleteRecursively(temporaryFolder)
  }
}
