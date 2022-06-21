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
import cli.{CLIConf, CLIExport, ViashCommand, ViashNs}

import scala.collection.JavaConverters
import java.io.File
import java.io.FileNotFoundException
import java.nio.file.NoSuchFileException
import com.dataintuitive.viash.helpers.MissingResourceFileException
import com.dataintuitive.viash.helpers.BuildStatus._

object Main {
  private val pkg = getClass.getPackage
  val name: String = if (pkg.getImplementationTitle != null) pkg.getImplementationTitle else "viash"
  val version: String = if (pkg.getImplementationVersion != null) pkg.getImplementationVersion else "test"

  def main(args: Array[String]): Unit = {
    try {
      internalMain(args)
    } catch {
      case e @ ( _: FileNotFoundException | _: NoSuchFileException | _: MissingResourceFileException ) =>
        System.err.println(s"viash: ${e.getMessage()}")
        System.exit(1)
      case e: Exception =>
        System.err.println(
          s"""Unexpected error occurred! If you think this is a bug, please post
            |create an issue at https://github.com/viash-io/viash/issues containing
            |a reproducible example and the stack trace below.
            |
            |$name - $version
            |Stacktrace:""".stripMargin
        )
        e.printStackTrace()
        System.exit(1)
    }
  }
  def internalMain(args: Array[String]): Unit = {
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
          setup = cli.build.setup.toOption,
          push = cli.build.push()
        )
      case List(cli.test) =>
        val config = readConfig(cli.test, applyPlatform = false)
        ViashTest(config, keepFiles = cli.test.keep.toOption.map(_.toBoolean))
      case List(cli.namespace, cli.namespace.build) =>
        val configs = readConfigs(cli.namespace.build)
        ViashNamespace.build(
          configs = configs,
          target = cli.namespace.build.target(),
          setup = cli.namespace.build.setup.toOption,
          push = cli.namespace.build.push(),
          parallel = cli.namespace.build.parallel(),
          writeMeta = cli.namespace.build.writeMeta(),
          flatten = cli.namespace.build.flatten()
        )
      case List(cli.namespace, cli.namespace.test) =>
        val configs = readConfigs(cli.namespace.test, applyPlatform = false)
        ViashNamespace.test(
          configs = configs,
          parallel = cli.namespace.test.parallel(),
          keepFiles = cli.namespace.test.keep.toOption.map(_.toBoolean),
          tsv = cli.namespace.test.tsv.toOption,
          append = cli.namespace.test.append()
        )
      case List(cli.namespace, cli.namespace.list) =>
        val configs = readConfigs(cli.namespace.list, applyPlatform = false)
        ViashNamespace.list(
          configs = configs,
          cli.namespace.list.format()
        )
      case List(cli.config, cli.config.view) =>
        val config = Config.read(
          configPath = cli.config.view.config(),
          configMods = cli.config.view.config_mods(),
          modifyConfig = false
        )
        ViashConfig.view(config, cli.config.view.format())
      case List(cli.config, cli.config.inject) =>
        val config = Config.read(
          configPath = cli.config.inject.config(),
          configMods = cli.config.inject.config_mods(),
          modifyConfig = false
        )
        ViashConfig.inject(config)
      case Nil if (cli.cliexport()) =>
          CLIExport.export()
      case _ =>
        Console.err.println("No subcommand was specified. See `viash --help` for more information.")
    }
  }

  def readConfig(
    subcommand: ViashCommand,
    applyPlatform: Boolean = true
  ): Config = {
    Config.read(
      configPath = subcommand.config(),
      platform = subcommand.platform.toOption,
      applyPlatform = applyPlatform,
      configMods = subcommand.config_mods()
    )
  }

  def readConfigs(
    subcommand: ViashNs,
    applyPlatform: Boolean = true,
  ): List[Either[Config, BuildStatus]] = {
    val source = subcommand.src()
    val query = subcommand.query.toOption
    val queryNamespace = subcommand.query_namespace.toOption
    val queryName = subcommand.query_name.toOption
    val sourceDir = Paths.get(source)

    // create regex for filtering platform ids
    val platformStr = (subcommand.platform.toOption).getOrElse(".*")

    // find *.vsh.* files and parse as config
    val scriptFiles = find(sourceDir, (path, attrs) => {
      path.toString.contains(".vsh.") &&
        attrs.isRegularFile
    })

    scriptFiles.flatMap { file =>
      val conf1 =
        try {
          // first read config to get an idea of the available platforms
          val confTest = Config.read(
            file.toString, 
            applyPlatform = false, 
            configMods = subcommand.config_mods()
          )

          val funName = confTest.functionality.name
          val funNs = confTest.functionality.namespace

          // does name & namespace match regex?
          val queryTest = (query, funNs) match {
            case (Some(regex), Some(ns)) => regex.r.findFirstIn(ns + "/" + funName).isDefined
            case (Some(regex), None) => regex.r.findFirstIn(funName).isDefined
            case (None, _) => true
          }
          val nameTest = queryName match {
            case Some(regex) => regex.r.findFirstIn(funName).isDefined
            case None => true
          }
          val namespaceTest = (queryNamespace, funNs) match {
            case (Some(regex), Some(ns)) => regex.r.findFirstIn(ns).isDefined
            case (Some(_), None) => false
            case (None, _) => true
          }

          // if config passes regex checks, return it
          if (queryTest && nameTest && namespaceTest && confTest.functionality.enabled) {
            Left(confTest)
          } else {
            Right(Disabled)
          }
        } catch {
          case _: Exception =>
            Console.err.println(s"${Console.RED}Reading file '$file' failed${Console.RESET}")
            Right(ParseError)
        }

      if (conf1.isRight) {
        List(conf1)
      } else {

        val configs = if (platformStr.contains(":") || (new File(platformStr)).exists) {
          // platform is a file
          List(Config.read(
            configPath = file.toString,
            platform = Some(platformStr),
            applyPlatform = applyPlatform,
            configMods = subcommand.config_mods()
          ))
        } else {
          // platform is a regex for filtering the ids
          val platIDs = conf1.left.get.platforms.map(_.id)

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
              applyPlatform = applyPlatform,
              configMods = subcommand.config_mods()
            )
          }
        }
        configs.map(c => Left(c))
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
}
