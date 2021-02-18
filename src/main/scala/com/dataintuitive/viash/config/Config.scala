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

package com.dataintuitive.viash.config

import com.dataintuitive.viash.functionality._
import com.dataintuitive.viash.platforms._
import com.dataintuitive.viash.helpers.{IO, Git, GitInfo}
import com.dataintuitive.viash.helpers.Scala._
import java.net.URI
import io.circe.yaml.parser
import com.dataintuitive.viash.functionality.resources._
import java.io.File

case class Config(
  functionality: Functionality,
  platform: Option[Platform] = None,
  platforms: List[Platform] = Nil,
  info: Option[Info] = None
)

object Config {
  def parse(uri: URI): Config = {
    val str = IO.read(uri)
    parse(str, uri)
  }

  def parse(yamlText: String, uri: URI): Config = {
    val config = parser.parse(yamlText)
      .fold(throw _, _.as[Config])
      .fold(throw _, identity)

    val fun = config.functionality

    // make paths absolute
    val resources = fun.resources.getOrElse(Nil).map(_.copyWithAbsolutePath(uri))

    // make absolute otherwise viash test can't find resources
    val tests = fun.tests.getOrElse(Nil).map(_.copyWithAbsolutePath(uri))

    // copy resources with updated paths into config and return
    config.copy(
      functionality = fun.copy(
        resources = Some(resources),
        tests = Some(tests)
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

      val script = scriptObj(dest = Some(basename), text = Some(code))

      (yaml, Some(script))
    } else {
      throw new RuntimeException("config file (" + config + ") must be a yaml file containing a viash config.")
    }
  }

  def read(
    config: String,
    platform: Option[String] = None,
    modifyFun: Boolean = true,
    namespace: Option[String] = None
  ): Config = {

    // read yaml
    val (yaml, optScript) = readYAML(config)

    // read config
    val configUri = IO.uri(config)
    val conf0 = parse(yaml, configUri)

    // get the platform
    // * if a platform id is passed, look up the platform in the platforms list
    // * else if a platform yaml is passed, read platform from file
    // * else if a platform is already defined in the config, use that
    // * else if platforms is a non-empty list, use the first platform
    // * else use the native platform
    val pl =
      if (platform.isDefined) {
        val pid = platform.get

        val platformNames = conf0.platforms.map(_.id)

        if (platformNames.contains(pid)) {
          conf0.platforms(platformNames.indexOf(pid))
        } else if (pid.endsWith(".yaml") || pid.endsWith(".yml")) {
          Platform.parse(IO.uri(platform.get))
        } else {
          throw new RuntimeException("platform must be a platform id specified in the config or a path to a platform yaml file.")
        }
      } else if (conf0.platform.isDefined) {
        conf0.platform.get
      } else if (conf0.platforms.nonEmpty) {
        conf0.platforms.head
      } else {
        NativePlatform()
      }

    // gather git info
    val path = new File(config).getParentFile
    val GitInfo(_, rgr, gc) = Git.getInfo(path)

    // check whether to modify the fun
    val modifyFunFun: Functionality => Functionality = {
      if (modifyFun) {
        pl.modifyFunctionality
      } else {
        identity
      }
    }

    // combine config into final object
    conf0.copy(
      // add info
      info = Some(Info(
        viash_version = Some(com.dataintuitive.viash.Main.version),
        config = config,
        platform = platform,
        git_commit = gc,
        git_remote = rgr
      )),
      // apply platform modification to functionality
      functionality = modifyFunFun(conf0.functionality.copy(
        // override namespace if none is specified in config
        namespace = conf0.functionality.namespace | namespace,
        // add script (if available) to resources
        resources = Some(optScript.toList ::: conf0.functionality.resources.getOrElse(Nil))
      )),
      // insert selected platform
      platform = Some(pl)
    )
  }

}
