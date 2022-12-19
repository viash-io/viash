package io.viash.e2e.ns_list

import io.viash._

import io.viash.config.Config
import io.viash.helpers.{Exec, IO}
import io.circe.yaml.parser
import org.scalatest.{BeforeAndAfterAll, FunSuite}

import java.io.File
import java.nio.file.Paths
import scala.io.Source

class MainNSListNativeSuite extends FunSuite{
  // path to namespace components
  private val nsPath = getClass.getResource("/testns/").getPath
  private val scalaPath = getClass.getResource("/testscala/").getPath

  private val components = List(
    "ns_add",
    "ns_subtract",
    "ns_multiply",
    "ns_divide",
    "ns_power",
  )


  // convert testbash
  test("viash ns list") {
    val (stdout, stderr, exitCode) = TestHelper.testMainWithStdErr(
    "ns", "list",
      "-s", nsPath,
    )

    assert(exitCode == 1)

    for (component <- components) {
      val regexName = raw"""name:\s+"$component""""
      assert(regexName.r.findFirstIn(stdout).isDefined, s"\nRegex: ${regexName}; text: \n$stdout")
    }

    val regexBuildError = raw"Reading file \'.*/src/ns_error/config\.vsh\.yaml\' failed"
    assert(regexBuildError.r.findFirstIn(stderr).isDefined, "Expecting to get an error because of an invalid yaml in ns_error")

    val stdout2 = s"(?s)(\u001b.{4})?((Not all configs parsed successfully)|(All \\d+ configs parsed successfully)).*$$".r.replaceAllIn(stdout, "")

    val config = parser.parse(stdout2)
      .fold(throw _, _.as[Array[Config]])
      .fold(throw _, identity)

    // Do some sample checks whether relevant information is available. This certainly isn't comprehensive.
    // List of tuples with regex and expected count
    val samples = List(
      (raw"""[\r\n]+\s+name: "--input1"[\r\n]+""", 5),
      (raw"""[\r\n]+\s+resources:[\r\n]+\s+- type: "python_script"[\r\n]+""", 5),
      (raw"""[\r\n]+\s+test_resources:[\r\n]+\s+- type: "bash_script"[\r\n]+""", 4)
    )

    for ((regex, count) <- samples) {
      assert(regex.r.findAllMatchIn(stdout).size == count, s"Expecting $count hits on stdout of regex [$regex]")
    }
  }



  // convert testbash
  test("viash ns list filter by platform") {
    val (stdout, stderr, exitCode) = TestHelper.testMainWithStdErr(
    "ns", "list",
      "-s", nsPath,
      "-p", "docker"
    )

    assert(exitCode == 1)
    val configs = parser.parse(stdout)
      .fold(throw _, _.as[Array[Config]])
      .fold(throw _, identity)
    assert(configs.length == 0)
  }
  test("viash ns list filter by platform #2") {
    val (stdout, stderr, exitCode) = TestHelper.testMainWithStdErr(
    "ns", "list",
      "-s", scalaPath,
      "-p", "docker"
    )

    assert(exitCode == 0)

    val configs = parser.parse(stdout)
      .fold(throw _, _.as[Array[Config]])
      .fold(throw _, identity)
    assert(configs.length == 1)
  }
  test("viash ns list filter by platform #3") {
    val (stdout, stderr, exitCode) = TestHelper.testMainWithStdErr(
    "ns", "list",
      "-s", scalaPath,
      "-p", "not_exists"
    )

    assert(exitCode == 0)
    val configs = parser.parse(stdout)
      .fold(throw _, _.as[Array[Config]])
      .fold(throw _, identity)
    assert(configs.length == 0)
  }

}
