package io.viash.config_mods

import io.circe.Json
import org.scalatest.FunSuite
import io.circe.syntax._
import java.nio.file.Paths

class VcmFinderTest extends FunSuite {
  private val rootPath = Paths.get(getClass.getResource("/").getPath)
  private val testBashPath = rootPath.resolve("testbash")
  private val testPythonPath = rootPath.resolve("testpython")
  private val testNsPath = rootPath.resolve("testns")

  test("listing vcm files in testbash") {
    val paths = VcmFinder.listVcmFiles(testBashPath)
    assert(paths.length == 2)
    assert(paths.map(_.toFile().getName()).toSet == Set("test1.vcm", "test2.vcm"))
  }

  test("listing vcm files in testpython") {
    val paths = VcmFinder.listVcmFiles(testPythonPath)
    assert(paths.isEmpty)
  }

  test("listing vcm files in testns") {
    val paths = VcmFinder.listVcmFiles(testNsPath)
    assert(paths.length == 1)
    assert(paths.map(_.toFile().getName()).toSet == Set("project.vcm"))
  }

  test("walking vcm files in testbash") {
    val paths = VcmFinder.walkVcmFiles(testBashPath)
    assert(paths.length == 3)
    assert(paths.map(_.toFile().getName()).toSet == Set("test1.vcm", "test2.vcm", "project.vcm"))
  }

  test("walking vcm files in testpython") {
    val paths = VcmFinder.walkVcmFiles(testPythonPath)
    assert(paths.length == 1)
    assert(paths.map(_.toFile().getName()).toSet == Set("project.vcm"))
  }

  test("walking vcm files in testns") {
    val paths = VcmFinder.walkVcmFiles(testNsPath)
    assert(paths.length == 1)
    assert(paths.map(_.toFile().getName()).toSet == Set("project.vcm"))
  }

  test("finding viash config mods in testbash") {
    val cm = VcmFinder.findAllVcm(testBashPath)
    val result = cm.postparseCommands.toSet 
    
    val expectedStr = 
      """.functionality.authors += { name: "Rick" };
        |.functionality.authors += { name: "Sam" };
        |.platforms[.type == 'docker'].target_registry := 'ghcr.io';
        |.platforms[.type == 'docker'].target_organization := 'viash-io';
        |.platforms[.type == 'docker'].namespace_separator := '/';
        |.platforms[.type == 'docker'].target_image_source := 'https://github.com/viash-io/viash';
        """.stripMargin
    val expectedCm = ConfigModParser.block.parse(expectedStr)
    val expected = expectedCm.postparseCommands.toSet

    assert(result == expected)
  }

  test("finding viash config mods in testpython") {
    val cm = VcmFinder.findAllVcm(testPythonPath)
    val result = cm.postparseCommands.toSet 
    
    val expectedStr = 
      """.platforms[.type == 'docker'].target_registry := 'ghcr.io';
        |.platforms[.type == 'docker'].target_organization := 'viash-io';
        |.platforms[.type == 'docker'].namespace_separator := '/';
        |.platforms[.type == 'docker'].target_image_source := 'https://github.com/viash-io/viash';
        """.stripMargin
    val expectedCm = ConfigModParser.block.parse(expectedStr)
    val expected = expectedCm.postparseCommands.toSet

    assert(result == expected)
  }

  test("finding viash config mods in testns") {
    val cm = VcmFinder.findAllVcm(testNsPath)
    val result = cm.postparseCommands.toSet 
    
    val expectedStr = 
      """.functionality.info := {"foo": "bar"}"""
    val expectedCm = ConfigModParser.block.parse(expectedStr)
    val expected = expectedCm.postparseCommands.toSet
  }
}