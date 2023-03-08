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
import io.viash.helpers.{Git, GitInfo, IO}
import io.viash.helpers.circe._
import io.viash.helpers.status._

import java.net.URI
import io.circe.yaml.parser
import io.viash.functionality.resources._

import java.io.File
import io.circe.DecodingFailure
import io.circe.ParsingFailure
import io.viash.config_mods.ConfigMods
import java.nio.file.Paths

import io.viash.schemas._
import java.io.ByteArrayOutputStream
import java.nio.file.FileSystemNotFoundException

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
      | - @[Nextflow VDSL3](platform_nextflow)
      |""".stripMargin)
  platforms: List[Platform] = Nil,

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
  val `__merge__`: Option[File] = None
  

  /**
    * Detect a config's platform
    * 
    * Order of execution:
    *   - if a platform id is passed, look up the platform in the platforms list
    *   - else if a platform yaml is passed, read platform from file
    *   - else if platforms is a non-empty list, use the first platform
    *   - else use the native platform
    *
    * @param platformStr A platform ID referring to one of the config's platforms, or a path to a YAML file
    * @return A platform
    */
  def findPlatform(platformStr: Option[String]): Platform = {
    if (platformStr.isDefined) {
      val pid = platformStr.get

      val platformNames = this.platforms.map(_.id)

      if (platformNames.contains(pid)) {
        this.platforms(platformNames.indexOf(pid))
      } else if (pid.endsWith(".yaml") || pid.endsWith(".yml")) {
        Platform.parse(IO.uri(platformStr.get))
      } else {
        throw new RuntimeException("platform must be a platform id specified in the config or a path to a platform yaml file.")
      }
    } else if (this.platforms.nonEmpty) {
      this.platforms.head
    } else {
      NativePlatform()
    }
  }
}

object Config {
  def parse(uri: URI, preparseMods: Option[ConfigMods]): Config = {
    val str = IO.read(uri)
    parse(str, uri, preparseMods)
  }

  def parse(yamlText: String, uri: URI, preparseMods: Option[ConfigMods]): Config = {
    def errorHandler[C](e: Exception): C = {
      Console.err.println(s"${Console.RED}Error parsing '${uri}'.${Console.RESET}\nDetails:")
      throw e
    }

    // read json
    val js1 = parser.parse(yamlText).fold(errorHandler, a => a)

    // apply inheritance if need be
    val js2 = js1.inherit(uri)

    if (js1 != js2) {
      Console.err.println("Warning: Config inheritance (__merge__) is an experimental feature. Changes to the API are expected.")
    }

    // apply preparse config mods
    val js3 = preparseMods match {
      case None => js2
      case Some(cmds) => cmds(js2, preparse = true)
    }

    // parse as config
    val config = js3.as[Config].fold(errorHandler, identity)

    // make paths absolute
    val resources = config.functionality.resources.map(_.copyWithAbsolutePath(uri))
    val tests = config.functionality.test_resources.map(_.copyWithAbsolutePath(uri))

    // copy resources with updated paths into config and return
    config.copy(
      functionality = config.functionality.copy(
        resources = resources,
        test_resources = tests
      )
    )
  }

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
    configMods: List[String] = Nil
  ): Config = {
    val uri = IO.uri(configPath)
    readFromUri(
      uri = uri,
      addOptMainScript = addOptMainScript,
      configMods = configMods
    )
  }

  def readFromUri(
    uri: URI,
    addOptMainScript: Boolean = true,
    configMods: List[String] = Nil
  ): Config = {
    // read cli config mods
    val confMods = ConfigMods.parseConfigMods(configMods)

    /* STRING */
    // read yaml as string
    val (yamlText, optScript) = readYAML(uri)
    
    /* JSON 0: parsed from string */
    // parse yaml into Json
    def parsingErrorHandler[C](e: Exception): C = {
      Console.err.println(s"${Console.RED}Error parsing '${uri}'.${Console.RESET}\nDetails:")
      throw e
    }
    val json0 = parser.parse(yamlText).fold(parsingErrorHandler, identity)

    /* JSON 1: after inheritance */
    // apply inheritance if need be
    val json1 = json0.inherit(uri)

    /* JSON 2: after preparse config mods  */
    // apply preparse config mods if need be
    val json2 = confMods(json1, preparse = true)

    /* CONFIG 0: converted from json */
    // convert Json into Config
    val conf0 = json2.as[Config].fold(parsingErrorHandler, identity)

    /* CONFIG 1: make resources absolute */
    // make paths absolute
    val resources = conf0.functionality.resources.map(_.copyWithAbsolutePath(uri))
    val tests = conf0.functionality.test_resources.map(_.copyWithAbsolutePath(uri))

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
      Console.err.println(s"${Console.YELLOW}Warning: The status of the component '${conf2.functionality.name}' is set to deprecated.${Console.RESET}")
    
    if (conf2.functionality.resources.isEmpty && optScript.isEmpty)
      Console.err.println(s"${Console.YELLOW}Warning: no resources specified!${Console.RESET}")

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
    query: Option[String] = None,
    queryNamespace: Option[String] = None,
    queryName: Option[String] = None,
    configMods: List[String] = Nil,
    addOptMainScript: Boolean = true
  ): List[Either[Config, Status]] = {

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
        val config = 
          Console.withErr(stderr) {
            Console.withOut(stdout) {
              Config.read(
                file.toString, 
                addOptMainScript = addOptMainScript,
                configMods = configMods
              )
            }
          }

        val funName = config.functionality.name
        val funNs = config.functionality.namespace

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
        if (queryTest && nameTest && namespaceTest && config.functionality.isEnabled) {
          // TODO: stdout and stderr are no longer in the correct order :/
          Console.out.print(stdout.toString)
          Console.err.print(stderr.toString)
          Left(config)
        } else {
          Right(Disabled)
        }
      } catch {
        case _: Exception =>
          Console.err.println(s"${Console.RED}Reading file '$file' failed${Console.RESET}")
          Right(ParseError)
      }
    }
  }
}
