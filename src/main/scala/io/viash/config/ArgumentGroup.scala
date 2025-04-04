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

import io.viash.config.arguments.Argument
import io.viash.schemas._

@description("A grouping of the @[arguments](argument), used to display the help message.")
@example(
  """argument_groups:
    |  - name: "Input"
    |    arguments:
    |      - name: "--id"
    |        type: string
    |        required: true
    |      - name: "--input"
    |        type: file
    |        required: true
    |  - name: "Output"
    |    arguments:
    |      - name: "--output"
    |        type: file
    |        direction: output
    |        required: true
    |      - name: "--output_optional"
    |        type: file
    |        direction: output
    |""",
    "yaml")
case class ArgumentGroup(
  @description("The name of the argument group.")
  name: String,

  @description("A clean version of the argument group's name. This is only used for documentation.")
  @example("label: \"My argument group\"", "yaml")
  @default("Empty")
  @since("Viash 0.9.0")
  label: Option[String] = None,

  @description("A one-sentence summary of the argument group. This is only used for documentation.")
  @example("summary: \"Arguments related to functionality XYZ\"", "yaml")
  @default("Empty")
  @since("Viash 0.9.0")
  summary: Option[String] = None,

  @description("A description of the argument group. This is only used for documentation. Multiline descriptions are supported.")
  @example(
    """description: |
      |  A (multiline) description of the purpose of the arguments
      |  in this argument group.""", "yaml")
  @default("Empty")
  description: Option[String] = None,

  @description(
    """A list of @[arguments](argument) for this component. For each argument, a type and a name must be specified. Depending on the type of argument, different properties can be set. See these reference pages per type for more information:  
      |
      | - @[string](arg_string)
      | - @[file](arg_file)
      | - @[integer](arg_integer)
      | - @[double](arg_double)
      | - @[boolean](arg_boolean)
      | - @[boolean_true](arg_boolean_true)
      | - @[boolean_false](arg_boolean_false)
      |""")
  @example(
    """arguments:
      |  - name: --foo
      |    type: file
      |    alternatives: [-f]
      |    description: Description of foo
      |    default: "/foo/bar"
      |    must_exist: true
      |    direction: output
      |    required: false
      |    multiple: true
      |    multiple_sep: ";"
      |  - name: --bar
      |    type: string
      |""",
      "yaml")
  @default("Empty")
  arguments: List[Argument[_]] = Nil
)
