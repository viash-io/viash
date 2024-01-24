package io.viash.e2e.build

import io.viash._

import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import java.nio.file.Paths

import io.viash.config.Config

import scala.io.Source
import io.viash.helpers.{IO, Exec, Logger}
import io.viash.exceptions.ConfigParserException

import io.circe.Json
import io.circe.yaml.parser.parse
import io.viash.config_mods.ConfigModParser

class NativeProjectConfigSuite extends AnyFunSuite with BeforeAndAfterAll {
  Logger.UseColorOverride.value = Some(false)
  // which configs to test
  private val configFile = getClass.getResource(s"/testbash/config.vsh.yaml").getPath

  private val temporaryConfigFolder = IO.makeTemp(s"viash_${this.getClass.getName}_")
  private val configDeriver = ConfigDeriver(Paths.get(configFile), temporaryConfigFolder)

  val baseConfigFilePath = configDeriver.derive(""".functionality.version := null""", "base_config")

  // Use config mod functionality so that we can more easily differentiate between e.g. '.license' and '.project_config.license'
  val versionMod = ConfigModParser.parse(ConfigModParser.path, """.functionality.version""").get
  val licenseMod = ConfigModParser.parse(ConfigModParser.path, """.license""").get
  val organizationMod = ConfigModParser.parse(ConfigModParser.path, """.organization""").get
  val docker_registryMod = ConfigModParser.parse(ConfigModParser.path, """.engines[.type == "docker"].target_registry""").get
  val docker_repositoryMod = ConfigModParser.parse(ConfigModParser.path, """.engines[.type == "docker"].target_image_source""").get

  // TODO .repositories functionality is not tested here

  test("Base config where fields are not set and _viash.yaml does not exist") {
    val testOutput = TestHelper.testMain(
      workingDir = Some(temporaryConfigFolder),
      "config", "view",
      baseConfigFilePath
    )

    assert(testOutput.exitCode == Some(0))

    val baseJson = parse(testOutput.stdout).toOption.get

    assert(versionMod.get(baseJson) == Json.Null)
    assert(licenseMod.get(baseJson) == Json.Null)
    assert(organizationMod.get(baseJson) == Json.Null)
    assert(docker_registryMod.get(baseJson) == Json.arr(Json.Null, Json.Null))
    assert(docker_repositoryMod.get(baseJson) == Json.arr(Json.Null, Json.Null))
  }

  test("Add a _viash.yaml without component fields filled in") {
    val projectConfig = 
      """version: test_version
        |license: test_license
        |organization: test_organization
        |links:
        |  repository: test_repository
        |  docker_registry: test_registry
        |""".stripMargin
    IO.write(projectConfig, temporaryConfigFolder.resolve("_viash.yaml"))

    val testOutput = TestHelper.testMain(
      workingDir = Some(temporaryConfigFolder),
      "config", "view",
      baseConfigFilePath
    )

    assert(testOutput.exitCode == Some(0))

    val baseJson = parse(testOutput.stdout).toOption.get

    assert(versionMod.get(baseJson) == Json.fromString("test_version"))
    assert(licenseMod.get(baseJson) == Json.fromString("test_license"))
    assert(organizationMod.get(baseJson) == Json.fromString("test_organization"))
    assert(docker_registryMod.get(baseJson) == Json.arr(Json.fromString("test_registry"), Json.fromString("test_registry")))
    assert(docker_repositoryMod.get(baseJson) == Json.arr(Json.fromString("test_repository"), Json.fromString("test_repository")))
  }

  test("Add a _viash.yaml with component fields filled in") {
    val newConfigFilePath = 
      configDeriver.derive(
        List(
          """.functionality.version := "foo"""",
          """.license := "bar"""",
          """.organization := "baz"""",
          """.engines[.type == "docker"].target_registry := "qux"""",
          """.engines[.type == "docker"].target_image_source := "quux"""",
        ),
        "config_with_fields_set"
      )

    val projectConfig = 
      """version: test_version
        |license: test_license
        |organization: test_organization
        |links:
        |  repository: test_repository
        |  docker_registry: test_registry
        |""".stripMargin
    IO.write(projectConfig, temporaryConfigFolder.resolve("_viash.yaml"))

    val testOutput = TestHelper.testMain(
      workingDir = Some(temporaryConfigFolder),
      "config", "view",
      newConfigFilePath
    )

    assert(testOutput.exitCode == Some(0))

    val baseJson = parse(testOutput.stdout).toOption.get

    assert(versionMod.get(baseJson) == Json.fromString("foo"))
    assert(licenseMod.get(baseJson) == Json.fromString("bar"))
    assert(organizationMod.get(baseJson) == Json.fromString("baz"))
    assert(docker_registryMod.get(baseJson) == Json.arr(Json.fromString("qux"), Json.fromString("qux")))
    assert(docker_repositoryMod.get(baseJson) == Json.arr(Json.fromString("quux"), Json.fromString("quux")))
  }

  override def afterAll(): Unit = {
    IO.deleteRecursively(temporaryConfigFolder)
  }
}
