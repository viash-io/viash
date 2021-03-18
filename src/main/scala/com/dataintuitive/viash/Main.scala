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
          setup = cli.build.setup(),
          push = cli.build.push()
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
          push = cli.namespace.build.push(),
          parallel = cli.namespace.build.parallel(),
          writeMeta = cli.namespace.build.writeMeta()
        )
      case List(cli.namespace, cli.namespace.test) =>
        val configs = readConfigs(cli.namespace.test, modifyFun = false)
        ViashNamespace.test(
          configs = configs,
          parallel = cli.namespace.test.parallel(),
          keepFiles = cli.namespace.test.keep.toOption.map(_.toBoolean),
          tsv = cli.namespace.test.tsv.toOption
        )
      case List(cli.namespace, cli.namespace.list) =>
        val configs = readConfigs(cli.namespace.test, modifyFun = false)
        ViashNamespace.list(
          configs = configs
        )
      case List(cli.config, cli.config.view) =>
        val config = Config.readOnly(
          configPath = cli.config.view.config(),
          commands = cli.config.view.command()
        )
        ViashConfig.view(config)
      case _ =>
        System.err.println("No subcommand was specified. See `viash --help` for more information.")
    }
  }

  def readConfig(
    subcommand: ViashCommand,
    modifyFun: Boolean = true
  ): Config = {
    Config.read(
      configPath = subcommand.config(),
      platform = subcommand.platform.toOption | subcommand.platformid.toOption,
      modifyFun = modifyFun,
      commands = subcommand.command()
    )
  }

  def readConfigs(
    subcommand: ViashNs,
    modifyFun: Boolean = true,
  ): List[Config] = {
    val source = subcommand.src()
    val query = subcommand.query.toOption
    val queryNamespace = subcommand.query_namespace.toOption
    val queryName = subcommand.query_name.toOption
    val sourceDir = Paths.get(source)

    // create regex for filtering platform ids
    val platformStr = (subcommand.platform.toOption | subcommand.platformid.toOption).getOrElse(".*")

    // find *.vsh.* files and parse as config
    val scriptFiles = find(sourceDir, (path, attrs) => {
      path.toString.contains(".vsh.") &&
        attrs.isRegularFile
    })

    scriptFiles.flatMap { file =>
      val conf1 =
        try {
          // first read config to get an idea of the available platforms
          val confTest =
            Config.read(file.toString, modifyFun = false, commands = subcommand.command())

          val funName = confTest.functionality.name
          val funNs = confTest.functionality.namespace

          // does name & namespace match regex?
          val queryTest = (queryNamespace, funNs) match {
            case (Some(regex), Some(ns)) => regex.r.findFirstIn(ns + "/" + funName).isDefined
            case (Some(regex), None) => regex.r.findFirstIn(funName).isDefined
            case (None, None) => true
          }
          val nameTest = queryName match {
            case Some(regex) => regex.r.findFirstIn(funName).isDefined
            case None => false
          }
          val namespaceTest = (queryNamespace, funNs) match {
            case (Some(regex), Some(ns)) => regex.r.findFirstIn(ns).isDefined
            case (Some(_), None) => false
            case (None, _) => true
          }

          // if config passes regex checks, return it
          if (queryTest && nameTest && namespaceTest) {
            Some(confTest)
          } else {
            None
          }
        } catch {
          case e: Exception => {
            System.err.println(s"Reading file '$file' failed")
            None
          }
        }

      if (conf1.isEmpty) {
        Nil
      } else {

        if (platformStr.contains(":") || (new File(platformStr)).exists) {
          // platform is a file
          List(Config.read(
            configPath = file.toString,
            platform = Some(platformStr),
            modifyFun = modifyFun,
            commands = subcommand.command()
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
              modifyFun = modifyFun,
              commands = subcommand.command()
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
}
