package io.viash.e2e.export

import io.viash._
import io.viash.helpers.{Logger, IO}
import org.scalatest.funsuite.AnyFunSuite

import java.nio.file.{Files, Path, Paths}
import org.scalatest.BeforeAndAfterAll

class MainExportValidation extends AnyFunSuite with BeforeAndAfterAll {
  Logger.UseColorOverride.value = Some(false)
  private val temporaryFolder = IO.makeTemp(s"viash_${this.getClass.getName}_")

  private val configFile = getClass.getResource("/testbash/config.vsh.yaml").getPath
  private val configDeriver = ConfigDeriver(Paths.get(configFile), temporaryFolder)

  private val schemaFile = temporaryFolder.resolve("schema.json")
  private val validation = getClass.getResource("/verification/check_config/config.vsh.yaml").getPath

  test("Export json schema") {
    val testOutput = TestHelper.testMain(
      "export", "json_schema",
      "--format", "json",
      "--output", schemaFile.toString()
    )

    assert(testOutput.exitCode == Some(0))
    assert(Files.exists(schemaFile))
  }

  test("validate testbash with viash export json_schema", DockerTest) {
    val newConfigFilePath = configDeriver.derive(
      ".version := \"0.1\"",
      "testbash_version_string"
    )

    val testOutput2 = TestHelper.testMain(
      "run", validation.toString(),
      "--",
      "--schema", schemaFile.toString(),
      "--data", newConfigFilePath
    )

    assert(testOutput2.exitCode == Some(0), s"Validation failed; $testOutput2")
    assert(testOutput2.stdout.contains("testbash_version_string.vsh.yaml valid"))
  }

  test("validate testbash with viash export json_schema, invalid config - version should be a string", DockerTest) {
    val testOutput2 = TestHelper.testMain(
      "run", validation.toString(),
      "--",
      "--schema", schemaFile.toString(),
      "--data", configFile
    )

    assert(testOutput2.exitCode == Some(1))
    assert(testOutput2.stdout.contains("testbash/config.vsh.yaml invalid"))
    assert(testOutput2.stdout.contains("^^^ must be string"))
  }

  override def afterAll(): Unit = {
    IO.deleteRecursively(temporaryFolder)
  }
}
