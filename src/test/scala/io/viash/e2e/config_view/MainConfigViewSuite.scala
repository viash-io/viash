package io.viash.e2e.config_view

import io.viash._
import io.viash.helpers.{IO, Logger}

import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import java.nio.file.{Files, Paths}
import io.circe.Json
import io.circe.yaml.parser

class MainConfigViewSuite extends AnyFunSuite with BeforeAndAfterAll {
  Logger.UseColorOverride.value = Some(false)
  // path to namespace components
  private val configFile = getClass.getResource(s"/testbash/config.vsh.yaml").getPath
  private val workingDirAnonymizing = getClass.getResource("/testns/").getPath()
  private val configFileAnonymizing = getClass.getResource("/testns/src/ns_add/config.vsh.yaml").getPath

  private val temporaryFolder = IO.makeTemp(s"viash_${this.getClass.getName}_")

  test("viash config view local") {
    val testOutput = TestHelper.testMain(
      "config", "view",
      configFile
    )

    assert(testOutput.stdout.startsWith("name:"))
    assert(testOutput.stdout.contains("testbash"))
  }

  test("viash config view remote") {
    val testOutput = TestHelper.testMain(
      "config", "view",
      "https://raw.githubusercontent.com/viash-io/viash/main/src/test/resources/testbash/config.vsh.yaml"
    )

    assert(testOutput.stdout.startsWith("name:"))
    assert(testOutput.stdout.contains("testbash"))
  }

  test("viash config view wit anonymized paths") {
    // Output is not anonymized when no package config is found, and that can't be found unless we pass the workingDir
    val testOutput = TestHelper.testMain(
      // workingDir = Some(Paths.get(workingDirAnonymizing)),
      "config", "view",
      configFileAnonymizing
    )

    assert(testOutput.stdout.contains(s"""config: "[anonymized]/config.vsh.yaml""""))
  }

  test("viash config view with relative anonymous paths") {
    val testOutput = TestHelper.testMain(
      workingDir = Some(Paths.get(workingDirAnonymizing)),
      "config", "view",
      configFileAnonymizing
    )

    assert(testOutput.stdout.contains("""config: "src/ns_add/config.vsh.yaml""""))
  }

  test("viash config view keeps null values in info fields") {
    val configPath = temporaryFolder.resolve("config.vsh.yaml")
    Files.write(configPath,
      """name: foo
        |info:
        |  string_field: value
        |  null_field: null
        |arguments:
        |  - name: --x
        |    type: string
        |    info:
        |      null_field: null
        |  - name: --y
        |    type: string
        |  - name: --z
        |    type: string
        |    info: {}
        |""".stripMargin.getBytes()
    )

    for (format <- List("yaml", "json")) {
      val testOutput = TestHelper.testMain(
        "config", "view",
        configPath.toString,
        "--format", format
      )

      val json = parser.parse(testOutput.stdout).fold(throw _, identity)
      val arguments = json.hcursor.downField("argument_groups").downN(0).downField("arguments")

      assert(json.hcursor.downField("info").focus == Some(Json.obj("string_field" -> Json.fromString("value"), "null_field" -> Json.Null)), s"format: $format")
      assert(arguments.downN(0).downField("info").focus == Some(Json.obj("null_field" -> Json.Null)), s"format: $format")
      // unset info fields are not shown
      assert(arguments.downN(1).downField("info").failed, s"format: $format")
      // blank info fields are kept
      assert(arguments.downN(2).downField("info").focus == Some(Json.obj()), s"format: $format")
    }
  }

  override def afterAll(): Unit = {
    IO.deleteRecursively(temporaryFolder)
  }
}
