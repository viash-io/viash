/*
 * Copyright (C) 2020  Data Intuitive
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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
    val (viashArgs, runArgs) = {
        if (args.length > 0 && args(0) == "run") {
          args.span(_ != "--")
        } else {
          (args, Array[String]())
        }
    }

    val cli = new CLIConf(viashArgs)

    cli.subcommands match {
      case List(cli.run) =>
        val config = readConfig(cli.run)
        ViashRun(config, args = runArgs.dropWhile(_ == "--"), keepFiles = cli.run.keep.toOption.map(_.toBoolean))
      case List(cli.build) =>
        val config = readConfig(cli.build)
        ViashBuild(
          config,
          output = cli.build.output(),
          printMeta = cli.build.printMeta(),
          writeMeta = cli.build.writeMeta(),
          setup = cli.build.setup()
        )
      case List(cli.test) =>
        val config = readConfig(cli.test, modifyFun = false)
        ViashTest(config, keepFiles = cli.test.keep.toOption.map(_.toBoolean))
      case List(cli.namespace, cli.namespace.build) =>
        val configs = readConfigs(cli.namespace.build)
        ViashNamespace.build(
          configs = configs,
          target = cli.namespace.build.target(),
          setup = cli.namespace.build.setup(),
          parallel = cli.namespace.build.parallel(),
          writeMeta = cli.namespace.build.writeMeta()
        )
      case List(cli.namespace, cli.namespace.test) =>
        val configs = readConfigs(cli.namespace.test, modifyFun = false)
        ViashNamespace.test(
          configs = configs,
          parallel = cli.namespace.test.parallel(),
          keepFiles = cli.namespace.test.keep.toOption.map(_.toBoolean),
          tsv = cli.namespace.test.tsv.toOption,
        )
      case List(cli.config, cli.config.view) =>
        val config = Config.readOnly(configPath = cli.config.view.config())
        val commands = cli.config.view.command.getOrElse(Nil)
        ViashConfig.view(config, commands)
      case _ =>
        println("No subcommand was specified. See `viash --help` for more information.")
    }
  }

  def readConfig(
    subcommand: ViashCommand,
    modifyFun: Boolean = true
  ): Config = {
    Config.read(
      configPath = subcommand.config(),
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
    val scriptFiles = find(sourceDir, (path, attrs) => {
      path.toString.contains(".vsh.") &&
        attrs.isRegularFile &&
        namespaceMatch(path.toString)
    })

    scriptFiles.flatMap { file =>
      val conf1 =
        try {
          // first read config to get an idea of the available platforms
          Some(Config.read(file.toString, modifyFun = false))
        } catch {
          case e: Exception => {
            println(s"Reading file '$file' failed")
            None
          }
        }

      if (conf1.isEmpty) {
        Nil
      } else {
        // determine which namespace to use
        val _namespace = getNamespace(namespace, file)

        if (platformStr.contains(":") || (new File(platformStr)).exists) {
          // platform is a file
          List(Config.read(
            configPath = file.toString,
            platform = Some(platformStr),
            namespace = _namespace,
            modifyFun = modifyFun
          ))
        } else {
          // platform is a regex for filtering the ids
          val platIDs = conf1.get.platforms.map(_.id)

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
              configPath = file.toString,
              platform = plat,
              namespace = _namespace,
              modifyFun = modifyFun
            )
          }
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
