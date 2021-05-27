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

package com.dataintuitive.viash.functionality

import dataobjects._
import resources._
import com.dataintuitive.viash.config.Version

case class Functionality(
  name: String,
  namespace: Option[String] = None,
  version: Option[Version] = None,
  authors: List[Author] = Nil,
  arguments: List[DataObject[_]] = Nil,
  resources: Option[List[Resource]] = None,
  description: Option[String] = None,
  function_type: Option[FunctionType] = None,
  tests: Option[List[Resource]] = None,
  info: Map[String, String] = Map.empty[String, String],

  // dummy arguments are used for handling extra directory mounts in docker
  dummy_arguments: Option[List[DataObject[_]]] = None,

  // setting this to true will change the working directory
  // to the resources directory when running the script
  // this is used when running `viash test`.
  set_wd_to_resources_dir: Option[Boolean] = None
) {

  // check whether there are not multiple positional arguments with multiplicity >1
  // and if there is one, whether its position is last
  {
    val positionals = arguments.filter(a => a.otype == "")
    val multiix = positionals.indexWhere(_.multiple)

    require(
      multiix == -1 || multiix == positionals.length - 1,
      message = s"positional argument ${positionals(multiix).name} should be last since it has multiplicity >1"
    )
  }

  // check functionality name
  require(Functionality.nameRegex.findFirstIn(name).isDefined, message = "functionality name must begin with a letter and consist only of alphanumeric characters or underscores.")

  // check arguments
  arguments.foreach { arg =>
    require(!Functionality.reservedParameters.contains(arg.name), message = s"${arg.name} is a viash reserved parameter name")
    require(Functionality.nameRegex.findFirstIn(arg.name).isDefined, message = s"argument ${arg.name}: name must begin with a letter and consist only of alphanumeric characters or underscores.")
    arg.alternatives.foreach { alternative =>
      require(!Functionality.reservedParameters.contains(alternative), message = s"argument ${alternative}: name is reserved by viash")
    }
  }

  def mainScript: Option[Script] =
    resources.getOrElse(Nil).head match {
      case s: Script => Some(s)
      case _ => None
    }

  def mainCode: Option[String] = mainScript.flatMap(_.read)

  def argumentsAndDummies: List[DataObject[_]] = arguments ::: dummy_arguments.getOrElse(Nil)
}

object Functionality {
  val nameRegex = "^-?-?[A-Za-z][A-Za-z0-9_]*$".r
  val reservedParameters = List("-h", "--help", "-v", "--verbose", "--verbosity", "--version")
}