package com.dataintuitive.viash

import java.nio.file.{Files, Path, Paths}
import java.nio.file.attribute.BasicFileAttributes

import config.Config
import helpers.Scala._

import scala.collection.JavaConverters
import java.io.File

object Main {
  private val pkg = getClass.getPackage
  val name: String = if (pkg.getImplementationTitle != null) pkg.getImplementationTitle else "viash"
  val version: String = if (pkg.getImplementationVersion != null) pkg.getImplementationVersion else "test"

  def main(args: Array[String]) {
    // circumvent CLI limitations
    val workAroundCommands = List("run", "build", "test")
    val exceptionFlags = List("-h", "--help")
    val (configStr, viashArgs, runArgs) = {
      if (args.length >= 2 && workAroundCommands.contains(args(0)) && !exceptionFlags.contains(args(1))) {
        if (!args(1).contains(".vsh.")) {
          throw new RuntimeException("The first argument after 'viash " + args(0) + "' should be a viash config file.")
        }

        val argNoConfig = args.patch(1, Nil, 1)
        val (vArgs, rArgs) =
          if (args(0) == "run") {
            argNoConfig.span(_ != "--")
          } else {
            (argNoConfig, Array[String]())
          }
        (Some(args(1)), vArgs, rArgs)
      } else if (args.length == 1 && workAroundCommands.contains(args(0))) {
        throw new RuntimeException("The first argument after 'viash " + args(0) + "' should be a viash config file.")
      } else {
        (None, args, Array[String]())
      }
    }

    val cli = new CLIConf(viashArgs)

    cli.subcommands match {
      case List(cli.run) =>
        val config = readConfig(configStr, cli.run)
        ViashRun(config, args = runArgs.dropWhile(_ == "--"), keepFiles = cli.run.keep.toOption.map(_.toBoolean))
      case List(cli.build) =>
        val config = readConfig(configStr, cli.build)
        ViashBuild(config, output = cli.build.output(), printMeta = cli.build.meta(), setup = cli.build.setup())
      case List(cli.test) =>
        val config = readConfig(configStr, cli.test, modifyFun = false)
        ViashTest(config, keepFiles = cli.test.keep.toOption.map(_.toBoolean))
      case List(cli.namespace, cli.namespace.build) =>
        val configs = readConfigs(cli.namespace.build)
        ViashNamespace.build(
          configs = configs,
          target = cli.namespace.build.target(),
          setup = cli.namespace.build.setup(),
          parallel = cli.namespace.build.parallel()
        )
      case List(cli.namespace, cli.namespace.test) =>
        val configs = readConfigs(cli.namespace.test, modifyFun = false)
        ViashNamespace.test(
          configs = configs,
          parallel = cli.namespace.test.parallel(),
          keepFiles = cli.namespace.test.keep.toOption.map(_.toBoolean),
          tsv = cli.namespace.test.tsv.toOption,
        )
      case _ =>
        println("No subcommand was specified. See `viash --help` for more information.")
    }
  }

  def readConfig(
    config: Option[String],
    subcommand: ViashCommand,
    modifyFun: Boolean = true
  ): Config = {
    Config.read(
      config = config.get,
      platform = subcommand.platform.toOption | subcommand.platformid.toOption,
      modifyFun = modifyFun
    )
  }

  def readConfigs(
    subcommand: ViashNs,
    modifyFun: Boolean = true
  ): List[Config] = {
    val source = subcommand.src()
    val namespace = subcommand.namespace.toOption
    val sourceDir = Paths.get(source)

    // create regex for filtering platform ids
    val platformStr = (subcommand.platform.toOption | subcommand.platformid.toOption).getOrElse(".*")

    // create regex for filtering by namespace
    val namespaceMatch = {
      namespace match {
        case Some(ns) =>
          val nsRegex = s"""^$source/$ns/.*""".r
          (path: String) =>
            nsRegex.findFirstIn(path).isDefined
        case _ =>
          (_: String) => true
      }
    }

    // find *.vsh.* files and parse as config
    val scriptRegex = ".*\\.vsh\\.[^.]*$".r
    val scriptFiles = find(sourceDir, (path, attrs) => {
      path.toString.contains(".vsh.") &&
        attrs.isRegularFile &&
        namespaceMatch(path.toString)
    })

    scriptFiles.flatMap { file =>
      // first read config to get an idea of the available platforms
      val conf1 = Config.read(file.toString, modifyFun = false)

      // determine which namespace to use
      val _namespace = getNamespace(namespace, file)

      if (platformStr.contains(":") || (new File(platformStr)).exists) {
        // platform is a file
        List(Config.read(
          config = file.toString,
          platform = Some(platformStr),
          namespace = _namespace,
          modifyFun = modifyFun
        ))
      } else {
        // platform is a regex for filtering the ids
        val platIDs = conf1.platforms.map(_.id)

        val filteredPlats =
          if (platIDs.isEmpty) {
            // config did not contain any platforms, so the native platform should be used
            List(None)
          } else {
            // filter platforms using the regex
            platIDs.filter(platformStr.r.findFirstIn(_).isDefined).map(Some(_))
          }

        filteredPlats.map { plat =>
          Config.read(
            config = file.toString,
            platform = plat,
            namespace = _namespace,
            modifyFun = modifyFun
          )
        }
      }
    }
  }

  /**
   * Find all files in a directory and filter according to their properties.
   */
  def find(sourceDir: Path, filter: (Path, BasicFileAttributes) => Boolean): List[Path] = {
    val it = Files.find(sourceDir, Integer.MAX_VALUE, (p, b) => filter(p, b)).iterator()
    JavaConverters.asScalaIterator(it).toList
  }

  /**
   * Given a Path to a viash config file (functionality.yaml / *.vsh.yaml),
   * extract an implicit namespace if appropriate.
   */
  private def getNamespace(namespace: Option[String], file: Path): Option[String] = {
    if (namespace.isDefined) {
      namespace
    } else {
      val parentDir = file.toString.split("/").dropRight(2).lastOption.getOrElse("src")
      if (parentDir != "src")
        Some(parentDir)
      else
        None
    }
  }
}
