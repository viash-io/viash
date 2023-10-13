package io.viash.e2e.ns_list

import io.viash._

import io.viash.config.Config
import io.viash.helpers.{Exec, IO, Logger}
import io.circe.yaml.parser
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite

import java.io.File
import java.nio.file.Paths
import scala.io.Source

class MainNSListNativeSuite extends AnyFunSuite{
  Logger.UseColorOverride.value = Some(false)
  // path to namespace components
  private val nsPath = getClass.getResource("/testns/").getPath
  private val scalaPath = getClass.getResource("/test_languages/scala/").getPath

  private val components = List(
    "ns_add",
    "ns_subtract",
    "ns_multiply",
    "ns_divide",
    "ns_power",
  )


  // convert testbash
  test("viash ns list") {
   val testOutput = TestHelper.testMain(
      "ns", "list",
      "-s", nsPath,
    )

    assert(testOutput.exitCode == Some(1))

    for (component <- components) {
      val regexName = raw"""name:\s+"$component""""
      assert(regexName.r.findFirstIn(testOutput.stdout).isDefined, s"\nRegex: ${regexName}; text: \n${testOutput.stdout}")
    }

    val regexBuildError = raw"Reading file \'.*/src/ns_error/config\.vsh\.yaml\' failed"
    assert(regexBuildError.r.findFirstIn(testOutput.stderr).isDefined, "Expecting to get an error because of an invalid yaml in ns_error")

    val stdout2 = s"(?s)(\u001b.{4})?((Not all configs parsed successfully)|(All \\d+ configs parsed successfully)).*$$".r.replaceAllIn(testOutput.stdout, "")

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
      assert(regex.r.findAllMatchIn(testOutput.stdout).size == count, s"Expecting $count hits on stdout of regex [$regex]")
    }
  }



  // convert testbash
  test("viash ns list filter by engine and runner") {
    val testOutput = TestHelper.testMain(
      "ns", "list",
      "-s", nsPath,
      "--engine", "docker",
      "--runner", "docker"
    )

    assert(testOutput.exitCode == Some(1))
    val configs = parser.parse(testOutput.stdout)
      .fold(throw _, _.as[Array[Config]])
      .fold(throw _, identity)
    assert(configs.length == 0)
  }
  test("viash ns list filter by engine #2") {
    val testOutput = TestHelper.testMain(
      "ns", "list",
      "-s", scalaPath,
      "--engine", "docker",
      "--runner", "executable"
    )

    assert(testOutput.exitCode == Some(0))

    val configs = parser.parse(testOutput.stdout)
      .fold(throw _, _.as[Array[Config]])
      .fold(throw _, identity)
    assert(configs.length == 1)
  }
  test("viash ns list filter by engine #3") {
    val testOutput = TestHelper.testMain(
     "ns", "list",
      "-s", scalaPath,
      "--engine", "not_exists",
      "--runner", "not_exists"
    )

    assert(testOutput.exitCode == Some(0))
    val configs = parser.parse(testOutput.stdout)
      .fold(throw _, _.as[Array[Config]])
      .fold(throw _, identity)
    assert(configs.length == 0)
  }

  // test query_name
  test("viash ns list query_name") {
    val testOutput = TestHelper.testMain(
     "ns", "list",
      "-s", nsPath,
      "--query_name", "ns_add"
    )

    assert(testOutput.exitCode == Some(1))
    val configs = parser.parse(testOutput.stdout)
      .fold(throw _, _.as[Array[Config]])
      .fold(throw _, identity)
    assert(configs.length == 1)
    assert(testOutput.stdout.contains("name: \"ns_add\""))
  }

  test("viash ns list query_name full match") {
    val testOutput = TestHelper.testMain(
     "ns", "list",
      "-s", nsPath,
      "--query_name", "^ns_add$"
    )

    assert(testOutput.exitCode == Some(1))
    val configs = parser.parse(testOutput.stdout)
      .fold(throw _, _.as[Array[Config]])
      .fold(throw _, identity)
    assert(configs.length == 1)
    assert(testOutput.stdout.contains("name: \"ns_add\""))
  }

  test("viash ns list query_name partial match") {
    val testOutput = TestHelper.testMain(
      "ns", "list",
      "-s", nsPath,
      "--query_name", "add"
    )

    assert(testOutput.exitCode == Some(1))
    val configs = parser.parse(testOutput.stdout)
      .fold(throw _, _.as[Array[Config]])
      .fold(throw _, identity)
    assert(configs.length == 1)
    assert(testOutput.stdout.contains("name: \"ns_add\""))
  }

  test("viash ns list query_name no match") {
    val testOutput = TestHelper.testMain(
      "ns", "list",
      "-s", nsPath,
      "--query_name", "foo"
    )

    assert(testOutput.exitCode == Some(1))
    assert(testOutput.stdout.trim() == "[]")
  }

  // test query
  test("viash ns list query") {
    val testOutput = TestHelper.testMain(
      "ns", "list",
      "-s", nsPath,
      "-q", "testns/ns_add"
    )

    assert(testOutput.exitCode == Some(1))
    val configs = parser.parse(testOutput.stdout)
      .fold(throw _, _.as[Array[Config]])
      .fold(throw _, identity)
    assert(configs.length == 1)
    assert(testOutput.stdout.contains("name: \"ns_add\""))
  }

  test("viash ns list query full match") {
    val testOutput = TestHelper.testMain(
      "ns", "list",
      "-s", nsPath,
      "-q", "^testns/ns_add$"
    )

    assert(testOutput.exitCode == Some(1))
    val configs = parser.parse(testOutput.stdout)
      .fold(throw _, _.as[Array[Config]])
      .fold(throw _, identity)
    assert(configs.length == 1)
    assert(testOutput.stdout.contains("name: \"ns_add\""))
  }

  test("viash ns list query partial match") {
    val testOutput = TestHelper.testMain(
      "ns", "list",
      "-s", nsPath,
      "-q", "test.*/.*add"
    )

    assert(testOutput.exitCode == Some(1))
    val configs = parser.parse(testOutput.stdout)
      .fold(throw _, _.as[Array[Config]])
      .fold(throw _, identity)
    assert(configs.length == 1)
    assert(testOutput.stdout.contains("name: \"ns_add\""))
  }

  test("viash ns list query only partial name") {
    val testOutput = TestHelper.testMain(
      "ns", "list",
      "-s", nsPath,
      "-q", "add"
    )

    assert(testOutput.exitCode == Some(1))
    val configs = parser.parse(testOutput.stdout)
      .fold(throw _, _.as[Array[Config]])
      .fold(throw _, identity)
    assert(configs.length == 1)
    assert(testOutput.stdout.contains("name: \"ns_add\""))
  }

  test("viash ns list query no match") {
    val testOutput = TestHelper.testMain(
      "ns", "list",
      "-s", nsPath,
      "-q", "foo"
    )

    assert(testOutput.exitCode == Some(1))
    assert(testOutput.stdout.trim() == "[]")
  }

  // test query_namespace
  test("viash ns list query_namespace") {
    val testOutput = TestHelper.testMain(
     "ns", "list",
      "-s", nsPath,
      "--query_namespace", "testns"
    )

    assert(testOutput.exitCode == Some(1))
    val configs = parser.parse(testOutput.stdout)
      .fold(throw _, _.as[Array[Config]])
      .fold(throw _, identity)

    assert(configs.length == components.length)
    assert(testOutput.stdout.contains("name: \"ns_add\""))
    assert(testOutput.stdout.contains("name: \"ns_subtract\""))
    assert(!testOutput.stdout.contains("name: \"ns_error\""))
    assert(!testOutput.stdout.contains("name: \"ns_disabled\""))
  }

  test("viash ns list query_namespace full match") {
    val testOutput = TestHelper.testMain(
     "ns", "list",
      "-s", nsPath,
      "--query_namespace", "^testns$"
    )

    assert(testOutput.exitCode == Some(1))
    val configs = parser.parse(testOutput.stdout)
      .fold(throw _, _.as[Array[Config]])
      .fold(throw _, identity)

    assert(configs.length == components.length)
    assert(testOutput.stdout.contains("name: \"ns_add\""))
    assert(testOutput.stdout.contains("name: \"ns_subtract\""))
    assert(!testOutput.stdout.contains("name: \"ns_error\""))
    assert(!testOutput.stdout.contains("name: \"ns_disabled\""))
  }

  test("viash ns list query_namespace partial match") {
    val testOutput = TestHelper.testMain(
      "ns", "list",
      "-s", nsPath,
      "--query_namespace", "test"
    )

    assert(testOutput.exitCode == Some(1))
    val configs = parser.parse(testOutput.stdout)
      .fold(throw _, _.as[Array[Config]])
      .fold(throw _, identity)

    assert(configs.length == components.length)
    assert(testOutput.stdout.contains("name: \"ns_add\""))
    assert(testOutput.stdout.contains("name: \"ns_subtract\""))
    assert(!testOutput.stdout.contains("name: \"ns_error\""))
    assert(!testOutput.stdout.contains("name: \"ns_disabled\""))
  }

  test("viash ns list query_namespace no match") {
    val testOutput = TestHelper.testMain(
      "ns", "list",
      "-s", nsPath,
      "--query_namespace", "foo"
    )

    assert(testOutput.exitCode == Some(1))
    assert(testOutput.stdout.trim() == "[]")
  }

}
