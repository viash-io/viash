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

package io.viash.config

import io.viash.config_mods.ConfigModParser
import io.viash.functionality._
import io.viash.platforms.Platform
import io.viash.helpers.{Git, GitInfo, IO, Logging}
import io.viash.helpers.circe._
import io.viash.helpers.status._
import io.viash.helpers.Yaml

import java.net.URI
import io.viash.functionality.resources._

import java.io.File
import io.viash.config_mods.ConfigMods
import java.nio.file.Paths

import io.viash.schemas._
import java.io.ByteArrayOutputStream
import java.nio.file.FileSystemNotFoundException
import io.viash.runners.{Runner, ExecutableRunner, NextflowRunner}
import io.viash.engines.{Engine, NativeEngine, DockerEngine}
import io.viash.helpers.ReplayableMultiOutputStream
import io.viash.packageConfig.PackageConfig
import io.viash.lenses.ConfigLenses._

@description(
  """A Viash configuration is a YAML file which contains metadata to describe the behaviour and build target(s) of a component.  
    |We commonly name this file `config.vsh.yaml` in our examples, but you can name it however you choose.  
    |""".stripMargin)
@example(
  """functionality:
    |  name: hello_world
    |  arguments:
    |    - type: string
    |      name: --input
    |      default: "world"
    |  resources:
    |    - type: bash_script
    |      path: script.sh
    |      text: echo Hello $par_input
    |runners:
    |  - type: executable
    |engines:
    |  - type: docker
    |    image: "bash:4.0"
    |""".stripMargin, "yaml")
case class Config(
  @description(
    """The @[functionality](functionality) describes the behaviour of the script in terms of arguments and resources.
      |By specifying a few restrictions (e.g. mandatory arguments) and adding some descriptions, Viash will automatically generate a stylish command-line interface for you.
      |""".stripMargin)
  functionality: Functionality,

  @description(
    """A list of runners to execute target artifacts.
      |
      | - @[ExecutableRunner](executable_runner)
      | - @[NextflowRunner](nextflow_runner)
      |""".stripMargin)
  @since("Viash 0.8.0")
  @default("Empty")
  runners: List[Runner] = Nil,
  @description(
    """A list of engine environments to execute target artifacts in.
      |
      | - @[NativeEngine](native_engine)
      | - @[DockerEngine](docker_engine)
      |""".stripMargin)
  @since("Viash 0.8.0")
  @default("Empty")
  engines: List[Engine] = Nil,

  @undocumented
  info: Option[Info] = None,

  @description("The package config content used during build.")
  @since("Viash 0.9.0")
  @undocumented
  package_config: Option[PackageConfig] = None,
) {

  @description(
    """Config inheritance by including YAML partials. This is useful for defining common APIs in
      |separate files. `__merge__` can be used in any level of the YAML. For example,
      |not just in the config but also in the functionality or any of the engines.
      |""".stripMargin)
  @example("__merge__: ../api/common_interface.yaml", "yaml")
  @since("Viash 0.6.3")
  private val `__merge__`: Option[File] = None

  @description(
  """A list of platforms to generate target artifacts for.
    |
    | - @[Native](platform_native)
    | - @[Docker](platform_docker)
    | - @[Nextflow](platform_nextflow)
    |""".stripMargin)
  @default("Empty")
  @deprecated("Use 'engines' and 'runners' instead.", "0.9.0", "0.10.0")
  private val platforms: List[Platform] = Nil
  
  /**
    * Find the runner
    * 
    * Order of execution:
    *   - if an runner id is passed, look up the runner in the runners list
    *   - else if runners is a non-empty list, use the first runner
    *   - else use the executable runner
    *
    * @param runnerStr An runner ID referring to one of the config's runners
    * @return An runner
    */
  def findRunner(query: Option[String]): Runner = {
    findRunners(query).head
  }

  /**
    * Find the runners
    * 
    * Order of execution:
    *   - if an runner id is passed, look up the runner in the runners list
    *   - else if runners is a non-empty list, use the first runner
    *   - else use the executable runner
    *
    * @param query An runner ID referring to one of the config's runners
    * @return An runner
    */
  def findRunners(query: Option[String]): List[Runner] = {
    // TODO: match on query, there's no need to do a .* if query is None
    val regex = query.getOrElse(".*").r

    val foundMatches = runners.filter{ e =>
      regex.findFirstIn(e.id).isDefined
    }
    
    foundMatches match {
      case li if li.nonEmpty =>
        li
      case Nil if query.isDefined =>
        throw new RuntimeException(s"no runner id matching regex '$regex' could not be found in the config.")
      case _ =>
        // TODO: switch to the getRunners.head ?
        List(ExecutableRunner())
    }
  }

  /**
    * Find the engines
    * 
    * Order of execution:
    *   - if an engine id is passed, look up the engine in the engines list
    *   - else if engines is a non-empty list, use the first engine
    *   - else use the executable engine
    *
    * @param query An engine ID referring to one of the config's engines
    * @return An engine
    */
  def findEngines(query: Option[String]): List[Engine] = {
    // TODO: match on query, there's no need to do a .* if query is None
    val regex = query.getOrElse(".*").r

    val foundMatches = engines.filter{ e =>
      regex.findFirstIn(e.id).isDefined
    }
    
    foundMatches match {
      case li if li.nonEmpty =>
        li
      case Nil if query.isDefined =>
        throw new RuntimeException(s"no engine id matching regex '$regex' could not be found in the config.")
      case _ =>
        // TODO: switch to the getEngines.head ?
        List(NativeEngine())
    }
  }
}

object Config extends Logging {
  def readYAML(config: String): (String, Option[Script]) = {
    val configUri = IO.uri(config)
    readYAML(configUri)
  }

  def readYAML(configUri: URI): (String, Option[Script]) = {
    // get config text
    val configStr = IO.read(configUri)
    val configUriStr = configUri.toString

    // get extension
    val extension = configUriStr.substring(configUriStr.lastIndexOf(".") + 1).toLowerCase()

    // get basename
    val basenameRegex = ".*/".r
    val basename = basenameRegex.replaceFirstIn(configUriStr, "")

    // detect whether a script (with joined header) was passed or a joined yaml
    // using the extension
    if ((extension == "yml" || extension == "yaml") && configStr.contains("functionality:")) {
      (configStr, None)
    } else if (Script.extensions.contains(extension)) {
      // detect scripting language from extension
      val scriptObj = Script.fromExt(extension)

      // check whether viash header contains a functionality
      val commentStr = scriptObj.commentStr + "'"
      val headerComm = commentStr + " "
      assert(
        configStr.contains(s"$commentStr functionality:"),
        message = s"""viash script should contain a functionality header: "$commentStr functionality: <...>""""
      )

      // split header and body
      val (header, body) = configStr.split("\n").partition(_.startsWith(headerComm))
      val yaml = header.map(s => s.drop(3)).mkString("\n")
      val code = body.mkString("\n")

      val script = Script(dest = Some(basename), text = Some(code), `type` = scriptObj.`type`)

      (yaml, Some(script))
    } else {
      throw new RuntimeException("config file (" + configUri + ") must be a yaml file containing a viash config.")
    }
  }

  // reads and modifies the config based on the current setup
  def read(
    configPath: String,
    addOptMainScript: Boolean = true,
    viashPackage: Option[PackageConfig] = None,
  ): Config = {
    val uri = IO.uri(configPath)
    readFromUri(
      uri = uri,
      addOptMainScript = addOptMainScript,
      viashPackage = viashPackage
    )
  }

  def readFromUri(
    uri: URI,
    addOptMainScript: Boolean = true,
    viashPackage: Option[PackageConfig] = None,
  ): Config = {
    val packageDir = viashPackage.flatMap(_.rootDir).map(_.toUri())
    val configMods: List[String] = viashPackage.map(_.config_mods.toList).getOrElse(Nil)

    // read cli config mods
    val confMods = ConfigMods.parseConfigMods(configMods)

    /* STRING */
    // read yaml as string
    val (yamlText, optScript) = readYAML(uri)

    // replace valid yaml definitions for +.inf with "+.inf" so that circe doesn't trip over its toes
    val replacedYamlText = Yaml.replaceInfinities(yamlText)
    
    /* JSON 0: parsed from string */
    // parse yaml into Json
    val json0 = Convert.textToJson(replacedYamlText, uri.toString())

    /* JSON 1: after inheritance */
    // apply inheritance if need be
    val json1 = json0.inherit(uri, packageDir)

    /* JSON 2: after preparse config mods  */
    // apply preparse config mods if need be
    val json2 = confMods(json1, preparse = true)

    /* CONFIG Base: converted from json */
    // convert Json into Config
    val confBase = Convert.jsonToClass[Config](json2, uri.toString())

    /* CONFIG 0: apply values from package config */
    // apply values from package config if need be
    val conf0 = {
      val vpVersion = viashPackage.flatMap(_.version)
      val vpRepositories = viashPackage.map(_.repositories).getOrElse(Nil)
      val vpLicense = viashPackage.flatMap(_.license)
      val vpOrganization = viashPackage.flatMap(_.organization)
      val vpRepository = viashPackage.flatMap(_.links.repository)
      val vpDockerRegistry = viashPackage.flatMap(_.links.docker_registry)

      val lenses =
        composedVersionLens.modify(_ orElse vpVersion) andThen
        composedLicenseLens.modify(_ orElse vpLicense) andThen
        composedOrganizationLens.modify(_ orElse vpOrganization) andThen
        composedRepositoriesLens.modify(vpRepositories ::: _) andThen
        composedLinksRepositoryLens.modify(_ orElse vpRepository) andThen
        composedLinksDockerRegistryLens.modify(_ orElse vpDockerRegistry)
        
      lenses(confBase)
    }

    /* CONFIG 1: apply post-parse config mods */
    // apply config mods only if need be
    val conf1 = 
      if (confMods.postparseCommands.nonEmpty) {
        // turn config back into json
        val js = encodeConfig(conf0)
        // apply config mods
        val modifiedJs = confMods(js, preparse = false)
        // turn json back into a config
        Convert.jsonToClass[Config](modifiedJs, uri.toString())
      } else {
        conf0
      }

    /* CONFIG 2: store parent path in resource to be able to access them in the future */
    // copy resources with updated paths into config and return
    val parentURI = uri.resolve("")
    val resources = conf1.functionality.resources.map(_.copyWithAbsolutePath(parentURI, packageDir))
    val tests = conf1.functionality.test_resources.map(_.copyWithAbsolutePath(parentURI, packageDir))

    val conf2a = composedResourcesLens.set(resources)(conf1)
    val conf2 = composedTestResourcesLens.set(tests)(conf2a)

    /* CONFIG 3: add info */
    // gather git info
    // todo: resolve git in package?
    val conf3 = uri.getScheme() match {
      case "file" =>
        val path = Paths.get(uri).toFile().getParentFile
        val GitInfo(_, rgr, gc, gt) = Git.getInfo(path)

        // create info object
        val info = 
          Info(
            viash_version = Some(io.viash.Main.version),
            config = uri.toString.replaceAll("^file:/+", "/"),
            git_commit = gc,
            git_remote = rgr,
            git_tag = gt
          )
        // add info and additional resources
        conf2.copy(
          info = Some(info),
        )
      case _ => conf2
    }

    // print warnings if need be
    if (conf3.functionality.status == Status.Deprecated)
      warn(s"Warning: The status of the component '${conf3.functionality.name}' is set to deprecated.")
    
    if (conf3.functionality.resources.isEmpty && optScript.isEmpty)
      warn(s"Warning: no resources specified!")

    /* CONFIG 4: add Viash Package Config */
    val conf4 = conf3.copy(
      package_config = viashPackage
    )

    if (!addOptMainScript) {
      return conf4
    }
    
    /* CONFIG 5: add main script if config is stored inside script */
    // add info and additional resources
    val conf5 = composedResourcesLens.modify(optScript.toList ::: _)(conf4)

    conf5
  }

  def readConfigs(
    source: String,
    query: Option[String] = None,
    queryNamespace: Option[String] = None,
    queryName: Option[String] = None,
    addOptMainScript: Boolean = true,
    viashPackage: Option[PackageConfig] = None,
  ): List[AppliedConfig] = {

    val sourceDir = Paths.get(source)

    // find [^\.]*.vsh.* files and parse as config
    val scriptFiles = IO.find(sourceDir, (path, attrs) => {
      path.toString.contains(".vsh.") &&
        !path.toFile.getName.startsWith(".") &&
        attrs.isRegularFile
    })

    scriptFiles.map { file =>
      try {
        val rmos = new ReplayableMultiOutputStream()

        val appliedConfig: AppliedConfig = 
          // Don't output any warnings now. Only output warnings if the config passes the regex checks.
          // When replaying, output directly to the console. Logger functions were already called, so we don't want to call them again.
          Console.withErr(rmos.getOutputStream(Console.err.print)) {
            Console.withOut(rmos.getOutputStream(Console.out.print)) {
              read(
                file.toString,
                addOptMainScript = addOptMainScript,
                viashPackage = viashPackage
              )
            }
          }

        val funName = appliedConfig.config.functionality.name
        val funNs = appliedConfig.config.functionality.namespace
        val isEnabled = appliedConfig.config.functionality.isEnabled

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

        if (!isEnabled) {
          appliedConfig.setStatus(Disabled)
        } else if (queryTest && nameTest && namespaceTest) {
          // if config passes regex checks, show warnings if there are any
          rmos.replay()
          appliedConfig
        } else {
          appliedConfig.setStatus(DisabledByQuery)
        }
      } catch {
        case _: Exception =>
          error(s"Reading file '$file' failed")
          AppliedConfig(Config(Functionality("failed")), None, Nil, Some(ParseError))
      }
    }
  }
}
