package com.dataintuitive.viash

import java.nio.file.{Paths, Files, Path}
import java.nio.file.attribute.BasicFileAttributes
import functionality.Functionality
import platforms.Platform
import config.Config

object ViashNamespace {
  def find(sourceDir: Path, filter: (Path, BasicFileAttributes) => Boolean) = {
    import scala.collection.JavaConverters._
    Files.find(sourceDir, Integer.MAX_VALUE, (p, b) => filter(p, b)).iterator().asScala.toList
  }

  def findAllConfigs(namespace: Option[String], source: String, target: String) = {
    val sourceDir = Paths.get(source)

    val funFiles = find(sourceDir, (path, attrs) => {
      path.toString().endsWith("functionality.yaml") && attrs.isRegularFile()
    })

    val legacyExports = funFiles.flatMap(getLegacyConfigs(_, namespace, target))

    val scriptRegex = ".*\\.vsh\\.[^\\.]*$".r
    val scriptFiles = find(sourceDir, (path, attrs) => {
      scriptRegex.findFirstIn(path.toString().toLowerCase).isDefined &&
        attrs.isRegularFile()
    })

    val newExports = scriptFiles.flatMap(getNewConfigs(_, namespace, target))

    legacyExports ::: newExports
  }

  def build(namespace: Option[String], source: String, target: String) {
    val allExports = findAllConfigs(namespace, source, target)

    for ((conf, in, out) ← allExports) {
      println(s"Exporting $in ==> $out")
      ViashExport.export(conf, out)

      val symlinkSrc = Paths.get(out, conf.functionality.name).toAbsolutePath()
      val symlinkDest = Paths.get(target, namespace.map(_ + "-").getOrElse("") + conf.functionality.name)

      if (symlinkSrc.toFile.exists) {
        if (symlinkDest.toFile.exists) {
          symlinkDest.toFile.delete() // overwrite previous symlinks
        }
        println(s"  Symlinking $symlinkSrc ==> $symlinkDest")
        Files.createSymbolicLink(symlinkDest, symlinkSrc)
      }
    }
  }

  def getLegacyConfigs(funPath: Path, namespace: Option[String], target: String) = {
    val fun = Functionality.read(funPath.toString())

    val dir = funPath.getParent

    List("native", "docker", "nextflow").flatMap{ plat =>
      val platPath = Paths.get(dir.toString, "platform_" + plat + ".yaml")
      val output = if (namespace.isDefined) {
        Paths.get(target, plat, namespace.get, fun.name)
      } else {
        Paths.get(target, plat, fun.name)
      }

      if (platPath.toFile.exists()) {
        val conf = Config.read(
          functionality = Some(funPath.toString()),
          platform = Some(platPath.toString())
        )

        Some((conf, funPath.toString, output.toString))
      } else {
        None
      }
    }
  }

  def getNewConfigs(configPath: Path, namespace: Option[String], target: String) = {
    val conf = Config.read(
      component = Some(configPath.toString())
    )
    val platforms = if (conf.platforms contains conf.platform.get) {
      conf.platforms
    } else {
      conf.platform.get :: conf.platforms
    }

    platforms.map{ pl =>
      val output = if (namespace.isDefined) {
        Paths.get(target, pl.id, namespace.get, conf.functionality.name)
      } else {
        Paths.get(target, pl.id, conf.functionality.name)
      }

      val newConf = conf.copy(platform = Some(pl))
      (newConf, configPath.toString, output.toString)
    }
  }
}