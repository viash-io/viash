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
import java.net.{URI, HttpURLConnection}
import sys.process.{Process, ProcessLogger}

import config.Config
import helpers.{IO, Exec, SysEnv, DependencyResolver, Logger, Logging}
import helpers.status._
import packageConfig.PackageConfig
import cli.{CLIConf, ViashCommand, DocumentedSubcommand, ViashNs, ViashNsBuild, ViashLogger}
import exceptions._
import scala.util.Try
import org.rogach.scallop._
import io.viash.helpers.LoggerLevel
import io.viash.runners.Runner
import io.viash.config.AppliedConfig
import io.viash.engines.Engine
import io.viash.helpers.data_structures.*
import java.io.{BufferedOutputStream, FileOutputStream}
import io.viash.helpers.DependencyResolver.findRemoteConfig
import sys.process._
import scala.jdk.CollectionConverters._
import scala.io.Source
import io.viash.helpers.autonetconfig.AutoNetConfigStruct

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
      case e @ ( _: FileNotFoundException | _: MissingResourceFileException | _: ConfigModException ) =>
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
    // try to find package settings
    val pack0 = workingDir.map(PackageConfig.findViashPackage(_)).getOrElse(PackageConfig())

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
    cli.subcommands.lastOption match {
      case Some(x: ViashLogger) => 
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

    // backwards compability for --platform
    cli.subcommands.lastOption match {
      case Some(x: ViashCommand) => 
        if (x.platform.isDefined) {
          if (x.runner.isDefined || x.engine.isDefined) {
            throw new IllegalArgumentException("Error: --platform cannot be used together with --runner or --engine.")
          }
          warn("Warning: --platform is deprecated in Viash 0.9.0, will be removed in Viash 0.10.0. Use --runner or --engine instead.")
        }
      case Some(x: ViashNs) =>
        if (x.platform.isDefined) {
          if (x.runner.isDefined || x.engine.isDefined) {
            throw new IllegalArgumentException("Error: --platform cannot be used together with --runner or --engine.")
          }
          warn("Warning: --platform is deprecated in Viash 0.9.0, will be removed in Viash 0.10.0. Use --runner or --engine instead.")
        }
      case _ => 
    }
    // backwards compability for --apply_platform
    cli.subcommands match {
      case List(cli.namespace, cli.namespace.exec) =>
        if (cli.namespace.exec.applyPlatform()) {
          if (cli.namespace.exec.applyRunner() || cli.namespace.exec.applyEngine()) {
            throw new IllegalArgumentException("Error: --platform cannot be used together with --runner or --engine.")
          }
          warn("Warning: --apply_platform is deprecated in Viash 0.9.0, will be removed in Viash 0.10.0n. Use --apply_runner or --apply_engine instead.")
        }
      case _ =>
    }
    
    // see if there are package overrides passed to the viash command
    val packSrc = cli.subcommands.lastOption match {
      case Some(x: ViashNs) => x.src.toOption
      case _ => None
    }
    val packTarg = cli.subcommands.lastOption match {
      case Some(x: ViashNsBuild) => x.target.toOption
      case _ => None
    }
    val packCm = cli.subcommands.lastOption match {
      case Some(x: ViashNs) => x.config_mods()
      case Some(x: ViashCommand) => x.config_mods()
      case _ => Nil
    }

    val pack1 = pack0.copy(
      source = packSrc orElse pack0.source orElse Some("src"),
      target = packTarg orElse pack0.target orElse Some("target"),
      config_mods = pack0.config_mods ::: packCm
    )

    // process commands
    cli.subcommands match {
      case List(cli.run) =>
        val config = readConfig(cli.run, packageConfig = pack1)
        ViashRun(
          appliedConfig = config,
          args = runArgs.toIndexedSeq.dropWhile(_ == "--"), 
          keepFiles = cli.run.keep.toOption.map(_.toBoolean),
          cpus = cli.run.cpus.toOption,
          memory = cli.run.memory.toOption
        )
      case List(cli.build) =>
        val config = readConfig(cli.build, packageConfig = pack1)
        val config2 = singleConfigDependencies(config, cli.build.output.toOption, pack1.rootDir)
        val buildResult = ViashBuild(
          appliedConfig = config2,
          output = cli.build.output(),
          setup = cli.build.setup.toOption,
          push = cli.build.push()
        )
        if (buildResult.isError) 1 else 0
      case List(cli.test) =>
        val config = readConfig(cli.test, packageConfig = pack1)
        val config2 = singleConfigDependencies(config, None, pack1.rootDir)
        if (cli.test.dryRun.getOrElse(false) && !cli.test.keep.toOption.map(_.toBoolean).getOrElse(false)) {
          info("Warning: --dry_run is set, but --keep is not set. This will result in the generated files being deleted after the test.")
        }
        ViashTest(
          config2,
          keepFiles = cli.test.keep.toOption.map(_.toBoolean),
          setupStrategy = cli.test.setup.toOption,
          cpus = cli.test.cpus.toOption,
          memory = cli.test.memory.toOption,
          dryRun = cli.test.dryRun.toOption,
          deterministicWorkingDirectory = cli.test.deterministicWorkingDirectory.toOption
        )
        0 // Exceptions are thrown when a test fails, so then the '0' is not returned but a '1'. Can be improved further.
      case List(cli.namespace, cli.namespace.build) =>
        val configs = readConfigs(cli.namespace.build, packageConfig = pack1)
        val configs2 = namespaceDependencies(configs, pack1.target, pack1.rootDir)
        var buildResults = ViashNamespace.build(
          configs = configs2,
          target = pack1.target.get,
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
        val configs = readConfigs(cli.namespace.test, packageConfig = pack1)
        // resolve dependencies
        val configs2 = namespaceDependencies(configs, None, pack1.rootDir)
        // flatten engines
        val configs3 = configs2.flatMap{ ac => 
          ac.engines.map{ engine => 
            ac.copy(engines = List(engine))
          }
        }
        if (cli.namespace.test.dryRun.getOrElse(false) && !cli.namespace.test.keep.toOption.map(_.toBoolean).getOrElse(false)) {
          info("Warning: --dry_run is set, but --keep is not set. This will result in the generated files being deleted after the test.")
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
          dryRun = cli.namespace.test.dryRun.toOption,
          deterministicWorkingDirectory = cli.namespace.test.deterministicWorkingDirectory.toOption,
        )
        val errors = testResults.map(_._1).flatMap(_.status).count(_.isError)
        if (errors > 0) 1 else 0
      case List(cli.namespace, cli.namespace.list) =>
        if (cli.namespace.list.parse_argument_groups()) {
          info("Warning: --parse_argument_groups is deprecated and effectively always enabled.")
        }
        val configs = readConfigs(
          cli.namespace.list,
          packageConfig = pack1,
          addOptMainScript = false, 
          applyRunner = cli.namespace.list.runner.isDefined || cli.namespace.list.platform.isDefined,
          applyEngine = cli.namespace.list.engine.isDefined || cli.namespace.list.platform.isDefined
        )
        val configs2 = namespaceDependencies(configs, None, pack1.rootDir)
        ViashNamespace.list(
          configs = configs2,
          format = cli.namespace.list.format()
        )
        val errors = configs.flatMap(_.status).count(_.isError)
        if (errors > 0) 1 else 0
      case List(cli.namespace, cli.namespace.exec) =>
        val configs = readConfigs(
          cli.namespace.exec, 
          packageConfig = pack1, 
          applyRunner = cli.namespace.exec.applyRunner() || cli.namespace.exec.applyPlatform(),
          applyEngine = cli.namespace.exec.applyEngine() || cli.namespace.exec.applyPlatform()
        )
        ViashNamespace.exec(
          configs = configs,
          command = cli.namespace.exec.cmd(),
          dryrun = cli.namespace.exec.dryrun(),
          parallel = cli.namespace.exec.parallel(),
          workingDir = workingDir,
        )
        val errors = configs.flatMap(_.status).count(_.isError)
        if (errors > 0) 1 else 0
      case List(cli.config, cli.config.view) =>
        if (cli.config.view.parse_argument_groups()) {
          info("Warning: --parse_argument_groups is deprecated and effectively always enabled.")
        }
        val config = readConfig(
          cli.config.view,
          packageConfig = pack1,
          addOptMainScript = false,
          applyRunnerAndEngine = cli.config.view.platform.isDefined || cli.config.view.runner.isDefined || cli.config.view.engine.isDefined
        )
        val config2 = DependencyResolver.modifyConfig(config.config, None, pack1.rootDir)
        ViashConfig.view(
          config2, 
          format = cli.config.view.format()
        )
        0
      case List(cli.config, cli.config.inject) =>
        val config = readConfig(
          cli.config.inject,
          packageConfig = pack1,
          addOptMainScript = false,
          applyRunnerAndEngine = false
        )
        ViashConfig.inject(config.config)
        0
      case List(cli.`export`, cli.`export`.cli_schema) =>
        val output = cli.`export`.cli_schema.output.toOption.map(Paths.get(_))
        ViashExport.exportCLISchema(
          output,
          format = cli.`export`.cli_schema.format()
        )
        0
      case List(cli.`export`, cli.`export`.cli_autocomplete) =>
        val output = cli.`export`.cli_autocomplete.output.toOption.map(Paths.get(_))
        ViashExport.exportAutocomplete(
          output,
          format = cli.`export`.cli_autocomplete.format()
        )
        0
      case List(cli.`export`, cli.`export`.config_schema) =>
        val output = cli.`export`.config_schema.output.toOption.map(Paths.get(_))
        ViashExport.exportConfigSchema(
          output,
          format = cli.`export`.config_schema.format()
        )
        0
      case List(cli.`export`, cli.`export`.json_schema) =>
        val output = cli.`export`.json_schema.output.toOption.map(Paths.get(_))
        ViashExport.exportJsonSchema(
          output,
          format = cli.`export`.json_schema.format(),
          strict = cli.`export`.json_schema.strict(),
          minimal = cli.`export`.json_schema.minimal()
        )
        0
      case List(cli.`export`, cli.`export`.resource) =>
        val output = cli.`export`.resource.output.toOption.map(Paths.get(_))
        ViashExport.exportResource(
          cli.`export`.resource.path.toOption.get,
          output
        )
        0
      case _ =>
        error("Error: No subcommand was specified. See `viash --help` for more information.")
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
          config = appliedConfig.config
        ))
      case _ => None
    }

    // add runner and engine ids to the info object
    val configInfo = appliedConfig.config.build_info.map{_.copy(
      runner = runner.map(_.id),
      engine = Some(engines.map(_.id).mkString("|")),
      output = outputPath
    )}

    // update info, and add runner and engine to the config
    appliedConfig.copy(
      config = appliedConfig.config.copy(
        build_info = configInfo
      ),
      runner = runner,
      engines = engines,
      status = None
    )
  }

  def readConfig(
    subcommand: ViashCommand,
    packageConfig: PackageConfig,
    addOptMainScript: Boolean = true,
    applyRunnerAndEngine: Boolean = true
  ): AppliedConfig = {
    val packageBundleRegex = raw"vsh://(\w+)/([\w\-\.]+)/(.*)".r
    val configPath = subcommand.config() match {
      case packageBundleRegex(package_, version, component) =>
        fetchPackageBundle(package_, version, component)
      case str => str
    }
    
    val config = Config.read(
      configPath = configPath,
      addOptMainScript = addOptMainScript,
      viashPackage = Some(packageConfig)
    )
    if (applyRunnerAndEngine) {
      val runnerStr = subcommand.runner.toOption orElse subcommand.platform.toOption
      val engineStr = subcommand.engine.toOption orElse subcommand.platform.toOption
      
      val runner = config.findRunner(runnerStr)
      val engines = config.findEngines(engineStr)

      processConfigWithRunnerAndEngine(
        appliedConfig = config,
        runner = Some(runner), // TODO: fix? should findRunner return an option?
        engines = engines,
        targetDir = packageConfig.target
      )
    } else {
      config
    }
  }
  
  def readConfigs(
    subcommand: ViashNs,
    packageConfig: PackageConfig,
    addOptMainScript: Boolean = true,
    applyRunner: Boolean = true,
    applyEngine: Boolean = true
  ): List[AppliedConfig] = {
    val source = packageConfig.source.get
    val query = subcommand.query.toOption
    val queryNamespace = subcommand.query_namespace.toOption
    val queryName = subcommand.query_name.toOption
    val queryConfig = subcommand.query_config.toOption
    val runnerStr = subcommand.runner.toOption orElse subcommand.platform.toOption
    val engineStr = subcommand.engine.toOption orElse subcommand.platform.toOption
    val configMods = packageConfig.config_mods

    val configs0 = Config.readConfigs(
      source = source,
      query = query,
      queryNamespace = queryNamespace,
      queryName = queryName,
      queryConfig = queryConfig,
      addOptMainScript = addOptMainScript,
      viashPackage = Some(packageConfig)
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
                  targetDir = packageConfig.target
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
          val validConfigs = configs.filter(ac => ac.status == None || ac.status == Some(DisabledByQuery)).map(_.config)
          handleSingleConfigDependency(appliedConfig, target, rootDir, validConfigs)
        }.fold(
          e => e match {
            case de: AbstractDependencyException =>
              error(s"Config \"${appliedConfig.config.name}\": ${e.getMessage}")
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
    * defined, return its value. Else if a package file
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
      // else look for package file in working dir
      // and try to read as json
      workingDir
        .flatMap(PackageConfig.findPackageFile)
        .map(PackageConfig.readJson)
        .flatMap(js => {
          js.asObject.flatMap(_.apply("viash_version")).flatMap(_.asString)
        })
    }
  }

  /**
    * Fetch a package bundle from Viash Hub, always fetches the executable version
    *
    * @param package package name
    * @param version package version
    * @param component component name
    * @return the path to the config file matched 
    */
  def fetchPackageBundle(`package`: String, version: String, component: String): String = {
    debug(s"Fetching package bundle: ${`package`}/$version/$component")

    val cacheIdentifier = s"${`package`}-$version-$component-executable"
    debug(s"Cache identifier: $cacheIdentifier")
    val path = Paths.get(SysEnv.viashHome).resolve("package-bundle-cache")
    val tarballPath = path.resolve(s"$cacheIdentifier.tar.gz")
    val etag_path = path.resolve(s"$cacheIdentifier.etag")
    val dirPath = path.resolve(cacheIdentifier)

    val etag = Try(Source.fromFile(etag_path.toString).getLines().next()).toOption

    val anc = AutoNetConfigStruct.fetch("api-dev.viash-hub.dev")
    if (anc.isEmpty) {
      throw new RuntimeException("Error: Could not fetch ANC")
    }
    val hosts = anc.get.hosts
    val protocol = hosts.back_protocol
    val host = hosts.back

    val uri = new URI(s"$protocol://$host/package-bundle/${`package`}/$version/$component?runner=executable")
    infoOut(s"Fetching package bundle from: $uri")
    
    val url = uri.toURL()

    val connection = url.openConnection().asInstanceOf[HttpURLConnection]
    connection.setConnectTimeout(5000)
    connection.setReadTimeout(60000)
    if (etag.isDefined)
      connection.setRequestProperty("If-None-Match", etag.get)
    connection.connect()

    if (connection.getResponseCode >= 400) {
      errorOut(s"Error downloading $uri")
      throw new RuntimeException(s"Error: ${connection.getResponseCode()} ${connection.getResponseMessage()}")
    }
    else if (connection.getResponseCode() == 304)
      infoOut("Not modified")
    else {
      val headers = connection.getHeaderFields()
      val headersScala = headers.asScala
      debug(s"Headers: $headers")
      val etag = headersScala.get("etag").flatMap(_.asScala.headOption)
      debug(s"ETag: $etag")

      // save tarball
      (url #> tarballPath.toFile()).!!

      // save etag
      if (etag.isDefined) {
        val out = new BufferedOutputStream(new FileOutputStream(etag_path.toFile))
        out.write(etag.get.getBytes())
        out.close()
      }

      if (dirPath.toFile().exists()) {
        debug(s"Removing old cache directory: $dirPath")
        IO.deleteRecursively(dirPath)
      }

      // extract the tarball using library
      debug(s"Extract package bundle to: $dirPath")
      val cmd = Array("tar", "-xzf", tarballPath.toString, "-C", path.toString)
      Process(cmd).!!
    }
    
    val config = findRemoteConfig(dirPath.toString(), component, Some("executable"))
    infoOut(s"Config file: $config")

    config match {
      case Some(c) => c._1
      case None => throw new RuntimeException("Error: Could not find config file in package bundle")
    }
  }
}
