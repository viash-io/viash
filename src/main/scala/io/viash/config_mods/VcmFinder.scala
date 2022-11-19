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

package io.viash.config_mods

// will need to be updated in a next version of scala
// import scala.jdk.CollectionConverters._
import scala.collection.JavaConverters._

import java.nio.file.{Files, Path => JPath}
import java.util.stream.Collectors


object VcmFinder {
  /**
    * List all Viash Config Mod files in a directory
    *
    * @param path The directory in which to look for .vcm files
    * @return A list of files with the .vcm extension
    */
  def listVcmFiles(path: JPath): List[JPath] = {
    Files.list(path)
      .filter(f => Files.isRegularFile(f) && f.getFileName().toString.endsWith(".vcm"))
      .collect(Collectors.toList())
      .asScala
      .toList
  }

  /**
    * Recursively look for .vcm files in the parents of a directory.
    * 
    * Recursion stops when a file called 'project.vcm' is encountered.
    *
    * @param path The directory in which to look for .vcm files in itself and its parents
    * @param prev The previously found .vcm files
    * @return A list of files with the .vcm extension
    */
  def walkVcmFiles(path: JPath, prev: List[JPath] = Nil): List[JPath] = {
    // println(s"Trying $path")
    val vcmFiles = listVcmFiles(path)
    val newList = prev ::: vcmFiles // switch order?
    if (vcmFiles.exists(_.toFile().getName() == "project.vcm")) {
      // we are in root of project
      newList
    } else {
      val parent = path.getParent()
      if (parent != null) {
        walkVcmFiles(parent, newList)
      } else {
        newList
      }
    }
  }

  /**
    * Find all .vcm files in a directory and its parents, read them all,
    * and convert to a ConfigMods object
    *
    * @param path The directory in which to look for .vcm files in itself and its parents
    * @return A ConfigMods object or not
    */
  def findAllVcm(path: JPath): ConfigMods = {
    val files = walkVcmFiles(path)
    val cms = files.map{ file =>
      val y = scala.io.Source.fromFile(file.toFile).getLines.toList
      ConfigMods.parseConfigMods(y)
      // todo: throw warning or error when parsing fails
    }
    cms.fold(ConfigMods()) { _ + _ }
  }
}
