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
import io.circe.generic.extras._

case class Functionality(
  name: String,
  namespace: Option[String] = None,
  version: Option[Version] = None,
  authors: List[Author] = Nil,
  inputs: List[DataObject[_]] = Nil,
  outputs: List[DataObject[_]] = Nil,
  @JsonKey("arguments") argumentsOrig: List[DataObject[_]] = Nil,
  resources: List[Resource] = Nil,
  description: Option[String] = None,
  usage: Option[String] = None,
  function_type: Option[FunctionType] = None,
  tests: List[Resource] = Nil,
  info: Map[String, String] = Map.empty[String, String],

  // dummy arguments are used for handling extra directory mounts in docker
  dummy_arguments: List[DataObject[_]] = Nil,

  // setting this to true will change the working directory
  // to the resources directory when running the script
  // this is used when running `viash test`.
  set_wd_to_resources_dir: Boolean = false,

  // whether or not to add the resource dir to the path
  add_resources_to_path: Boolean = false
) {

  inputs.foreach {
    input => require(input.direction != Output, s"input $input.name can only have input as direction")
  }
  outputs.foreach {
    output => require(output.direction != Input, s"input $output.name can only have output as direction")
  }

  // Combine inputs, outputs and arguments into one combined list
  def arguments = inputs ::: outputs ::: argumentsOrig

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
  require(name.matches("^[A-Za-z][A-Za-z0-9_]*$"), message = "functionality name must begin with a letter and consist only of alphanumeric characters or underscores.")

  // check arguments
  arguments.foreach { arg =>
    require(arg.name.matches("^(-?|--|\\$)[A-Za-z][A-Za-z0-9_]*$"), message = s"argument $arg.name: name must begin with a letter and consist only of alphanumeric characters or underscores.")
    (arg.name :: arg.alternatives).foreach { argName =>
      require(!Functionality.reservedParameters.contains(argName), message = s"argument $argName: name is reserved by viash")
      require(!argName.matches("^\\$VIASH_"), message = s"argument $argName: environment variables beginning with 'VIASH_' are reserved for viash.")
    }
  }

  def mainScript: Option[Script] =
    resources.head match {
      case s: Script => Some(s)
      case _ => None
    }

  def mainCode: Option[String] = mainScript.flatMap(_.read)

  def argumentsAndDummies: List[DataObject[_]] = arguments ::: dummy_arguments
}

object Functionality {
  val reservedParameters = List("-h", "--help", "--version", "---v", "---verbose", "---verbosity")
}