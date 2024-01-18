package io.viash.project

import io.circe.Json
import org.scalatest.funsuite.AnyFunSuite
import io.circe.syntax._
import java.nio.file.Paths
import io.viash.helpers.Logger

class ProjectTest extends AnyFunSuite {
  Logger.UseColorOverride.value = Some(false)
  private val rootPath = Paths.get(getClass.getResource("/").getPath)
  private val testBashPath = rootPath.resolve("testbash")
  private val testNsPath = rootPath.resolve("testns")

  private val testNsProjPath = rootPath.resolve("testns/_viash.yaml")

  test("no proj file is found in testbash") {
    val projPath = ViashProject.findProjectFile(testBashPath)
    assert(projPath.isEmpty)
  }

  test("proj file is found in testns") {
    val projPath = ViashProject.findProjectFile(testNsPath)
    assert(projPath.isDefined)
    assert(projPath.get == testNsProjPath)
  }

  test("reading proj file works") {
    val proj = ViashProject.read(testNsProjPath)
    
    assert(proj.source.isDefined)
    assert(proj.source.get == rootPath.resolve("testns/src").toString)

    assert(proj.target.isDefined)
    assert(proj.target.get == rootPath.resolve("testns/target").toString)

    // Todo: change config mods into a configmods class
    val expectedCm = List(""".functionality.info := {"foo": "bar"}""")
    assert(proj.config_mods.toList == expectedCm)
  }

  test("combined function works in subdir") {
    val proj = ViashProject.read(testNsProjPath)
    val projPath = Some(testNsProjPath.getParent())
    
    val (proj2, projPath2) = ViashProject.findViashProject(rootPath.resolve("testns/src/ns_add"))

    assert(proj2 == proj)
    assert(projPath2 == projPath)
  }
}