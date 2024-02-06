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

class NativePackageConfigSuite extends AnyFunSuite with BeforeAndAfterAll {
  Logger.UseColorOverride.value = Some(false)
  // which configs to test
  private val configFile = getClass.getResource(s"/testbash/config.vsh.yaml").getPath

  private val temporaryConfigFolder = IO.makeTemp(s"viash_${this.getClass.getName}_")
  private val configDeriver = ConfigDeriver(Paths.get(configFile), temporaryConfigFolder)

  val baseConfigFilePath = configDeriver.derive(""".functionality.version := null""", "base_config")

  // Use config mod functionality so that we can more easily differentiate between e.g. '.license' and '.package_config.license'
  val versionMod = ConfigModParser.parse(ConfigModParser.path, """.functionality.version""").get
  val licenseMod = ConfigModParser.parse(ConfigModParser.path, """.functionality.license""").get
  val organizationMod = ConfigModParser.parse(ConfigModParser.path, """.functionality.organization""").get
  val linksRepositoryMod = ConfigModParser.parse(ConfigModParser.path, """.functionality.links.repository""").get
  val linksRegistryMod = ConfigModParser.parse(ConfigModParser.path, """.functionality.links.docker_registry""").get

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
    assert(linksRegistryMod.get(baseJson) == Json.Null)
    assert(linksRepositoryMod.get(baseJson) == Json.Null)
  }

  test("Add a _viash.yaml without component fields filled in") {
    val packageConfig = 
      """version: test_version
        |license: test_license
        |organization: test_organization
        |links:
        |  repository: test_repository
        |  docker_registry: test_registry
        |""".stripMargin
    IO.write(packageConfig, temporaryConfigFolder.resolve("_viash.yaml"))

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
    assert(linksRegistryMod.get(baseJson) == Json.fromString("test_registry"))
    assert(linksRepositoryMod.get(baseJson) == Json.fromString("test_repository"))
  }

  test("Add a _viash.yaml with component fields filled in") {
    val newConfigFilePath = 
      configDeriver.derive(
        List(
          """.functionality.version := "foo"""",
          """.functionality.license := "bar"""",
          """.functionality.organization := "baz"""",
          """.functionality.links := { repository: "qux", docker_registry: "quux" }""",
        ),
        "config_with_fields_set"
      )

    val packageConfig = 
      """version: test_version
        |license: test_license
        |organization: test_organization
        |links:
        |  repository: test_repository
        |  docker_registry: test_registry
        |""".stripMargin
    IO.write(packageConfig, temporaryConfigFolder.resolve("_viash.yaml"))

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
    assert(linksRepositoryMod.get(baseJson) == Json.fromString("qux"))
    assert(linksRegistryMod.get(baseJson) == Json.fromString("quux"))
  }

  override def afterAll(): Unit = {
    IO.deleteRecursively(temporaryConfigFolder)
  }
}
