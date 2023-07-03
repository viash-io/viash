package io.viash.e2e.export

import io.viash._

import org.scalatest.funsuite.AnyFunSuite

class MainExportSuite extends AnyFunSuite{

  // These are all *very* basic tests. Practicly no validation whatsoever to check whether the output is correct or not.

  test("viash export resource") {
    val stdout = TestHelper.testMain(
      "export", "resource", "platforms/nextflow/WorkflowHelper.nf"
    )

    assert(stdout.startsWith("/////////////////////////////////////\n// Viash Workflow helper functions //"))
    assert(stdout.contains("preprocessInputs"))
  }
  
  test("viash export cli_schema") {
    val stdout = TestHelper.testMain(
      "export", "cli_schema"
    )

    assert(stdout.startsWith("""- name: "run""""))
    assert(stdout.contains("viash config inject"))
  }

  test("viash export config_schema") {
    val stdout = TestHelper.testMain(
      "export", "config_schema"
    )

    assert(stdout.startsWith("""- - name: "__this__""""))
    assert(stdout.contains("""type: "OneOrMore[String]""""))
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

}
