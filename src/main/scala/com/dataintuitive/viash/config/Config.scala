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

import java.net.URI
import io.circe.yaml.parser
import io.viash.functionality.resources._

import java.io.File
import io.circe.DecodingFailure
import io.circe.ParsingFailure
import io.viash.config_mods.ConfigMods

case class Config(
  functionality: Functionality,
  platform: Option[Platform] = None,
  platforms: List[Platform] = Nil,
  info: Option[Info] = None
)

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
    val js = parser.parse(yamlText).fold(errorHandler, a => a)

    // apply preparse config mods
    val modifiedJs = preparseMods match {
      case None => js
      case Some(cmds) => cmds(js.hcursor, preparse = true).top.get
    }

    // parse as config
    val config = modifiedJs.as[Config].fold(errorHandler, identity)

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
    // get config uri
    val configUri = IO.uri(config)

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
      throw new RuntimeException("config file (" + config + ") must be a yaml file containing a viash config.")
    }
  }

  // parse
  def parseConfigMods(li: List[String] = Nil): Option[ConfigMods] = {
    if (li.isEmpty) {
      None
    } else {
      Some(ConfigModParser.parseBlock(li.mkString("; ")))
    }
  }

  // reads and modifies the config based on the current setup
  def read(
    configPath: String,
    platform: Option[String] = None,
    modifyConfig: Boolean = true,
    applyPlatform: Boolean = true,
    configMods: List[String] = Nil
  ): Config = {

    // read yaml
    val (yaml, optScript) = readYAML(configPath)

    // read config mods
    val confMods = parseConfigMods(configMods)
    
    /* CONFIG 0: read from yaml */
    // parse yaml as config, incl preparse configmods
    val conf0 = parse(yaml, IO.uri(configPath), confMods)

    /* CONFIG 1: apply config mods */
    // parse and apply commands
    val conf1 = confMods match {
      case None => conf0
      case Some(cmds) => {
        // turn config back into json
        val js = encodeConfig(conf0)
        // apply config mods
        val modifiedJs = cmds(js.hcursor, preparse = false)
        // turn json back into a config
        modifiedJs.as[Config].fold(throw _, identity)
      }
    }

    if (conf1.functionality.resources.isEmpty)
      Console.err.println("Warning: no resources specified!")

    if (!modifyConfig) {
      return conf1
    }

    /* CONFIG 2: add info and script (if available) */
    // gather git info
    val path = new File(configPath).getParentFile
    val GitInfo(_, rgr, gc, gt) = Git.getInfo(path)

    // create info object
    val info = 
      Info(
        viash_version = Some(io.viash.Main.version),
        config = configPath,
        platform = platform,
        git_commit = gc,
        git_remote = rgr,
        git_tag = gt
      )
    
    // add info and additional resources
    val conf2 = conf1.copy(
      info = Some(info),
      functionality = conf1.functionality.copy(
        resources = optScript.toList ::: conf1.functionality.resources
      )
    )

    /* CONFIG 3: apply platform wrapper */

    // get the platform
    // * if a platform id is passed, look up the platform in the platforms list
    // * else if a platform yaml is passed, read platform from file
    // * else if a platform is already defined in the config, use that
    // * else if platforms is a non-empty list, use the first platform
    // * else use the native platform
    val pl =
      if (platform.isDefined) {
        val pid = platform.get

        val platformNames = conf1.platforms.map(_.id)

        if (platformNames.contains(pid)) {
          conf1.platforms(platformNames.indexOf(pid))
        } else if (pid.endsWith(".yaml") || pid.endsWith(".yml")) {
          Platform.parse(IO.uri(platform.get))
        } else {
          throw new RuntimeException("platform must be a platform id specified in the config or a path to a platform yaml file.")
        }
      } else if (conf1.platform.isDefined) {
        conf1.platform.get
      } else if (conf1.platforms.nonEmpty) {
        conf1.platforms.head
      } else {
        NativePlatform()
      }
    
    // apply platform to functionality if so desired
    val conf3 = 
      if (!applyPlatform) {
        conf2
      } else {
        conf2.copy(
          functionality = pl.modifyFunctionality(conf2, false)
        )
      }
    
    // insert selected platform
    conf3.copy(
      platform = Some(pl)
    )
  }

}
