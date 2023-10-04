package io.viash.e2e.export

import io.viash._
import io.viash.helpers.Logger
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.BeforeAndAfter

import java.nio.file.{Files, Path}
import scala.io.Source

class MainExportSuite extends AnyFunSuite with BeforeAndAfter {
  Logger.UseColorOverride.value = Some(false)
  var tempFile: Path = _

  before {
    tempFile = Files.createTempFile("viash_export", ".txt")
  }

  after {
    Files.deleteIfExists(tempFile)
  }

  // These are all *very* basic tests. Practicly no validation whatsoever to check whether the output is correct or not.

  test("viash export resource") {
    val testOutput = TestHelper.testMain(
      "export", "resource", "runners/nextflow/WorkflowHelper.nf"
    )

    assert(testOutput.stdout.contains("def readConfig("))
  }

  test("viash export resource to file") {
    TestHelper.testMain(
      "export", "resource", "runners/nextflow/WorkflowHelper.nf",
      "--output", tempFile.toString
    )

    val lines = helpers.IO.read(tempFile.toUri())
    assert(lines.contains("def readConfig("))
  }

  test("viash export resource legacy") {
    val testOutput = TestHelper.testMain(
      "export", "resource", "platforms/nextflow/WorkflowHelper.nf"
    )

    assert(testOutput.stderr.contains("WARNING: The 'platforms/' prefix is deprecated. Please use 'runners/' instead."))

    assert(testOutput.stdout.contains("def readConfig("))
  }

  test("viash export resource to file legacy") {
    val testOutput = TestHelper.testMain(
      "export", "resource", "platforms/nextflow/WorkflowHelper.nf",
      "--output", tempFile.toString
    )

    assert(testOutput.stderr.contains("WARNING: The 'platforms/' prefix is deprecated. Please use 'runners/' instead."))

    val lines = helpers.IO.read(tempFile.toUri())
    assert(lines.contains("def readConfig("))
  }
  
  test("viash export cli_schema") {
    val testOutput = TestHelper.testMain(
      "export", "cli_schema"
    )

    assert(testOutput.stdout.startsWith("""- name: "run""""))
    assert(testOutput.stdout.contains("viash config inject"))
  }
  
  test("viash export cli_schema to file") {
    TestHelper.testMain(
      "export", "cli_schema",
      "--output", tempFile.toString
    )

    val lines = helpers.IO.read(tempFile.toUri())
    assert(lines.startsWith("""- name: "run""""))
    assert(lines.contains("viash config inject"))
  }

  test("viash export cli_autocomplete without format") {
    val testOutput = TestHelper.testMain(
      "export", "cli_autocomplete"
    )

    assert(testOutput.stdout.startsWith("""# bash completion for viash"""))
    assert(testOutput.stdout.contains("COMPREPLY=($(compgen -W 'run build test ns config' -- \"$cur\"))"))
  }

  test("viash export cli_autocomplete without format to file") {
    TestHelper.testMain(
      "export", "cli_autocomplete",
      "--output", tempFile.toString
    )

    val lines = helpers.IO.read(tempFile.toUri())
    assert(lines.startsWith("""# bash completion for viash"""))
    assert(lines.contains("COMPREPLY=($(compgen -W"))
  }

  test("viash export cli_autocomplete Bash") {
    val testOutput = TestHelper.testMain(
      "export", "cli_autocomplete",
      "--format", "bash"
    )

    assert(testOutput.stdout.startsWith("""# bash completion for viash"""))
    assert(testOutput.stdout.contains("COMPREPLY=($(compgen -W 'run build test ns config' -- \"$cur\"))"))
  }

  test("viash export cli_autocomplete Bash to file") {
    TestHelper.testMain(
      "export", "cli_autocomplete",
      "--format", "bash",
      "--output", tempFile.toString
    )

    val lines = helpers.IO.read(tempFile.toUri())
    assert(lines.startsWith("""# bash completion for viash"""))
    assert(lines.contains("COMPREPLY=($(compgen -W"))
  }

  test("viash export cli_autocomplete Zsh") {
    val testOutput = TestHelper.testMain(
      "export", "cli_autocomplete",
      "--format", "zsh"
    )

    assert(testOutput.stdout.startsWith("""#compdef viash"""))
    assert(testOutput.stdout.contains("_viash_export_commands"))
  }

  test("viash export cli_autocomplete Zsh to file") {
    TestHelper.testMain(
      "export", "cli_autocomplete",
      "--format", "zsh",
      "--output", tempFile.toString
    )

    val lines = helpers.IO.read(tempFile.toUri())
    assert(lines.startsWith("""#compdef viash"""))
    assert(lines.contains("_viash_export_commands"))
  }

  test("viash export config_schema") {
    val testOutput = TestHelper.testMain(
      "export", "config_schema"
    )

    assert(testOutput.stdout.startsWith("""- - name: "__this__""""))
    assert(testOutput.stdout.contains("""type: "OneOrMore[String]""""))
  }

  test("viash export config_schema to file") {
    TestHelper.testMain(
      "export", "config_schema",
      "--output", tempFile.toString
    )

    val lines = helpers.IO.read(tempFile.toUri())
    assert(lines.startsWith("""- - name: "__this__""""))
    assert(lines.contains("""type: "OneOrMore[String]""""))
  }

  test("viash export json_schema") {
    val testOutput = TestHelper.testMain(
      "export", "json_schema"
    )

    assert(testOutput.stdout.startsWith("""$schema: "https://json-schema.org/draft-07/schema#""""))
    assert(testOutput.stdout.contains("""- $ref: "#/definitions/Config""""))
  }

  test("viash export json_schema, explicit yaml format") {
    val testOutput = TestHelper.testMain(
      "export", "json_schema", "--format", "yaml"
    )

    assert(testOutput.stdout.startsWith("""$schema: "https://json-schema.org/draft-07/schema#""""))
    assert(testOutput.stdout.contains("""- $ref: "#/definitions/Config""""))
  }

  test("viash export json_schema to file, explicit yaml format") {
    TestHelper.testMain(
      "export", "json_schema", "--format", "yaml",
      "--output", tempFile.toString
    )

    val lines = helpers.IO.read(tempFile.toUri())
    assert(lines.startsWith("""$schema: "https://json-schema.org/draft-07/schema#""""))
    assert(lines.contains("""- $ref: "#/definitions/Config""""))
  }

  test("viash export json_schema, json format") {
    val testOutput = TestHelper.testMain(
      "export", "json_schema", "--format", "json"
    )

    assert(testOutput.stdout.startsWith(
        """{
          |  "$schema" : "https://json-schema.org/draft-07/schema#",
          |  "definitions" : {
          |""".stripMargin))
    assert(testOutput.stdout.contains(""""$ref" : "#/definitions/Config""""))
  }

  test("viash export json_schema to file, json format") {
    TestHelper.testMain(
      "export", "json_schema", "--format", "json",
      "--output", tempFile.toString
    )

    val lines = helpers.IO.read(tempFile.toUri())
    assert(lines.startsWith(
        """{
          |  "$schema" : "https://json-schema.org/draft-07/schema#",
          |  "definitions" : {
          |""".stripMargin))
    assert(lines.contains(""""$ref" : "#/definitions/Config""""))
  }

}
