package com.dataintuitive.viash

import java.nio.file.{Paths, Files, Path}
import java.nio.file.attribute.BasicFileAttributes
import config.Config
import config.Config.PlatformNotFoundException
import scala.collection.JavaConverters

object ViashNamespace {
  def find(sourceDir: Path, filter: (Path, BasicFileAttributes) => Boolean): List[Path] = {
    val it = Files.find(sourceDir, Integer.MAX_VALUE, (p, b) => filter(p, b)).iterator()
    JavaConverters.asScalaIterator(it).toList
  }

  def build(
    source: String,
    target: String,
    platform: Option[String] = None,
    platformID: Option[String] = None,
    namespace: Option[String] = None
  ) {
    val configs = findConfigs(source, platform, platformID, namespace)

    for ((conf, error) ← configs) {
      if (conf.isDefined) {
        val in = conf.get.info.get.parent_path.get
        val platType = conf.get.platform.get.id
        val out = in.replace(source, target + s"/$platType")
        println(s"Exporting $in =$platType=> $out")
        ViashBuild(conf.get, out, namespace = namespace)
      } else {
        val err = error.get
        val in = err.config.info.get.parent_path.get
        println(s"Skipping $in --- platform ${err.platform} not found")
      }
    }
  }

  def findConfigs(
    source: String,
    platform: Option[String] = None,
    platformID: Option[String] = None,
    namespace: Option[String]
  ): List[(Option[Config], Option[PlatformNotFoundException])] = {
    val sourceDir = Paths.get(source)

    val namespaceMatch =
      if (namespace.isDefined) {
        (path: String) => {
          val nsregex = s"""^$source/${namespace.get}/.*""".r
          nsregex.findFirstIn(path).isDefined
        }
      } else {
        (_: String) => true
      }

    // find funcionality.yaml files and parse as config
    val funFiles = find(sourceDir, (path, attrs) => {
      path.toString.endsWith("functionality.yaml") && attrs.isRegularFile && namespaceMatch(path.toString)
    })
    val legacyConfigs = funFiles.map { file =>
      try {
        (Some(Config.readSplitOrJoined(functionality = Some(file.toString), platform = platform, platformID = platformID, namespace = namespace)), None)
      } catch {
        case e: PlatformNotFoundException =>
          (None, Some(e))
      }
    }

    // find *.vsh.* files and parse as config
    val scriptRegex = ".*\\.vsh\\.[^.]*$".r
    val scriptFiles = find(sourceDir, (path, attrs) => {
      scriptRegex.findFirstIn(path.toString.toLowerCase).isDefined &&
        attrs.isRegularFile &&
        namespaceMatch(path.toString)
    })
    val newConfigs = scriptFiles.map { file =>
      try {
        (Some(Config.readSplitOrJoined(joined = Some(file.toString), platform = platform, platformID = platformID, namespace = namespace)), None)
      } catch {
        case e: PlatformNotFoundException =>
          (None, Some(e))
      }
    }

    // merge configs
    legacyConfigs ::: newConfigs
  }
}
