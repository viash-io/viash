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
    namespace: Option[String] = None,
    setup: Boolean = false,
    parallel: Boolean = false
  ) {
    val configs = findConfigs(source, platform, platformID, namespace)

    val configs2 = if (parallel) configs.par else configs

    configs2.foreach {
      case Left(conf) =>
        val in = conf.info.get.parent_path.get
        val platType = conf.platform.get.id
        val out = in.replace(source, target + s"/$platType")
        val namespaceOrNothing = conf.functionality.namespace.map( s => "(" + s + ")").getOrElse("")
        println(s"Exporting $in $namespaceOrNothing =$platType=> $out")
        ViashBuild(
          config = conf,
          output = out,
          namespace = conf.functionality.namespace,
          setup = setup
        )
      case Right(err) =>
        val in = err.config.info.get.parent_path.get
        println(s"Skipping $in --- platform ${err.platform} not found")
    }
  }

  def findConfigs(
    source: String,
    platform: Option[String] = None,
    platformID: Option[String] = None,
    namespace: Option[String]
  ): List[Either[Config, PlatformNotFoundException]] = {
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

    /**
     * Given a Path to a viash config file (functionality.yaml / *.vsh.yaml),
     * extract an implicit namespace if appropriate.
     */
    def nameSpaceOption(file:Path):Option[String] = {
      val parentDir = file.toString.split("/").dropRight(2).lastOption.getOrElse("src")
      if (parentDir != "src")
        Some(parentDir)
      else
        None
    }

    /**
     * Transform a list of Paths to tuples containing either explicit (CLI) or implicit namespace.
     */
    def funFilesWithNamespace(
      files: List[Path],
      explicitNamespace: Option[String]
    ):List[(Path, Option[String])] =
      if (explicitNamespace.isDefined) {
        // Explicit namespace (on the CLI)
        files.map(file => (file, namespace))
      } else {
        // Implicit namespace (from src/ directory structure)
        files.map{file => (file, nameSpaceOption(file))}
      }

    val legacyConfigs = funFilesWithNamespace(funFiles, namespace).map { case (file, _namespace) =>
      try {
        Left(Config
              .readSplitOrJoined(
                functionality = Some(file.toString),
                platform = platform,
                platformID = platformID,
                namespace = _namespace))
      } catch {
        case e: PlatformNotFoundException => Right(e)
      }
    }

    // find *.vsh.* files and parse as config
    val scriptRegex = ".*\\.vsh\\.[^.]*$".r
    val scriptFiles = find(sourceDir, (path, attrs) => {
      scriptRegex.findFirstIn(path.toString.toLowerCase).isDefined &&
        attrs.isRegularFile &&
        namespaceMatch(path.toString)
    })
    val newConfigs = funFilesWithNamespace(scriptFiles, namespace).map { case (file, _namespace) =>
      try {
        Left(Config
              .readSplitOrJoined(
                joined = Some(file.toString),
                platform = platform,
                platformID = platformID,
                namespace = _namespace))
      } catch {
        case e: PlatformNotFoundException => Right(e)
      }
    }

    // merge configs
    legacyConfigs ::: newConfigs
  }
}
