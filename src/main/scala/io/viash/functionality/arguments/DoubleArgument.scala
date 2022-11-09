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

package io.viash.functionality.arguments

import io.circe.Json
import io.viash.helpers.data_structures._
import io.viash.schemas._

@description("A `double` type argument has a numeric value with decimal points")
@example(
  """arguments:
    |  - name: --litres
    |    type: double
    |    default: 1.5
    |    description: Litres of fluid to process
    |    alternatives: ["-l"]
    |""".stripMargin,
    "yaml")
case class DoubleArgument(
  @description(
    """The name of the argument. Can be in the formats `--foo`, `-f` or `foo`. The number of dashes determines how values can be passed:  
      |
      |  - `--foo` is a long option, which can be passed with `executable_name --foo=value` or `executable_name --foo value`
      |  - `-f` is a short option, which can be passed with `executable_name -f value`
      |  - `foo` is an argument, which can be passed with `executable_name value`  
      |""".stripMargin)
  name: String,

  @description("List of alternative format variations for this argument.")
  alternatives: OneOrMore[String] = Nil,

  @description("A description of the argument. This will be displayed with `--help`.")
  description: Option[String] = None,

  @description("Structured information. Can be any shape: a string, vector, map or even nested map.")
  @example(
    """info:
      |  category: cat1
      |  labels: [one, two, three]""".stripMargin, "yaml")
  @since("Viash 0.6.3")
  info: Json = Json.Null,

  @description("An example value for this argument. If no [`default`](#default) property was specified, this will be used for that purpose.")
  @example(
    """- name: --my_double
      |  type: double
      |  example: 5.8
      |""".stripMargin,
      "yaml")
  example: OneOrMore[Double] = Nil,

  @description("The default value when no argument value is provided. This will not work if the [`required`](#required) property is enabled.")
  @example(
    """- name: --my_double
      |  type: double
      |  default: 5.8
      |""".stripMargin,
      "yaml")
  default: OneOrMore[Double] = Nil,

  @description("Make the value for this argument required. If set to `true`, an error will be produced if no value was provided. `false` by default.")
  @example(
    """- name: --my_double
      |  type: double
      |  required: true
      |""".stripMargin,
      "yaml")
  required: Boolean = false,

  @description("Minimum allowed value for this argument. If set and the provided value is lower than the minimum, an error will be produced. Can be combined with [`max`](#max) to clamp values.")
  @example(
    """- name: --my_double
      |  type: double
      |  min: 25.5
      |""".stripMargin,
      "yaml")
  min: Option[Double] = None,

  @description("Maximum allowed value for this argument. If set and the provided value is higher than the maximum, an error will be produced. Can be combined with [`min`](#min) to clamp values.")
  @example(
    """- name: --my_double
      |  type: double
      |  max: 80.4
      |""".stripMargin,
      "yaml")  
  max: Option[Double] = None,

  @undocumented
  direction: Direction = Input,

  @description("Treat the argument value as an array. Arrays can be passed using the delimiter `--foo=1:2:3` or by providing the same argument multiple times `--foo 1 --foo 2`. You can use a custom delimiter by using the [`multiple_sep`](#multiple_sep) property. `false` by default.")
  @example(
    """- name: --my_double
      |  type: double
      |  multiple: true
      |""".stripMargin,
      "yaml")
  @exampleWithDescription("my_component --my_double=5.8:22.6:200.4", "bash", "Here's an example of how to use this:")
  multiple: Boolean = false,

  @description("The delimiter character for providing [`multiple`](#multiple) values. `:` by default.")
  @example(
    """- name: --my_double
      |  type: double
      |  multiple: true
      |  multiple_sep: ","
      |""".stripMargin,
      "yaml")
  @exampleWithDescription("my_component --my_double=5.8,22.6,200.4", "bash", "Here's an example of how to use this:")
  multiple_sep: String = ":",

  dest: String = "par",
  `type`: String = "double"
) extends Argument[Double] {
  def copyArg(
    `type`: String, 
    name: String, 
    alternatives: OneOrMore[String],
    description: Option[String],
    info: Json,
    example: OneOrMore[Double],
    default: OneOrMore[Double],
    required: Boolean,
    direction: Direction,
    multiple: Boolean,
    multiple_sep: String,
    dest: String
  ): Argument[Double] = {
    copy(name, alternatives, description, info, example, default, required, this.min, this.max, direction, multiple, multiple_sep, dest, `type`)
  }
}
