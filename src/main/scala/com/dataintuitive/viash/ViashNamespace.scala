package com.dataintuitive.viash

import java.nio.file.{Paths, Files, Path}
import java.nio.file.attribute.BasicFileAttributes
import functionality.Functionality
import platforms.Platform

object ViashNamespace {
  def find(sourceDir: Path, filter: (Path, BasicFileAttributes) => Boolean) = {
    import scala.collection.JavaConverters._
    Files.find(sourceDir, Integer.MAX_VALUE, (p, b) => filter(p, b)).iterator().asScala.toList
  }

  def build(namespace: String, source: String, target: String) {
    val sourceDir = Paths.get(source)

    val funFiles = find(sourceDir, (path, attrs) => {
      path.toString().endsWith("functionality.yaml") && attrs.isRegularFile()
    })

    println(funFiles.mkString("functionality:\n  ", "\n  ", "\n"))

    val scriptRegex = ".*\\.r|\\.sh|\\.py".r
    val scriptFiles = find(sourceDir, (path, attrs) => {
      scriptRegex.findFirstIn(path.toString().toLowerCase).isDefined && attrs.isRegularFile()
    })

    println(scriptFiles.mkString("scripts:\n  ", "\n  ", "\n"))
  }

  def buildLegacy(funPath: Path, namespace: String, target: String) {
    //val fun = Main.readFunctionality(funPath.toString)
    val dir = funPath.getParent
    for (plat ← List("native", "docker", "nextflow")) {
      val platPath = Paths.get(dir.toString, "platform_" + plat + ".yaml")
      if (platPath.toFile.exists()) {
        println(s"  $plat platform file found")
//        val conf = Main.readAll(None, Some(funPath.toString), Some(platPath.toString), None, false)
      }
    }
    //Main.main(
  }
}