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
      val (stderrRegex, stdoutRegex) = createRegexes(s"_${component}_ -src/$component/?- !src/$component/$configYaml! ~native~ =testns=")
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
      val (stderrRegex, stdoutRegex) = createRegexes(s"_${component}_ -src/$component/?- !src/$component/$configYaml! ~native~ =testns=")
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

    // can't guarantee order of components
    val (stderrRegex, stdoutRegex) = createRegexes(s"(src/[^/]*/$configYaml ?){${components.length}}")
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

    for (component <- components) {
      val (stderrRegex, stdoutRegex) = createRegexes(s"src/$component/$configYaml src/$component/$configYaml $nsPathRegex/src/$component/$configYaml src/$component $nsPathRegex/src/$component src/$component/code\\.py $nsPathRegex/src/$component/code\\.py $component $component testns")
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

    for (component <- components) {
      val (stderrRegex, stdoutRegex) = createRegexes(s"target/executable/testns/$component $nsPathRegex/target/executable/testns/$component $component testns")
      assert(stderrRegex.findFirstIn(stderr).isDefined, s"\nRegex: $stderrRegex; text: \n$stderr")
      assert(stdoutRegex.findFirstIn(stdout).isDefined, s"\nRegex: $stdoutRegex; text: \n$stdout")
    }
  }

  test("Output fields when the working directory is not the namespace directory, so the package config is not found, absolute src path") {
    // TODO the output field is not relativized to workingDir, as it only sets the path to search for the package config and any path relativizing is done from where the executable is run.
    // Typically, this is the same as the workingDir, but not with sbt test.
    val rootResourceDir = Paths.get(getClass.getResource("/").getPath())
    val testOutput = TestHelper.testMain(
      workingDir = Some(rootResourceDir),
      "ns", "exec",
      "--src", nsPath,
      "-e",
      "echo {path} {abs-path} {dir} {abs-dir} {main-script} {abs-main-script} {output} {abs-output} {name} {namespace}"
    )

    val stdout = testOutput.stdout.replaceAll(nsPath, "[nsPath]")
    val stderr = testOutput.stderr.replaceAll(nsPath, "[nsPath]")

    for (component <- components) {
      val (stderrRegex, stdoutRegex) = createRegexes(
        s"""src/$component/$configYaml $nsPathRegex/src/$component/$configYaml
           |src/$component $nsPathRegex/src/$component
           |src/$component/code\\.py $nsPathRegex/src/$component/code\\.py
           |target/executable/testns/$component .*/target/executable/testns/$component
           |$component testns
           |""".stripMargin.replace("\n", " ").strip()
      )
      assert(stderrRegex.findFirstIn(stderr).isDefined, s"\nRegex: $stderrRegex; text: \n$stderr")
      assert(stdoutRegex.findFirstIn(stdout).isDefined, s"\nRegex: $stdoutRegex; text: \n$stdout")
    }
  }

  test("Output fields when the working directory is not the namespace directory, so the package config is not found, relative path") {
    // TODO the output field is not relativized to workingDir, as it only sets the path to search for the package config and any path relativizing is done from where the executable is run.
    // Typically, this is the same as the workingDir, but not with sbt test.
    val rootResourceDir = Paths.get(getClass.getResource("/").getPath())
    val testOutput = TestHelper.testMain(
      workingDir = Some(rootResourceDir),
      "ns", "exec",
      "--src", "src/test/resources/testns",
      "-e",
      "echo {path} {abs-path} {dir} {abs-dir} {main-script} {abs-main-script} {output} {abs-output} {name} {namespace}"
    )

    val stdout = testOutput.stdout.replaceAll(nsPath, "[nsPath]")
    val stderr = testOutput.stderr.replaceAll(nsPath, "[nsPath]")

    for (component <- components) {
      val (stderrRegex, stdoutRegex) = createRegexes(
        s"""src/$component/$configYaml $nsPathRegex/src/$component/$configYaml
           |src/$component $nsPathRegex/src/$component
           |src/$component/code\\.py $nsPathRegex/src/$component/code\\.py
           |target/executable/testns/$component .*/target/executable/testns/$component
           |$component testns
           |""".stripMargin.replace("\n", " ").strip()
      )
      assert(stderrRegex.findFirstIn(stderr).isDefined, s"\nRegex: $stderrRegex; text: \n$stderr")
      assert(stdoutRegex.findFirstIn(stdout).isDefined, s"\nRegex: $stdoutRegex; text: \n$stdout")
    }
  }

  test("Output fields when the working directory is in a subdirector of the namespace directory, so the package config is found, but not in the working directory, no src path") {
    val testOutput = TestHelper.testMain(
      workingDir = Some(workingDir.resolve("src/ns_add")),
      "ns", "exec",
      "-e",
      "echo {path} {abs-path} {dir} {abs-dir} {main-script} {abs-main-script} {output} {abs-output} {name} {namespace}"
    )

    val stdout = testOutput.stdout.replaceAll(nsPath, "[nsPath]")
    val stderr = testOutput.stderr.replaceAll(nsPath, "[nsPath]")

    for (component <- components) {
      val (stderrRegex, stdoutRegex) = createRegexes(
        s"""src/$component/$configYaml $nsPathRegex/src/$component/$configYaml
           |src/$component $nsPathRegex/src/$component
           |src/$component/code\\.py $nsPathRegex/src/$component/code\\.py
           |target/executable/testns/$component $nsPathRegex/target/executable/testns/$component
           |$component testns
           |""".stripMargin.replace("\n", " ").strip()
      )
      assert(stderrRegex.findFirstIn(stderr).isDefined, s"\nRegex: $stderrRegex; text: \n$stderr")
      assert(stdoutRegex.findFirstIn(stdout).isDefined, s"\nRegex: $stdoutRegex; text: \n$stdout")
    }
  }

  test("Output fields when the working directory is in a subdirector of the namespace directory, so the package config is found, but not in the working directory, absolute src path") {
    val testOutput = TestHelper.testMain(
      workingDir = Some(workingDir.resolve("src/ns_add")),
      "ns", "exec",
      "--src", nsPath,
      "-e",
      "echo {path} {abs-path} {dir} {abs-dir} {main-script} {abs-main-script} {output} {abs-output} {name} {namespace}"
    )

    val stdout = testOutput.stdout.replaceAll(nsPath, "[nsPath]")
    val stderr = testOutput.stderr.replaceAll(nsPath, "[nsPath]")

    for (component <- components) {
      val (stderrRegex, stdoutRegex) = createRegexes(
        s"""src/$component/$configYaml $nsPathRegex/src/$component/$configYaml
           |src/$component $nsPathRegex/src/$component
           |src/$component/code\\.py $nsPathRegex/src/$component/code\\.py
           |target/executable/testns/$component $nsPathRegex/target/executable/testns/$component
           |$component testns
           |""".stripMargin.replace("\n", " ").strip()
      )
      assert(stderrRegex.findFirstIn(stderr).isDefined, s"\nRegex: $stderrRegex; text: \n$stderr")
      assert(stdoutRegex.findFirstIn(stdout).isDefined, s"\nRegex: $stdoutRegex; text: \n$stdout")
    }
  }

}
