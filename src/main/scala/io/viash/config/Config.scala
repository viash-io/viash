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
import io.viash.platforms._
import io.viash.helpers.{Git, GitInfo, IO, Logging}
import io.viash.helpers.circe._
import io.viash.helpers.status._
import io.viash.helpers.Yaml
import io.viash.executors.Executor
import io.viash.engines.Engine

import java.net.URI
import io.viash.functionality.resources._

import java.io.File
import io.viash.config_mods.ConfigMods
import java.nio.file.Paths

import io.viash.schemas._
import java.io.ByteArrayOutputStream
import java.nio.file.FileSystemNotFoundException
import io.viash.executors.ExecutableExecutor
import io.viash.engines.NativeEngine
import io.viash.engines.DockerEngine

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
    |platforms:
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
    """A list of platforms to generate target artifacts for.
      |
      | - @[Native](platform_native)
      | - @[Docker](platform_docker)
      | - @[Nextflow](platform_nextflow)
      |""".stripMargin)
  platforms: List[Platform] = Nil,

  @description("A list of executors to execute target artifacts.")
  executors: List[Executor] = Nil,
  @description("A list of engine environments to execute target artifacts in.")
  engines: List[Engine] = Nil,

  @internalFunctionality
  info: Option[Info] = None
) {

  @description(
    """Config inheritance by including YAML partials. This is useful for defining common APIs in
      |separate files. `__merge__` can be used in any level of the YAML. For example,
      |not just in the config but also in the functionality or any of the platforms.
      |""".stripMargin)
  @example("__merge__: ../api/common_interface.yaml", "yaml")
  @since("Viash 0.6.3")
  private val `__merge__`: Option[File] = None
  

  
  /**
    * Find the executor
    * 
    * Order of execution:
    *   - if an executor id is passed, look up the executor in the executors list
    *   - else if executors is a non-empty list, use the first executor
    *   - else use the executable executor
    *
    * @param executorStr An executor ID referring to one of the config's executors
    * @return An executor
    */
  def findExecutor(query: Option[String]): Executor = {
    findExecutors(query).head
  }

  /**
    * Find the executors
    * 
    * Order of execution:
    *   - if an executor id is passed, look up the executor in the executors list
    *   - else if executors is a non-empty list, use the first executor
    *   - else use the executable executor
    *
    * @param query An executor ID referring to one of the config's executors
    * @return An executor
    */
  def findExecutors(query: Option[String]): List[Executor] = {
    // TODO: match on query, there's no need to do a .* if query is None
    val regex = query.getOrElse(".*").r

    val foundMatches = getExecutors.filter{ e =>
      regex.findFirstIn(e.id).isDefined
    }
    
    foundMatches match {
      case li if li.nonEmpty =>
        li
      case Nil if query.isDefined =>
        throw new RuntimeException(s"no executor id matching regex '$regex' could not be found in the config.")
      case _ =>
        // TODO: switch to the getExecutors.head ?
        List(ExecutableExecutor())
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

    val foundMatches = getEngines.filter{ e =>
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

  lazy val getEngines: List[Engine] = platforms.collect{
    case p: NativePlatform =>
      NativeEngine(
        id = p.id
      )
    case p: DockerPlatform => 
      DockerEngine(
        id = p.id,
        image = p.image, 
        organization = p.organization,
        registry = p.registry,
        tag = p.tag,
        target_image = p.target_image,
        target_organization = p.target_organization,
        target_registry = p.target_registry,
        target_tag = p.target_tag,
        namespace_separator = p.namespace_separator,
        target_image_source = p.target_image_source,
        setup = p.setup,
        test_setup = p.test_setup,
        entrypoint = p.entrypoint,
        cmd = p.cmd
      )
    } ::: engines
  lazy val getExecutors: List[Executor] = platforms.collect{
    case p: NativePlatform =>
      ExecutableExecutor(
        id = p.id
      )
    case p: DockerPlatform => 
      ExecutableExecutor(
        id = p.id,
        port = p.port,
        workdir = p.workdir,
        docker_setup_strategy = p.setup_strategy,
        docker_run_args = p.run_args
      )
    } ::: executors
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
    projectDir: Option[URI] = None,
    addOptMainScript: Boolean = true,
    configMods: List[String] = Nil
  ): Config = {
    val uri = IO.uri(configPath)
    readFromUri(
      uri = uri,
      projectDir = projectDir,
      addOptMainScript = addOptMainScript,
      configMods = configMods
    )
  }

  def readFromUri(
    uri: URI,
    projectDir: Option[URI] = None,
    addOptMainScript: Boolean = true,
    configMods: List[String] = Nil
  ): Config = {
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
    val json1 = json0.inherit(uri, projectDir)

    /* JSON 2: after preparse config mods  */
    // apply preparse config mods if need be
    val json2 = confMods(json1, preparse = true)

    /* CONFIG 0: converted from json */
    // convert Json into Config
    val conf0 = Convert.jsonToClass[Config](json2, uri.toString())

    /* CONFIG 1: store parent path in resource to be able to access them in the future */
    val parentURI = uri.resolve("")
    val resources = conf0.functionality.resources.map(_.copyWithAbsolutePath(parentURI, projectDir))
    val tests = conf0.functionality.test_resources.map(_.copyWithAbsolutePath(parentURI, projectDir))

    // copy resources with updated paths into config and return
    val conf1 = conf0.copy(
      functionality = conf0.functionality.copy(
        resources = resources,
        test_resources = tests
      )
    )

    /* CONFIG 2: apply post-parse config mods */
    // apply config mods only if need be
    val conf2 = 
      if (confMods.postparseCommands.nonEmpty) {
        // turn config back into json
        val js = encodeConfig(conf1)
        // apply config mods
        val modifiedJs = confMods(js, preparse = false)
        // turn json back into a config
        modifiedJs.as[Config].fold(throw _, identity)
      } else {
        conf1
      }

    /* CONFIG 3: add info */
    // gather git info
    // todo: resolve git in project?
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
    if (conf2.functionality.status == Status.Deprecated)
      warn(s"Warning: The status of the component '${conf2.functionality.name}' is set to deprecated.")
    
    if (conf2.functionality.resources.isEmpty && optScript.isEmpty)
      warn(s"Warning: no resources specified!")

    if (!addOptMainScript) {
      return conf3
    }
    
    /* CONFIG 4: add main script if config is stored inside script */
    // add info and additional resources
    val conf4 = conf3.copy(
      functionality = conf3.functionality.copy(
        resources = optScript.toList ::: conf3.functionality.resources
      )
    )

    conf4
  }

  def readConfigs(
    source: String,
    projectDir: Option[URI] = None,
    query: Option[String] = None,
    queryNamespace: Option[String] = None,
    queryName: Option[String] = None,
    configMods: List[String] = Nil,
    addOptMainScript: Boolean = true
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
        // read config to get an idea of the name and namespaces
        // warnings will be captured for now, and will be displayed when reading the second time
        val stdout = new ByteArrayOutputStream()
        val stderr = new ByteArrayOutputStream()
        val appliedConfig: AppliedConfig = 
          Console.withErr(stderr) {
            Console.withOut(stdout) {
              Config.read(
                file.toString,
                projectDir = projectDir,
                addOptMainScript = addOptMainScript,
                configMods = configMods
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

        // if config passes regex checks, show warning and return it
        if (queryTest && nameTest && namespaceTest && isEnabled) {
          // TODO: stdout and stderr are no longer in the correct order :/
          val stdout_s = stdout.toString()
          val stderr_s = stderr.toString()
          if (!stdout_s.isEmpty())
            infoOut(stdout_s)
          if (!stderr_s.isEmpty())
            info(stderr_s)
          appliedConfig
        } else {
          appliedConfig.setStatus(Disabled)
        }
      } catch {
        case _: Exception =>
          error(s"Reading file '$file' failed")
          AppliedConfig(Config(Functionality("failed")), None, Nil, Some(ParseError))
      }
    }
  }
}
