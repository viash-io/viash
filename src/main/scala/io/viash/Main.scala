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

import java.io.{File, FileNotFoundException}
import java.nio.file.{Path, Paths, Files, NoSuchFileException, FileSystemNotFoundException}
import java.net.URI
import sys.process.{Process, ProcessLogger}

import config.Config
import helpers.{IO, Exec, SysEnv, DependencyResolver, Logger, Logging}
import helpers.status._
import project.ViashProject
import cli.{CLIConf, ViashCommand, DocumentedSubcommand, ViashNs, ViashNsBuild, ViashLogger}
import exceptions._
import scala.util.Try
import org.rogach.scallop._
import io.viash.helpers.LoggerLevel
import io.viash.runners.Runner
import io.viash.config.AppliedConfig
import io.viash.functionality.Functionality
import io.viash.engines.Engine

object Main extends Logging {
  private val pkg = getClass.getPackage
  val name: String = if (pkg.getImplementationTitle != null) pkg.getImplementationTitle else "viash"
  val version: String = if (pkg.getImplementationVersion != null) pkg.getImplementationVersion else "test"

  /**
    * Viash main
    * 
    * This function will process the command-line
    * arguments and run the desired Viash command.
    * 
    * Internally, a different version of Viash may be
    * used if the user so desires.
    * 
    * This function should not throw exceptions but instead
    * exit with exit code > 0 if an exception has occurred.
    *
    * @param args The command line arguments
    */
  def main(args: Array[String]): Unit = {
    try {
      val exitCode = mainCLIOrVersioned(args)
      System.exit(exitCode)
    } catch {
      case e @ ( _: FileNotFoundException | _: MissingResourceFileException ) =>
        info(s"viash: ${e.getMessage()}")
        System.exit(1)
      case e: NoSuchFileException =>
        // This exception only returns the file/path that can't be found. Add a bit more feedback to the user.
        info(s"viash: ${e.getMessage()} (No such file or directory)")
        System.exit(1)
      case e: AbstractConfigException =>
        info(s"viash: Error parsing, ${e.innerMessage} in file '${e.uri}'.")
        info(s"Details:\n${e.getMessage()}")
        System.exit(1)
      case e: MalformedInputException =>
        info(s"viash: ${e.getMessage()}")
        System.exit(1)
      case e: AbstractDependencyException =>
        info(s"viash: ${e.getMessage()}")
        System.exit(1)
      case ee: ExitException =>
        System.exit(ee.code)
      case e: Exception =>
        info(
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

  /**
    * Detect Viash version and run
    * 
    * Detect which Viash version the user wants to run
    * and either run the function that will process the 
    * command-line arguments internally or pass them to
    * a different version of Viash.
    * 
    * If the version is set to "-", the internal CLI will
    * be used.
    * 
    * For testing purposes, exceptions are not handled by
    * this function but are instead handled by the `main()`
    * function.
    *
    * @param args The command line arguments
    * @return An exit code
    */
  def mainCLIOrVersioned(args: Array[String]): Int = {
      val workingDir = Paths.get(System.getProperty("user.dir"))

      val viashVersion = detectVersion(Some(workingDir))
      
      viashVersion match {
        // don't use `mainVersioned()` if the version is the same
        // as this Viash or if the variable is explicitly set to `-`.
        case Some(version) if version != "-" && version != Main.version =>
          mainVersioned(args, Some(workingDir), version)
        case _ =>
          mainCLI(args, workingDir = Some(workingDir))
        }
  }

  /**
    * Run a specific version of Viash
    *
    * @param args The command line arguments
    * @param workingDir The directory in which Viash was called
    * @param version Which versions of Viash to run
    * @return An exit code
    */
  def mainVersioned(args: Array[String], workingDir: Option[Path] = None, version: String): Int = {
    val path = Paths.get(SysEnv.viashHome).resolve("releases").resolve(version).resolve("viash")

    if (!Files.exists(path)) {
      // todo: be able to use 0.7.x notation to get the latest 0.7 version?
      // todo: allow 'latest' and '@branch' notation to build viash?
      // todo: throw nicer error when release does not exist
      val uri = new URI(s"https://github.com/viash-io/viash/releases/download/$version/viash")
      val parent = path.getParent()
      if (!Files.exists(parent)) {
        Files.createDirectories(parent)
      }
      IO.write(uri, path, true, Some(true))
    }
   
    Process(
      Array(path.toString) ++ args,
      cwd = workingDir.map(_.toFile),
      extraEnv = List("VIASH_VERSION" -> "-"): _*
    ).!(ProcessLogger(s => infoOut(s), s => info(s)))
  }

  /**
    * Viash CLI handler
    * 
    * This function processes the command-line arguments using a scallop CLI and
    * passes the right values to the Viash command objects.
    *
    * @param args The command line arguments
    * @param workingDir The directory in which Viash was called
    * @return An exit code
    */
  def mainCLI(args: Array[String], workingDir: Option[Path] = None): Int = {
    // try to find project settings
    val proj0 = workingDir.map(ViashProject.findViashProject(_)).getOrElse(ViashProject())

    // strip arguments meant for viash run
    val (viashArgs, runArgs) = {
        if (args.length > 0 && args(0) == "run") {
          args.span(_ != "--")
        } else {
          (args, Array[String]())
        }
    }

    // parse arguments
    val cli = new CLIConf(viashArgs.toIndexedSeq) 

    // Set Logger paramters
    cli.subcommands.last match {
      case x: ViashLogger => 
        if (x.colorize.isDefined) {
          val colorize = x.colorize() match {
            case "auto" => None
            case "true" => Some(true)
            case "false" => Some(false)
          }
          Logger.UseColorOverride.value = colorize
        }
        if (x.loglevel.isDefined) {
          Logger.UseLevelOverride.value = LoggerLevel.fromString(x.loglevel())
        }
      case _ => 
    }
    
    // see if there are project overrides passed to the viash command
    val projSrc = cli.subcommands.last match {
      case x: ViashNs => x.src.toOption
      case _ => None
    }
    val projTarg = cli.subcommands.last match {
      case x: ViashNsBuild => x.target.toOption
      case _ => None
    }
    val projCm = cli.subcommands.last match {
      case x: ViashNs => x.config_mods()
      case x: ViashCommand => x.config_mods()
      case _ => Nil
    }

    val proj1 = proj0.copy(
      source = projSrc orElse proj0.source orElse Some("src"),
      target = projTarg orElse proj0.target orElse Some("target"),
      config_mods = proj0.config_mods ::: projCm
    )

    // process commands
    cli.subcommands match {
      case List(cli.run) =>
        val config = readConfig(cli.run, project = proj1)
        ViashRun(
          appliedConfig = config,
          args = runArgs.toIndexedSeq.dropWhile(_ == "--"), 
          keepFiles = cli.run.keep.toOption.map(_.toBoolean),
          cpus = cli.run.cpus.toOption,
          memory = cli.run.memory.toOption
        )
      case List(cli.build) =>
        val config = readConfig(cli.build, project = proj1)
        val config2 = singleConfigDependencies(config, cli.build.output.toOption, proj1.rootDir)
        val buildResult = ViashBuild(
          appliedConfig = config2,
          output = cli.build.output(),
          setup = cli.build.setup.toOption,
          push = cli.build.push()
        )
        if (buildResult.isError) 1 else 0
      case List(cli.test) =>
        val config = readConfig(cli.test, project = proj1)
        val config2 = singleConfigDependencies(config, None, proj1.rootDir)
        ViashTest(
          config2,
          keepFiles = cli.test.keep.toOption.map(_.toBoolean),
          setupStrategy = cli.test.setup.toOption,
          cpus = cli.test.cpus.toOption,
          memory = cli.test.memory.toOption
        )
        0 // Exceptions are thrown when a test fails, so then the '0' is not returned but a '1'. Can be improved further.
      case List(cli.namespace, cli.namespace.build) =>
        val configs = readConfigs(cli.namespace.build, project = proj1)
        val configs2 = namespaceDependencies(configs, proj1.target, proj1.rootDir)
        var buildResults = ViashNamespace.build(
          configs = configs2,
          target = proj1.target.get,
          setup = cli.namespace.build.setup.toOption,
          push = cli.namespace.build.push(),
          parallel = cli.namespace.build.parallel(),
          flatten = cli.namespace.build.flatten()
        )
        val errors = buildResults
          .map(_.status.getOrElse(Success))
          .count(_.isError)
        if (errors > 0) 1 else 0
      case List(cli.namespace, cli.namespace.test) =>
        val configs = readConfigs(cli.namespace.test, project = proj1)
        // resolve dependencies
        val configs2 = namespaceDependencies(configs, None, proj1.rootDir)
        // flatten engines
        val configs3 = configs2.flatMap{ ac => 
          ac.engines.map{ engine => 
            ac.copy(engines = List(engine))
          }
        }
        val testResults = ViashNamespace.test(
          configs = configs3,
          parallel = cli.namespace.test.parallel(),
          keepFiles = cli.namespace.test.keep.toOption.map(_.toBoolean),
          tsv = cli.namespace.test.tsv.toOption,
          append = cli.namespace.test.append(),
          cpus = cli.namespace.test.cpus.toOption,
          memory = cli.namespace.test.memory.toOption,
          setup = cli.namespace.test.setup.toOption,
        )
        val errors = testResults.map(_._1).flatMap(_.status).count(_.isError)
        if (errors > 0) 1 else 0
      case List(cli.namespace, cli.namespace.list) =>
        val configs = readConfigs(
          cli.namespace.list,
          project = proj1,
          addOptMainScript = false, 
          applyRunner = cli.namespace.list.runner.isDefined,
          applyEngine = cli.namespace.list.engine.isDefined
        )
        val configs2 = namespaceDependencies(configs, None, proj1.rootDir)
        ViashNamespace.list(
          configs = configs2,
          format = cli.namespace.list.format(),
          parseArgumentGroups = cli.namespace.list.parse_argument_groups()
        )
        val errors = configs.flatMap(_.status).count(_.isError)
        if (errors > 0) 1 else 0
      case List(cli.namespace, cli.namespace.exec) =>
        val configs = readConfigs(
          cli.namespace.exec, 
          project = proj1, 
          applyRunner = cli.namespace.exec.applyRunner(),
          applyEngine = cli.namespace.exec.applyEngine()
        )
        ViashNamespace.exec(
          configs = configs,
          command = cli.namespace.exec.cmd(),
          dryrun = cli.namespace.exec.dryrun(),
          parallel = cli.namespace.exec.parallel()
        )
        val errors = configs.flatMap(_.status).count(_.isError)
        if (errors > 0) 1 else 0
      case List(cli.config, cli.config.view) =>
        val config = readConfig(
          cli.config.view,
          project = proj1,
          addOptMainScript = false,
          applyRunnerAndEngine = cli.config.view.runner.isDefined || cli.config.view.engine.isDefined
        )
        val config2 = DependencyResolver.modifyConfig(config.config, None, proj1.rootDir)
        ViashConfig.view(
          config2, 
          format = cli.config.view.format(),
          parseArgumentGroups = cli.config.view.parse_argument_groups()
        )
        0
      case List(cli.config, cli.config.inject) =>
        val config = readConfig(
          cli.config.inject,
          project = proj1,
          addOptMainScript = false,
          applyRunnerAndEngine = false
        )
        ViashConfig.inject(config.config)
        0
      case List(cli.export, cli.export.cli_schema) =>
        val output = cli.export.cli_schema.output.toOption.map(Paths.get(_))
        ViashExport.exportCLISchema(
          output,
          format = cli.export.cli_schema.format()
        )
        0
      case List(cli.export, cli.export.cli_autocomplete) =>
        val output = cli.export.cli_autocomplete.output.toOption.map(Paths.get(_))
        ViashExport.exportAutocomplete(
          output,
          format = cli.export.cli_autocomplete.format()
        )
        0
      case List(cli.export, cli.export.config_schema) =>
        val output = cli.export.config_schema.output.toOption.map(Paths.get(_))
        ViashExport.exportConfigSchema(
          output,
          format = cli.export.config_schema.format()
        )
        0
      case List(cli.export, cli.export.json_schema) =>
        val output = cli.export.json_schema.output.toOption.map(Paths.get(_))
        ViashExport.exportJsonSchema(
          output,
          format = cli.export.json_schema.format()
        )
        0
      case List(cli.export, cli.export.resource) =>
        val output = cli.export.resource.output.toOption.map(Paths.get(_))
        ViashExport.exportResource(
          cli.export.resource.path.toOption.get,
          output
        )
        0
      case _ =>
        info("No subcommand was specified. See `viash --help` for more information.")
        1
    }
  }


  def processConfigWithRunnerAndEngine(
    appliedConfig: AppliedConfig, 
    runner: Option[Runner],
    engines: List[Engine],
    targetDir: Option[String]
  ): AppliedConfig = {
    // determine output path
    val outputPath = (targetDir, runner) match {
      case (Some(td), Some(ex)) => 
        Some(ViashNamespace.targetOutputPath(
          targetDir = td,
          runnerId = ex.id,
          namespace = appliedConfig.config.functionality.namespace,
          functionalityName = appliedConfig.config.functionality.name
        ))
      case _ => None
    }

    // add runner and engine ids to the info object
    val configInfo = appliedConfig.config.info.map{_.copy(
      runner = runner.map(_.id),
      engine = Some(engines.map(_.id).mkString("|")),
      output = outputPath
    )}

    // update info, and add runner and engine to the config
    appliedConfig.copy(
      config = appliedConfig.config.copy(
        info = configInfo
      ),
      runner = runner,
      engines = engines,
      status = None
    )
  }

  def readConfig(
    subcommand: ViashCommand,
    project: ViashProject,
    addOptMainScript: Boolean = true,
    applyRunnerAndEngine: Boolean = true
  ): AppliedConfig = {
    
    val config = Config.read(
      configPath = subcommand.config(),
      projectDir = project.rootDir.map(_.toUri()),
      addOptMainScript = addOptMainScript,
      configMods = project.config_mods
    )
    if (applyRunnerAndEngine) {
      val runnerStr = subcommand.runner.toOption
      val engineStr = subcommand.engine.toOption
      
      val runner = config.findRunner(runnerStr)
      val engines = config.findEngines(engineStr)

      processConfigWithRunnerAndEngine(
        appliedConfig = config,
        runner = Some(runner), // TODO: fix? should findRunner return an option?
        engines = engines,
        targetDir = project.target
      )
    } else {
      config
    }
  }
  
  def readConfigs(
    subcommand: ViashNs,
    project: ViashProject,
    addOptMainScript: Boolean = true,
    applyRunner: Boolean = true,
    applyEngine: Boolean = true
  ): List[AppliedConfig] = {
    val source = project.source.get
    val query = subcommand.query.toOption
    val queryNamespace = subcommand.query_namespace.toOption
    val queryName = subcommand.query_name.toOption
    val runnerStr = subcommand.runner.toOption
    val engineStr = subcommand.engine.toOption
    val configMods = project.config_mods

    val configs0 = Config.readConfigs(
      source = source,
      projectDir = project.rootDir.map(_.toUri()),
      query = query,
      queryNamespace = queryNamespace,
      queryName = queryName,
      configMods = configMods,
      addOptMainScript = addOptMainScript
    )
    
    // TODO: apply engine and runner should probably be split into two Y_Y
    val configs1 = 
      if (applyRunner || applyEngine) {
        configs0.flatMap {
          // passthrough statuses
          case ac if ac.status.isDefined => List(ac)
          case ac =>
            try {
              val runners = ac.config.findRunners(runnerStr)
              val engines = ac.config.findEngines(engineStr)

              runners.map{ runner =>
                processConfigWithRunnerAndEngine(
                  appliedConfig = ac,
                  runner = Some(runner),
                  engines = engines,
                  targetDir = project.target
                )
              }
            } catch {
              case e: Exception =>
                error(e.getMessage())
                List(ac.setStatus(MissingRunnerOrEngine))
            }
          }
      } else {
        configs0
      }
    
      configs1
  }

  // Handle dependencies operations for a single config
  def singleConfigDependencies(appliedConfig: AppliedConfig, output: Option[String], rootDir: Option[Path]): AppliedConfig = {
    if (output.isDefined)
      DependencyResolver.createBuildYaml(output.get)

    handleSingleConfigDependency(appliedConfig, output, rootDir)
  }

  // Handle dependency operations for namespaces
  def namespaceDependencies(configs: List[AppliedConfig], target: Option[String], rootDir: Option[Path]): List[AppliedConfig] = {
    if (target.isDefined)
      DependencyResolver.createBuildYaml(target.get)
    
    configs.map{
      case ac if ac.status.isDefined => ac
      case appliedConfig => {
        Try{
          val validConfigs = configs.map(_.config)
          handleSingleConfigDependency(appliedConfig, target, rootDir, validConfigs)
        }.fold(
          e => e match {
            case de: AbstractDependencyException =>
              error(s"Config \"${appliedConfig.config.functionality.name}\": ${e.getMessage}")
              appliedConfig.setStatus(DependencyError)
            case _ => throw e
          },
          ac => ac
        )
      }
    }
  }

  // Actual handling of the dependency logic, to be used for single and namespace configs
  def handleSingleConfigDependency(config: AppliedConfig, output: Option[String], rootDir: Option[Path], namespaceConfigs: List[Config] = Nil) = {
    val dependencyRunnerId = DependencyResolver.getDependencyRunnerId(config.config, config.runner.map(_.id))
    val config1 = DependencyResolver.modifyConfig(config.config, dependencyRunnerId, rootDir, namespaceConfigs)
    val config2 = if (output.isDefined) {
      DependencyResolver.copyDependencies(config1, output.get, dependencyRunnerId.getOrElse("executable"))
    } else {
      config1
    }
    config.copy(config = config2)
  }


  /**
    * Detect the desired Viash version
    * 
    * If an environment variable `VIASH_VERSION` is
    * defined, return its value. Else if a project file
    * is found in the working directory, check whether
    * it contains a `viash_version` field. Otherwise
    * return None. 
    *
    * @param workingDir The directory in which Viash was called
    * @return The desired version of Viash (if specified)
    */
  def detectVersion(workingDir: Option[Path]): Option[String] = {
    // if VIASH_VERSION is defined, use that
    SysEnv.viashVersion orElse {
      // else look for project file in working dir
      // and try to read as json
      workingDir
        .flatMap(ViashProject.findProjectFile)
        .map(ViashProject.readJson)
        .flatMap(js => {
          js.asObject.flatMap(_.apply("viash_version")).flatMap(_.asString)
        })
    }
  }
  
}
