package com.dataintuitive.viash

import com.dataintuitive.viash.config.Config
import com.dataintuitive.viash.helpers.{Exec, IO}
import io.circe.yaml.parser
import org.scalatest.{BeforeAndAfterAll, FunSuite}

import java.io.File
import java.nio.file.Paths
import scala.io.Source

class MainNSListNativeSuite extends FunSuite{
  // path to namespace components
  private val nsPath = getClass.getResource("/testns/").getPath

  private val components = List(
    "ns_add",
    "ns_subtract",
    "ns_multiply",
    "ns_divide",
    "ns_power",
  )


  // convert testbash
  test("viash ns list") {
    val (stdout, stderr) = TestHelper.testMainWithStdErr(
    "ns", "list",
      "-s", nsPath,
    )

    for (component ← components) {
      val regexName = raw"""name:\s+"$component""""
      assert(regexName.r.findFirstIn(stdout).isDefined, s"\nRegex: ${regexName}; text: \n$stdout")
    }

    val regexBuildError = raw"Reading file \'.*/src/ns_error/config\.vsh\.yaml\' failed"
    assert(regexBuildError.r.findFirstIn(stderr).isDefined, "Expecting to get an error because of an invalid yaml in ns_error")

    try {
      val config = parser.parse(stdout)
        .fold(throw _, _.as[Array[Config]])
        .fold(throw _, identity)

      assert(config.isInstanceOf[Array[Config]])
    }
    catch {
      case _: Throwable  => fail("Parsing the output from ns list should be parsable again")
    }



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

}
