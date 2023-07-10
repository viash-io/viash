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

package io.viash.project

import java.nio.file.{Files, Path}

import io.circe.yaml.parser

import io.viash.schemas._
import io.viash.helpers.data_structures.OneOrMore
import io.viash.helpers.IO
import io.viash.helpers.circe._
import java.nio.file.Paths
import io.circe.Json
import java.net.URI
import scala.util.{Try, Success, Failure}
import io.viash.exceptions.ConfigParserException

@description("A Viash project configuration file. It's name should be `_viash.yaml`.")
@example(
  """viash_version: 0.6.4
    §source: src
    §target: target
    §config_mods: |
    §  .platforms[.type == 'docker'].target_registry := 'ghcr.io'
    §  .platforms[.type == 'docker'].target_organization := 'viash-io'
    §  .platforms[.type == 'docker'].namespace_separator := '/'
    §  .platforms[.type == 'docker'].target_image_source := 'https://github.com/viash-io/viash'
    §""".stripMargin('§'), "yaml"
)
@since("Viash 0.6.4")
@nameOverride("Project")
case class ViashProject(
  @description("Which version of Viash to use.")
  @example("viash_versions: 0.6.4", "yaml")
  viash_version: Option[String] = None,

  // todo: turn this into path
  @description("Which source directory to use for the `viash ns` commands.")
  @example("source: src", "yaml")
  source: Option[String] = None,

  // todo: turn this into path
  @description("Which target directory to use for `viash ns build`.")
  @example("target: target", "yaml")
  target: Option[String] = None,

  // todo: make this a ConfigMods object
  // todo: link to config mods docs
  @description("Which config mods to apply.")
  @example("config_mods: \".functionality.name := 'foo'\"", "yaml")
  @default("Empty")
  config_mods: OneOrMore[String] = Nil,

  @description("Directory in which the _viash.yaml resides.")
  @internalFunctionality
  rootDir: Option[Path] = None
)

object ViashProject {

  /**
    * Look for a Viash project file in a directory or its parents
    *
    * @param path The directory in which to look for a file called `_viash.yaml`
    * @return The path to the Viash project file, if found.
    */
  def findProjectFile(path: Path): Option[Path] = {
    val child = path.resolve("_viash.yaml")
    if (Files.isDirectory(path) && Files.exists(child)) {
      Some(child)
    } else {
      val parent = path.getParent()
      if (parent == null) {
        None
      } else {
        findProjectFile(path.getParent())
      }
    }
  }

  private def parsingErrorHandler[C](uri: Option[URI]) = {
    (e: Exception) => {
      val uriStr = uri.map(u => s" '$u'").getOrElse("")
      Console.err.println(s"${Console.RED}Error parsing$uriStr.${Console.RESET}\nDetails:")
      throw e
    }
  }

  /**
    * Read the text from a Path and convert to a Json
    *
    * @param path The path to read out
    * @return A Json
    */
  def readJson(path: Path): Json = {
    // make URI
    val uri = path.toUri()

    // read yaml as string
    val projStr = IO.read(uri)
    val json0 = parser.parse(projStr).fold(parsingErrorHandler(Some(uri)), identity)

    /* JSON 1: after inheritance */
    // apply inheritance if need be
    val json1 = json0.inherit(uri, projectDir = Some(uri))

    json1
  }

  /**
    * Read the text from a Path and convert to a ViashProject
    *
    * @param path The path to read out
    * @return A parsed project config
    */
  def read(
    path: Path
  ): ViashProject = {
    val json = readJson(path)

    /* PROJECT 0: converted from json */
    // convert Json into ViashProject
    val proj0 = Try(json.as[ViashProject]) match {
      case Success(res) => res.fold(parsingErrorHandler(Some(path.toUri())), identity)
      case Failure(e) => throw new ConfigParserException(path.toString(), e)
    }

    /* PROJECT 1: make resources absolute */
    // make paths absolute
    // todo: move to separate helper function
    def rela(parent: Path, path: String): String = {
      val pth = Paths.get(path).toFile
      if (pth.isAbsolute) {
        path
      } else {
        parent.resolve(path).toString
      }
    }
    val source = proj0.source.map(rela(path.getParent(), _))
    val target = proj0.target.map(rela(path.getParent(), _))

    // copy resources with updated paths into config and return
    val proj1 = proj0.copy(
      source = source,
      target = target,
      rootDir = Some(path.getParent())
    )

    proj1
  }

  /**
    * Look for a Viash project file in a directory or its parents
    * and convert to a ViashProject object
    *
    * @param path The directory in which to look for a file called `_viash.yaml`
    * @return The project config, if found
    */
  def findViashProject(path: Path): ViashProject = {
    findProjectFile(path) match {
      case Some(projectPath) =>
        read(projectPath)
      case None => ViashProject()
    }
  }
}