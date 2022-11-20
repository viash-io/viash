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

import io.viash.helpers.data_structures.OneOrMore
import io.viash.helpers.IO
import io.viash.helpers.circe._
import java.nio.file.Paths

case class ViashProject(
  source: Option[String] = None,
  target: Option[String] = None,
  // TODO: make this a ConfigMods object
  config_mods: OneOrMore[String] = Nil
)
// todo: resolve git in project?

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

  /**
    * Read the text from a Path and convert to a ViashProject
    *
    * @param path The path to read out
    * @return A parsed project config
    */
  def read(
    path: Path
  ): ViashProject = {
    // make URI
    val uri = path.toUri()

    // read yaml as string
    val projStr = IO.read(uri)
    
    /* JSON 0: parsed from string */
    // parse yaml into Json
    def parsingErrorHandler[C](e: Exception): C = {
      Console.err.println(s"${Console.RED}Error parsing '${uri}'.${Console.RESET}\nDetails:")
      throw e
    }
    val json0 = parser.parse(projStr).fold(parsingErrorHandler, identity)

    /* JSON 1: after inheritance */
    // apply inheritance if need be
    val json1 = json0.inherit(uri)

    /* PROJECT 0: converted from json */
    // convert Json into ViashProject
    val proj0 = json1.as[ViashProject].fold(parsingErrorHandler, identity)


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
      target = target
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