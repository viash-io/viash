package io.viash.packageConfig

import io.circe.Json
import org.scalatest.funsuite.AnyFunSuite
import io.circe.syntax._
import java.nio.file.Paths
import io.viash.helpers.Logger

class PackageTest extends AnyFunSuite {
  Logger.UseColorOverride.value = Some(false)
  private val rootPath = Paths.get(getClass.getResource("/").getPath)
  private val testBashPath = rootPath.resolve("testbash")
  private val testNsPath = rootPath.resolve("testns")

  private val testNsPackPath = rootPath.resolve("testns/_viash.yaml")

  test("no package config file is found in testbash") {
    val packagePath = PackageConfig.findPackageFile(testBashPath)
    assert(packagePath.isEmpty)
  }

  test("package config file is found in testns") {
    val packagePath = PackageConfig.findPackageFile(testNsPath)
    assert(packagePath.isDefined)
    assert(packagePath.get == testNsPackPath)
  }

  test("reading package config file works") {
    val pack = PackageConfig.read(testNsPackPath)
    
    assert(pack.source.isDefined)
    assert(pack.source.get == rootPath.resolve("testns/src").toString)

    assert(pack.target.isDefined)
    assert(pack.target.get == rootPath.resolve("testns/target").toString)

    // Todo: change config mods into a configmods class
    val expectedCm = List(""".functionality.info := {"foo": "bar"}""")
    assert(pack.config_mods.toList == expectedCm)
  }

  test("combined function works in subdir") {
    val pack = PackageConfig.read(testNsPackPath)
    
    val pack2 = PackageConfig.findViashPackage(rootPath.resolve("testns/src/ns_add"))

    assert(pack2 == pack)
    assert(pack2.rootDir == Some(rootPath.resolve("testns")))
  }
}