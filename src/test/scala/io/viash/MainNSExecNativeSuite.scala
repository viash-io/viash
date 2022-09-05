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
    val (stdout, stderr) = TestHelper.testMainWithStdErr(
      "ns", "exec",
      "--src", nsPath,
      "echo {dir} {path} \\;"
    )

    for (component ← components) {
      val regexCommand = s"""\\+ echo .*src/test/resources/testns/src/$component/? .*src/test/resources/testns/src/$component/config.vsh.yaml""".r
      assert(regexCommand.findFirstIn(stdout).isDefined, s"\nRegex: $regexCommand; text: \n$stdout")
      val outputCommand = s"""  Output: .*src/test/resources/testns/src/$component/? .*src/test/resources/testns/src/$component/config.vsh.yaml""".r
      assert(outputCommand.findFirstIn(stdout).isDefined, s"\nRegex: $outputCommand; text: \n$stdout")
    }
  }

  test("Check whether ns exec + works") {
    val (stdout, stderr) = TestHelper.testMainWithStdErr(
      "ns", "exec",
      "--src", nsPath,
      "echo {path} +"
    )
    val regexCommand = s"""\\+ echo .*src/test/resources.*src/test/resources.*src/test/resources.*src/test/resources""".r // can't guarantee order of components
    assert(regexCommand.findFirstIn(stdout).isDefined, s"\nRegex: $regexCommand; text: \n$stdout")
    val outputCommand = s"""  Output: .*src/test/resources.*src/test/resources.*src/test/resources.*src/test/resources""".r
    assert(outputCommand.findFirstIn(stdout).isDefined, s"\nRegex: $outputCommand; text: \n$stdout")
  }

  override def afterAll() {
    IO.deleteRecursively(temporaryFolder)
  }
}
