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
    val stdout = TestHelper.testMain(
      "export", "resource", "platforms/nextflow/WorkflowHelper.nf"
    )

    assert(stdout.contains("def readConfig("))
  }

  test("viash export resource to file") {
    val stdout = TestHelper.testMain(
      "export", "resource", "platforms/nextflow/WorkflowHelper.nf",
      "--output", tempFile.toString
    )

    val lines = helpers.IO.read(tempFile.toUri())
    assert(lines.contains("def readConfig("))
  }
  
  test("viash export cli_schema") {
    val stdout = TestHelper.testMain(
      "export", "cli_schema"
    )

    assert(stdout.startsWith("""- name: "run""""))
    assert(stdout.contains("viash config inject"))
  }
  
  test("viash export cli_schema to file") {
    val stdout = TestHelper.testMain(
      "export", "cli_schema",
      "--output", tempFile.toString
    )

    val lines = helpers.IO.read(tempFile.toUri())
    assert(lines.startsWith("""- name: "run""""))
    assert(lines.contains("viash config inject"))
  }

  test("viash export cli_autocomplete without format") {
    val stdout = TestHelper.testMain(
      "export", "cli_autocomplete"
    )

    assert(stdout.startsWith("""# bash completion for viash"""))
    assert(stdout.contains("COMPREPLY=($(compgen -W 'run build test ns config' -- \"$cur\"))"))
  }

  test("viash export cli_autocomplete without format to file") {
    val stdout = TestHelper.testMain(
      "export", "cli_autocomplete",
      "--output", tempFile.toString
    )

    val lines = helpers.IO.read(tempFile.toUri())
    assert(lines.startsWith("""# bash completion for viash"""))
    assert(lines.contains("COMPREPLY=($(compgen -W"))
  }

  test("viash export cli_autocomplete Bash") {
    val stdout = TestHelper.testMain(
      "export", "cli_autocomplete",
      "--format", "bash"
    )

    assert(stdout.startsWith("""# bash completion for viash"""))
    assert(stdout.contains("COMPREPLY=($(compgen -W 'run build test ns config' -- \"$cur\"))"))
  }

  test("viash export cli_autocomplete Bash to file") {
    val stdout = TestHelper.testMain(
      "export", "cli_autocomplete",
      "--format", "bash",
      "--output", tempFile.toString
    )

    val lines = helpers.IO.read(tempFile.toUri())
    assert(lines.startsWith("""# bash completion for viash"""))
    assert(lines.contains("COMPREPLY=($(compgen -W"))
  }

  test("viash export cli_autocomplete Zsh") {
    val stdout = TestHelper.testMain(
      "export", "cli_autocomplete",
      "--format", "zsh"
    )

    assert(stdout.startsWith("""#compdef viash"""))
    assert(stdout.contains("_viash_export_commands"))
  }

  test("viash export cli_autocomplete Zsh to file") {
    val stdout = TestHelper.testMain(
      "export", "cli_autocomplete",
      "--format", "zsh",
      "--output", tempFile.toString
    )

    val lines = helpers.IO.read(tempFile.toUri())
    assert(lines.startsWith("""#compdef viash"""))
    assert(lines.contains("_viash_export_commands"))
  }

  test("viash export config_schema") {
    val stdout = TestHelper.testMain(
      "export", "config_schema"
    )

    assert(stdout.startsWith("""- - name: "__this__""""))
    assert(stdout.contains("""type: "OneOrMore[String]""""))
  }

  test("viash export config_schema to file") {
    val stdout = TestHelper.testMain(
      "export", "config_schema",
      "--output", tempFile.toString
    )

    val lines = helpers.IO.read(tempFile.toUri())
    assert(lines.startsWith("""- - name: "__this__""""))
    assert(lines.contains("""type: "OneOrMore[String]""""))
  }

  test("viash export json_schema") {
    val stdout = TestHelper.testMain(
      "export", "json_schema"
    )

    assert(stdout.startsWith("""$schema: "https://json-schema.org/draft-07/schema#""""))
    assert(stdout.contains("""- $ref: "#/definitions/Config""""))
  }

  test("viash export json_schema, explicit yaml format") {
    val stdout = TestHelper.testMain(
      "export", "json_schema", "--format", "yaml"
    )

    assert(stdout.startsWith("""$schema: "https://json-schema.org/draft-07/schema#""""))
    assert(stdout.contains("""- $ref: "#/definitions/Config""""))
  }

  test("viash export json_schema to file, explicit yaml format") {
    val stdout = TestHelper.testMain(
      "export", "json_schema", "--format", "yaml",
      "--output", tempFile.toString
    )

    val lines = helpers.IO.read(tempFile.toUri())
    assert(lines.startsWith("""$schema: "https://json-schema.org/draft-07/schema#""""))
    assert(lines.contains("""- $ref: "#/definitions/Config""""))
  }

  test("viash export json_schema, json format") {
    val stdout = TestHelper.testMain(
      "export", "json_schema", "--format", "json"
    )

    assert(stdout.startsWith(
        """{
          |  "$schema" : "https://json-schema.org/draft-07/schema#",
          |  "definitions" : {
          |""".stripMargin))
    assert(stdout.contains(""""$ref" : "#/definitions/Config""""))
  }

  test("viash export json_schema to file, json format") {
    val stdout = TestHelper.testMain(
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
