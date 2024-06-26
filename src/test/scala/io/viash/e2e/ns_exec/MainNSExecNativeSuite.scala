package io.viash.e2e.ns_exec

import io.viash._

import io.viash.helpers.Logger
import org.scalatest.funsuite.AnyFunSuite

import java.nio.file.Paths
import scala.util.matching.Regex

class MainNSExecNativeSuite extends AnyFunSuite {
  Logger.UseColorOverride.value = Some(false)

  private val nsPath = getClass.getResource("/testns").getPath()
  private val workingDir = Paths.get(nsPath)

  private val components = List(
    "ns_add",
    "ns_subtract",
    "ns_multiply",
    "ns_divide",
    "ns_power",
  )

  // Helpers for creating regexes
  private val configYaml = raw"config\.vsh\.yaml"
  private val nsPathRegex = raw"\[nsPath\]"

  // Create regexes for stderr and stdout checking
  private def createRegexes(str: String): (Regex, Regex) = {
    val stderrRegex = s"""\\+ echo $str""".r
    val stdoutRegex = s"""$str""".r
    (stderrRegex, stdoutRegex)
  }

  test("Check whether ns exec \\; works - old") {
    val testOutput = TestHelper.testMain(
        workingDir = Some(workingDir),
        "ns", "exec",
        "--apply_runner",
        "--apply_engine",
        "echo _{functionality-name}_ -{dir}- !{path}! ~{engine}~ ={namespace}=+\\;"
      )
    val stdout = testOutput.stdout.replaceAll(nsPath, "[nsPath]")
    val stderr = testOutput.stderr.replaceAll(nsPath, "[nsPath]")

    for (component <- components) {
      val (stderrRegex, stdoutRegex) = createRegexes(s"_${component}_ -$nsPathRegex/src/$component/?- !$nsPathRegex/src/$component/$configYaml! ~native~ =testns=")
      assert(stderrRegex.findFirstIn(stderr).isDefined, s"\nRegex: $stderrRegex; text: \n$stderr")
      assert(stdoutRegex.findFirstIn(stdout).isDefined, s"\nRegex: $stdoutRegex; text: \n$stdout")
    }
  }

  test("Check whether ns exec \\; works - new") {
    val testOutput = TestHelper.testMain(
        workingDir = Some(workingDir),
        "ns", "exec",
        "--apply_runner",
        "--apply_engine",
        "echo _{name}_ -{dir}- !{path}! ~{engine}~ ={namespace}=+\\;"
      )
    val stdout = testOutput.stdout.replaceAll(nsPath, "[nsPath]")
    val stderr = testOutput.stderr.replaceAll(nsPath, "[nsPath]")

    for (component <- components) {
      val (stderrRegex, stdoutRegex) = createRegexes(s"_${component}_ -$nsPathRegex/src/$component/?- !$nsPathRegex/src/$component/$configYaml! ~native~ =testns=")
      assert(stderrRegex.findFirstIn(stderr).isDefined, s"\nRegex: $stderrRegex; text: \n$stderr")
      assert(stdoutRegex.findFirstIn(stdout).isDefined, s"\nRegex: $stdoutRegex; text: \n$stdout")
    }
  }

  test("Check whether ns exec + works") {
    val testOutput = TestHelper.testMain(
      workingDir = Some(workingDir),
      "ns", "exec",
      "echo {path} +"
    )
    val stdout = testOutput.stdout.replaceAll(nsPath, "[nsPath]")
    val stderr = testOutput.stderr.replaceAll(nsPath, "[nsPath]")
    println(s"stdout: $stdout")

    // can't guarantee order of components
    val (stderrRegex, stdoutRegex) = createRegexes(s"($nsPathRegex/src/[^/]*/$configYaml ?){${components.length}}")
    assert(stderrRegex.findFirstIn(stderr).isDefined, s"\nRegex: $stderrRegex; text: \n$stderr")
    assert(stdoutRegex.findFirstIn(stdout).isDefined, s"\nRegex: $stdoutRegex; text: \n$stdout")
  }

  test("Check some other fields without those that require applying engines and runners") {
    val testOutput = TestHelper.testMain(
      workingDir = Some(workingDir),
      "ns", "exec",
      "echo {} {path} {abs-path} {dir} {abs-dir} {main-script} {abs-main-script} {functionality-name} {name} {namespace}"
    )

    val stdout = testOutput.stdout.replaceAll(nsPath, "[nsPath]")
    val stderr = testOutput.stderr.replaceAll(nsPath, "[nsPath]")

    // TODO non `abs` fields are returning absolute paths?
    for (component <- components) {
      val (stderrRegex, stdoutRegex) = createRegexes(s"$nsPathRegex/src/$component/$configYaml $nsPathRegex/src/$component/$configYaml $nsPathRegex/src/$component/$configYaml $nsPathRegex/src/$component $nsPathRegex/src/$component $nsPathRegex/src/$component/code\\.py $nsPathRegex/src/$component/code\\.py $component $component testns")
      assert(stderrRegex.findFirstIn(stderr).isDefined, s"\nRegex: $stderrRegex; text: \n$stderr")
      assert(stdoutRegex.findFirstIn(stdout).isDefined, s"\nRegex: $stdoutRegex; text: \n$stdout")
    }
  }

  test("Check message when specifying unknown field") {
    val testOutput = TestHelper.testMain(
      workingDir = Some(workingDir),
      "ns", "exec",
      "echo {foo} {name} {namespace}"
    )

    assert(testOutput.stderr.contains("Not all substitution fields are supported fields: foo."))
  }

  test("Check message when specifying output field without applying engine and runner") {
    val testOutput = TestHelper.testMain(
      workingDir = Some(workingDir),
      "ns", "exec",
      "echo {output} {abs-output} {name} {namespace}"
    )

    assert(testOutput.stderr.contains("Not all substitution fields are supported fields: output abs-output."))
  }

  test("Check message when specifying output fields when applying engine and runner") {
    val testOutput = TestHelper.testMain(
      workingDir = Some(workingDir),
      "ns", "exec",
      "-e",
      "echo {output} {abs-output} {name} {namespace}"
    )

    val stdout = testOutput.stdout.replaceAll(nsPath, "[nsPath]")
    val stderr = testOutput.stderr.replaceAll(nsPath, "[nsPath]")

    // TODO non `abs` fields are returning absolute paths?
    for (component <- components) {
      val (stderrRegex, stdoutRegex) = createRegexes(s"$nsPathRegex/target/executable/testns/$component $nsPathRegex/target/executable/testns/$component $component testns")
      assert(stderrRegex.findFirstIn(stderr).isDefined, s"\nRegex: $stderrRegex; text: \n$stderr")
      assert(stdoutRegex.findFirstIn(stdout).isDefined, s"\nRegex: $stdoutRegex; text: \n$stdout")
    }
  }

}
