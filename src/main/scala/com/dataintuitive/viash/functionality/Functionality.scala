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
import com.dataintuitive.viash.helpers._

/**
  * The functionality-part of the config file describes the behaviour of the script in terms of arguments and resources.
  * By specifying a few restrictions (e.g. mandatory arguments) and adding some descriptions, Viash will automatically generate a stylish command-line interface for you.
  *
  * @param name Name of the component and the filename of the executable when built with `viash build` .
  * @param namespace Namespace this component is a part of. This is required when grouping components together in a pipeline and building multiple components at once using `viash ns build` .
  * @param version Version of the component. This field will be used to version the executable and the Docker container.
  * @param authors A list of authors (introduced in Viash 0.3.1). An author must at least have a name, but can also have a list of roles, an e-mail address, and a map of custom properties.
  * @param inputs List of input arguments
  * @param outputs List of output arguments
  * @param arguments
  * @param resources
  * @param description A description of the component. This will be displayed with `--help` .
  * @param usage
  * @param test_resources
  * @param info A map for storing custom annotation (introduced in Viash 0.4.0).
  * @param dummy_arguments
  * @param set_wd_to_resources_dir
  * @param enabled
  */
case class Functionality(
  name: String,
  namespace: Option[String] = None,
  version: Option[Version] = None,
  authors: List[Author] = Nil,

  @since("Viash 0.5.11")
  inputs: List[DataObject[_]] = Nil,

  @since("Viash 0.5.11")
  outputs: List[DataObject[_]] = Nil,
  arguments: List[DataObject[_]] = Nil,

  @example("""resources:
             |  - type: r_script
             |    path: script.R
             |  - type: file
             |    path: resource1.txt
             |""".stripMargin,
             "yaml")
  @example("""resources:
             |  - type: r_script
             |    path: script.R
             |  - type: file
             |    path: resource1.txt
             |""".stripMargin,
             "blah")  
  resources: List[Resource] = Nil,
  @deprecated description: Option[String] = None,
  usage: Option[String] = None,
  test_resources: List[Resource] = Nil,
  @since("Viash 0.4.0") info: Map[String, String] = Map.empty[String, String],

  // dummy arguments are used for handling extra directory mounts in docker
  dummy_arguments: List[DataObject[_]] = Nil,

  // setting this to true will change the working directory
  // to the resources directory when running the script
  // this is used when running `viash test`.
  set_wd_to_resources_dir: Boolean = false,

  // setting this to false with disable this component when using namespaces.
  enabled: Boolean = true
) {

  // note that in the Functionality companion object, defaults gets added to inputs and outputs *before* actually 
  // parsing the configuration file with Circe. This is done in the .prepare step.
  inputs.foreach {
    input => require(input.direction == Input, s"input ${input.name} can only have input as direction")
  }
  outputs.foreach {
    output => require(output.direction == Output, s"input ${output.name} can only have output as direction")
  }

  // Combine inputs, outputs and arguments into one combined list
  def allArguments = inputs ::: outputs ::: arguments

  // check whether there are not multiple positional arguments with multiplicity >1
  // and if there is one, whether its position is last
  {
    val positionals = allArguments.filter(a => a.flags == "")
    val multiix = positionals.indexWhere(_.multiple)

    require(
      multiix == -1 || multiix == positionals.length - 1,
      message = s"positional argument ${positionals(multiix).name} should be last since it has multiplicity >1"
    )
  }

  // check functionality name
  require(name.matches("^[A-Za-z][A-Za-z0-9_]*$"), message = "functionality name must begin with a letter and consist only of alphanumeric characters or underscores.")

  // check arguments
  allArguments.foreach { arg =>
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

  def allArgumentsAndDummies: List[DataObject[_]] = allArguments ::: dummy_arguments
}

object Functionality {
  val reservedParameters = List("-h", "--help", "--version", "---v", "---verbose", "---verbosity")
}