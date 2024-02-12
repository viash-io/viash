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

package io.viash.functionality


import io.circe.Json
import io.circe.generic.extras._
import io.viash.schemas._
import io.viash.wrapper.BashWrapper
import scala.collection.immutable.ListMap

@description(
  """The functionality-part of the config file describes the behaviour of the script in terms of arguments and resources.
    |By specifying a few restrictions (e.g. mandatory arguments) and adding some descriptions, Viash will automatically generate a stylish command-line interface for you.
    |""".stripMargin)
case class Functionality(

) {
  // Handled in preparsing
  // @description(
  //   """A list of @[arguments](argument) for this component. For each argument, a type and a name must be specified. Depending on the type of argument, different properties can be set. See these reference pages per type for more information:  
  //     |
  //     | - @[string](arg_string)
  //     | - @[file](arg_file)
  //     | - @[integer](arg_integer)
  //     | - @[double](arg_double)
  //     | - @[boolean](arg_boolean)
  //     | - @[boolean_true](arg_boolean_true)
  //     | - @[boolean_false](arg_boolean_false)
  //     |""".stripMargin)
  // @example(
  //   """arguments:
  //     |  - name: --foo
  //     |    type: file
  //     |    alternatives: [-f]
  //     |    description: Description of foo
  //     |    default: "/foo/bar"
  //     |    must_exist: true
  //     |    direction: output
  //     |    required: false
  //     |    multiple: true
  //     |    multiple_sep: ";"
  //     |  - name: --bar
  //     |    type: string
  //     |""".stripMargin,
  //     "yaml")
  // @default("Empty")
  // private val arguments: List[Argument[_]] = Nil

}

