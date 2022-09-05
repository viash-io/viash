package io.viash

import io.viash.helpers.IO
import org.scalatest.{BeforeAndAfterAll, FunSuite}

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, OpenOption, Paths}
import scala.io.Source

class MainNSExecNativeSuite extends FunSuite with BeforeAndAfterAll {
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
    val (stdoutRaw, stderr) =
      TestHelper.testMainWithStdErr(
        "ns", "exec",
        "--src", nsPath,
        "echo _{functionality-name}_ -{dir}- !{path}! \\;"
      )
    val stdout = stdoutRaw.replaceAll(nsPath, "src/")

    for (component ← components) {
      val regexCommand = s"""\\+ echo _${component}_ -src/$component/?- !src/$component/config.vsh.yaml!""".r
      assert(regexCommand.findFirstIn(stdout).isDefined, s"\nRegex: $regexCommand; text: \n$stdout")
      val outputCommand = s"""  Output: _${component}_ -src/$component/?- !src/$component/config.vsh.yaml!""".r
      assert(outputCommand.findFirstIn(stdout).isDefined, s"\nRegex: $outputCommand; text: \n$stdout")
    }
  }

  test("Check whether ns exec + works") {
    val (stdoutRaw, stderr) = TestHelper.testMainWithStdErr(
      "ns", "exec",
      "--src", nsPath,
      "echo {path} +"
    )
    val stdout = stdoutRaw.replaceAll(nsPath, "src/")
    println(stdout)

    // can't guarantee order of components
    val regexCommand = s"""\\+ echo src/[^/]*/config.vsh.yaml src/[^/]*/config.vsh.yaml src/[^/]*/config.vsh.yaml src/[^/]*/config.vsh.yaml""".r
    assert(regexCommand.findFirstIn(stdout).isDefined, s"\nRegex: $regexCommand; text: \n$stdout")
    val outputCommand = s"""  Output: src/[^/]*/config.vsh.yaml src/[^/]*/config.vsh.yaml src/[^/]*/config.vsh.yaml src/[^/]*/config.vsh.yaml""".r
    assert(outputCommand.findFirstIn(stdout).isDefined, s"\nRegex: $outputCommand; text: \n$stdout")
  }

  override def afterAll() {
    IO.deleteRecursively(temporaryFolder)
  }
}
