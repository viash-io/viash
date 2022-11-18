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

package io.viash

import java.nio.file.Paths
import config.Config
import helpers.Scala._
import cli.{CLIConf, ViashCommand, ViashNs}

import java.io.File
import java.io.FileNotFoundException
import java.nio.file.NoSuchFileException
import io.viash.helpers.MissingResourceFileException
import io.viash.helpers.status._
import io.viash.platforms.Platform

object Main {
  private val pkg = getClass.getPackage
  val name: String = if (pkg.getImplementationTitle != null) pkg.getImplementationTitle else "viash"
  val version: String = if (pkg.getImplementationVersion != null) pkg.getImplementationVersion else "test"

  def main(args: Array[String]): Unit = {
    try {
      val exitCode = internalMain(args)
      System.exit(exitCode)
    } catch {
      case e @ ( _: FileNotFoundException | _: NoSuchFileException | _: MissingResourceFileException ) =>
        Console.err.println(s"viash: ${e.getMessage()}")
        System.exit(1)
      case e: Exception =>
        Console.err.println(
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
  def internalMain(args: Array[String]): Int = {
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
        val (config, platform) = readConfig(cli.run)
        ViashRun(
          config = config,
          platform = platform.get, 
          args = runArgs.dropWhile(_ == "--"), 
          keepFiles = cli.run.keep.toOption.map(_.toBoolean),
          cpus = cli.run.cpus.toOption,
          memory = cli.run.memory.toOption
        )
      case List(cli.build) =>
        val (config, platform) = readConfig(cli.build)
        ViashBuild(
          config = config,
          platform = platform.get,
          output = cli.build.output(),
          setup = cli.build.setup.toOption,
          push = cli.build.push()
        )
        0 // Exceptions are thrown when something bad happens, so then the '0' is not returned but a '1'. Can be improved further.
      case List(cli.test) =>
        val (config, platform) = readConfig(cli.test)
        ViashTest(
          config,
          platform = platform.get,
          keepFiles = cli.test.keep.toOption.map(_.toBoolean),
          cpus = cli.test.cpus.toOption,
          memory = cli.test.memory.toOption
        )
        0 // Exceptions are thrown when a test fails, so then the '0' is not returned but a '1'. Can be improved further.
      case List(cli.namespace, cli.namespace.build) =>
        val configs = readConfigs(cli.namespace.build)
        ViashNamespace.build(
          configs = configs,
          target = cli.namespace.build.target(),
          setup = cli.namespace.build.setup.toOption,
          push = cli.namespace.build.push(),
          parallel = cli.namespace.build.parallel(),
          flatten = cli.namespace.build.flatten()
        )
        0 // Might be possible to be improved further.
      case List(cli.namespace, cli.namespace.test) =>
        val configs = readConfigs(cli.namespace.test)
        val testResults = ViashNamespace.test(
          configs = configs,
          parallel = cli.namespace.test.parallel(),
          keepFiles = cli.namespace.test.keep.toOption.map(_.toBoolean),
          tsv = cli.namespace.test.tsv.toOption,
          append = cli.namespace.test.append(),
          cpus = cli.namespace.test.cpus.toOption,
          memory = cli.namespace.test.memory.toOption
        )
        val errors = testResults.flatMap(_.right.toOption).count(_.isError)
        if (errors > 0) 1 else 0
      case List(cli.namespace, cli.namespace.list) =>
        val configs = readConfigs(
          cli.namespace.list, 
          addOptMainScript = false, 
          applyPlatform = false
        )
        ViashNamespace.list(
          configs = configs,
          format = cli.namespace.list.format(),
          parseArgumentGroups = cli.namespace.list.parse_argument_groups()
        )
        val errors = configs.flatMap(_.right.toOption).count(_.isError)
        if (errors > 0) 1 else 0
      case List(cli.namespace, cli.namespace.exec) =>
        val configs = readConfigs(cli.namespace.exec, applyPlatform = cli.namespace.exec.applyPlatform())
        ViashNamespace.exec(
          configs = configs,
          command = cli.namespace.exec.cmd(),
          dryrun = cli.namespace.exec.dryrun(),
          parallel = cli.namespace.exec.parallel()
        )
        val errors = configs.flatMap(_.right.toOption).count(_.isError)
        if (errors > 0) 1 else 0
      case List(cli.config, cli.config.view) =>
        val (config, _) = readConfig(
          subcommand = cli.config.view,
          addOptMainScript = false,
          applyPlatform = false
        )
        ViashConfig.view(
          config, 
          format = cli.config.view.format(),
          parseArgumentGroups = cli.config.view.parse_argument_groups()
        )
        0
      case List(cli.config, cli.config.inject) =>
        val (config, _) = readConfig(
          subcommand = cli.config.inject,
          addOptMainScript = false,
          applyPlatform = false
        )
        ViashConfig.inject(config)
        0
      case List(cli.export, cli.export.cli_schema) =>
        val output = cli.export.cli_schema.output.toOption.map(Paths.get(_))
        ViashExport.exportCLISchema(output)
        0
      case List(cli.export, cli.export.config_schema) =>
        val output = cli.export.config_schema.output.toOption.map(Paths.get(_))
        ViashExport.exportConfigSchema(output)
        0
      case List(cli.export, cli.export.resource) =>
        val output = cli.export.resource.output.toOption.map(Paths.get(_))
        ViashExport.exportResource(cli.export.resource.path.toOption.get, output)
        0
      case _ =>
        Console.err.println("No subcommand was specified. See `viash --help` for more information.")
        1
    }
  }

  def processConfigWithPlatform(
    config: Config, 
    platformStr: Option[String]
  ): (Config, Option[Platform]) = {
    // add platformStr to the info object
    val conf1 = config.copy(
      info = config.info.map{
        _.copy(platform = platformStr)
      }
    )

    // find platform, see javadoc of this function for details on how
    val plat = conf1.findPlatform(platformStr)

    (conf1, Some(plat))
  }

  def readConfig(
    subcommand: ViashCommand,
    addOptMainScript: Boolean = true,
    applyPlatform: Boolean = true
  ): (Config, Option[Platform]) = {
    val config = Config.read(
      configPath = subcommand.config(),
      addOptMainScript = addOptMainScript,
      configMods = subcommand.config_mods()
    )
    if (applyPlatform) {
      processConfigWithPlatform(
        config = config, 
        platformStr = subcommand.platform.toOption
      )
    } else {
      (config, None)
    }
  }
  
  def readConfigs(
    subcommand: ViashNs,
    addOptMainScript: Boolean = true,
    applyPlatform: Boolean = true
  ): List[Either[(Config, Option[Platform]), Status]] = {
    val source = subcommand.src()
    val query = subcommand.query.toOption
    val queryNamespace = subcommand.query_namespace.toOption
    val queryName = subcommand.query_name.toOption
    val platformStr = subcommand.platform.toOption
    val configMods = subcommand.config_mods()

    val configs = Config.readConfigs(
      source = source,
      query = query,
      queryNamespace = queryNamespace,
      queryName = queryName,
      configMods = configMods,
      addOptMainScript = addOptMainScript
    )

    if (applyPlatform) {
      // create regex for filtering platform ids
      val platformStrVal = platformStr.getOrElse(".*")

      configs.flatMap{config => config match {
        // passthrough statuses
        case Right(stat) => List(Right(stat))
        case Left(conf1) =>
          val platformStrs = 
            if (platformStrVal.contains(":") || (new File(platformStrVal)).exists) {
              // platform is a file
              List(Some(platformStrVal))
            } else {
              // platform is a regex for filtering the ids
              val platIDs = conf1.platforms.map(_.id)

              if (platIDs.isEmpty) {
                // config did not contain any platforms, so the native platform should be used
                List(None)
              } else {
                // filter platforms using the regex
                platIDs.filter(platformStrVal.r.findFirstIn(_).isDefined).map(Some(_))
              }
            }
          platformStrs.map{ platStr =>
            Left(processConfigWithPlatform(
              config = conf1,
              platformStr = platStr
            ))
          }
        }}
    } else {
      configs.map{c => c match {
        case Right(status) => Right(status)
        case Left(conf) => Left((conf, None: Option[Platform]))
      }}
    }
  }

  

}
