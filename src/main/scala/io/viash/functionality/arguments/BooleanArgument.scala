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

import io.viash.helpers.Circe.OneOrMore
import io.viash.schemas._

abstract class BooleanArgumentBase extends Argument[Boolean] {
  val flagValue: Option[Boolean]
}

@description( 
  """A `boolean` type argument has two possible values: `true` or `false`.
    |  
    |Example:  
    |  
    | ```yaml  
    |arguments:
    |  - name: --trim
    |    type: boolean
    |    default: true
    |    description: Trim whitespace from the final output
    |    alternatives: ["-t"]
    |```  
    |  
    |""".stripMargin)
case class BooleanArgument(
  @description(
    """The name of the argument. Can be in the formats `--trim`, `-t` or `trim`. The number of dashes determines how values can be passed:  
      |
      |  - `--trim` is a long option, which can be passed with `executable_name --trim`
      |  - `-t` is a short option, which can be passed with `executable_name -t`
      |  - `trim` is an argument, which can be passed with `executable_name trim`  
      |""".stripMargin)
  name: String,

  @description("List of alternative format variations for this argument.")
  alternatives: OneOrMore[String] = Nil,

  @description("A description of the argument. This will be displayed with `--help`.")
  description: Option[String] = None,
  
  @description("An example value for this argument. If no [`default`](#default) property was specified, this will be used for that purpose.")
  @example(
    """- name: --my_boolean
      |  type: boolean
      |  example: true
      |""".stripMargin,
      "yaml")
  example: OneOrMore[Boolean] = Nil,

  @description("The default value when no argument value is provided. This will not work if the [`required`](#required) property is enabled.")
  @example(
    """- name: --my_boolean
      |  type: boolean
      |  default: true
      |""".stripMargin,
      "yaml") 
  default: OneOrMore[Boolean] = Nil,

  @description("Make the value for this argument required. If set to `true`, an error will be produced if no value was provided. `false` by default.")
  @example(
    """- name: --my_boolean
      |  type: boolean
      |  required: true
      |""".stripMargin,
      "yaml")
  required: Boolean = false,

  direction: Direction = Input,

  @description("Treat the argument value as an array. Arrays can be passed using the delimiter `--foo=1:2:3` or by providing the same argument multiple times `--foo 1 --foo 2`. You can use a custom delimiter by using the [`multiple_sep`](#multiple_sep) property. `false` by default.")
  @example(
    """- name: --my_boolean
      |  type: boolean
      |  multiple: true
      |""".stripMargin,
      "yaml")
  @exampleWithDescription("my_component --my_boolean=true:true:false", "bash", "Here's an example of how to use this:")
  multiple: Boolean = false,

  @description("The delimiter character for providing [`multiple`](#multiple) values. `:` by default.")
  @example(
    """- name: --my_boolean
      |  type: boolean
      |  multiple: true
      |  multiple_sep: ","
      |""".stripMargin,
      "yaml")
  @exampleWithDescription("my_component --my_boolean=true,true,false", "bash", "Here's an example of how to use this:")
  multiple_sep: Char = ':',

  `type`: String = "boolean"
) extends BooleanArgumentBase {

  val flagValue: Option[Boolean] = None

  def copyArg(
    `type`: String, 
    name: String, 
    alternatives: OneOrMore[String],
    description: Option[String],
    example: OneOrMore[Boolean],
    default: OneOrMore[Boolean],
    required: Boolean,
    direction: Direction,
    multiple: Boolean,
    multiple_sep: Char
  ): Argument[Boolean] = {
    copy(name, alternatives, description, example, default, required, direction, multiple, multiple_sep, `type`)
  }
}

@description(
  """An argument of the `boolean_true` type acts like a `boolean` flag with a default value of `false`. When called as an argument it sets the `boolean` to `true`.  
    |  
    |Example:  
    |  
    | ```yaml  
    |arguments:
    |  - name: --silent
    |    type: boolean_true
    |    description: Ignore console output
    |    alternatives: ["-s"]
    |```  
    |  
    |""".stripMargin)
case class BooleanTrueArgument(
  @description(
    """The name of the argument. Can be in the formats `--silent`, `-s` or `silent`. The number of dashes determines how values can be passed:  
      |
      |  - `--silent` is a long option, which can be passed with `executable_name --silent`
      |  - `-s` is a short option, which can be passed with `executable_name -s`
      |  - `silent` is an argument, which can be passed with `executable_name silent`  
      |""".stripMargin)
  name: String,

  @description("List of alternative format variations for this argument.")
  alternatives: OneOrMore[String] = Nil,

  @description("A description of the argument. This will be displayed with `--help`.")
  description: Option[String] = None,

  direction: Direction = Input,

  `type`: String = "boolean_true"
) extends BooleanArgumentBase {

  val required: Boolean = false
  val flagValue: Option[Boolean] = Some(true)
  val default: OneOrMore[Boolean] = Nil
  val multiple: Boolean = false
  val multiple_sep: Char = ':'
  val example: OneOrMore[Boolean] = Nil

  def copyArg(
    `type`: String, 
    name: String, 
    alternatives: OneOrMore[String],
    description: Option[String],
    default: OneOrMore[Boolean],
    example: OneOrMore[Boolean],
    required: Boolean,
    direction: Direction,
    multiple: Boolean,
    multiple_sep: Char
  ): Argument[Boolean] = {
    copy(name, alternatives, description, direction, `type`)
  }
}

@description(
  """An argument of the `boolean_false` type acts like an inverted `boolean` flag with a default value of `true`. When called as an argument it sets the `boolean` to `false`.  
    |  
    |Example:  
    |  
    | ```yaml  
    |arguments:
    |  - name: --no-log
    |    type: boolean_false
    |    description: Disable logging
    |    alternatives: ["-nl"]
    |```  
    |  
    |""".stripMargin)
case class BooleanFalseArgument(
  @description(
    """The name of the argument. Can be in the formats `--no-log`, `-n` or `no-log`. The number of dashes determines how values can be passed:  
      |
      |  - `--no-log` is a long option, which can be passed with `executable_name --no-log`
      |  - `-n` is a short option, which can be passed with `executable_name -n`
      |  - `no-log` is an argument, which can be passed with `executable_name no-log`  
      |""".stripMargin)
  name: String,

  @description("List of alternative format variations for this argument.")
  alternatives: OneOrMore[String] = Nil,

  @description("A description of the argument. This will be displayed with `--help`.")
  description: Option[String] = None,

  direction: Direction = Input,

  `type`: String = "boolean_false"
) extends BooleanArgumentBase {

  val required: Boolean = false
  val flagValue: Option[Boolean] = Some(false)
  val default: OneOrMore[Boolean] = Nil
  val multiple: Boolean = false
  val multiple_sep: Char = ':'
  val example: OneOrMore[Boolean] = Nil

  def copyArg(
    `type`: String, 
    name: String, 
    alternatives: OneOrMore[String],
    description: Option[String],
    default: OneOrMore[Boolean],
    example: OneOrMore[Boolean],
    required: Boolean,
    direction: Direction,
    multiple: Boolean,
    multiple_sep: Char
  ): Argument[Boolean] = {
    copy(name, alternatives, description, direction, `type`)
  }
}
