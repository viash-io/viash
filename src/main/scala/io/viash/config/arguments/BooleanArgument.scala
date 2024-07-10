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

package io.viash.config.arguments

import io.circe.Json
import io.viash.helpers.data_structures._
import io.viash.schemas._

abstract class BooleanArgumentBase extends Argument[Boolean] {
  val flagValue: Option[Boolean]
}

@description("A `boolean` type argument has two possible values: `true` or `false`.")
@example(
  """arguments:
    |  - name: --trim
    |    type: boolean
    |    default: true
    |    description: Trim whitespace from the final output
    |    alternatives: ["-t"]
    |""".stripMargin,
    "yaml")
@subclass("boolean")
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
  @default("Empty")
  alternatives: OneOrMore[String] = Nil,
  
  @description("A clean notation of the argument's name. This is used when generating documentation.")
  @example("label: an argument", "yaml")
  @default("Empty")
  @since("Viash 0.9.0")
  label: Option[String] = None,

  @description("A short summary of the argument. This is used when generating documentation.")
  @example("summary: Use this argument to do X", "yaml")
  @default("Empty")
  @since("Viash 0.9.0")
  summary: Option[String] = None,

  @description("A description of the argument. This will be displayed with `--help`.")
  description: Option[String] = None,

  @description("Structured information. Can be any shape: a string, vector, map or even nested map.")
  @example(
    """info:
      |  category: cat1
      |  labels: [one, two, three]""".stripMargin, "yaml")
  @since("Viash 0.6.3")
  @default("Empty")
  info: Json = Json.Null,
  
  @description("An example value for this argument. If no [`default`](#default) property was specified, this will be used for that purpose.")
  @example(
    """- name: --my_boolean
      |  type: boolean
      |  example: true
      |""".stripMargin,
      "yaml")
  @default("Empty")
  example: OneOrMore[Boolean] = Nil,

  @description("The default value when no argument value is provided. This will not work if the [`required`](#required) property is enabled.")
  @example(
    """- name: --my_boolean
      |  type: boolean
      |  default: true
      |""".stripMargin,
      "yaml")
  @default("Empty")
  default: OneOrMore[Boolean] = Nil,

  @description("Make the value for this argument required. If set to `true`, an error will be produced if no value was provided. `false` by default.")
  @example(
    """- name: --my_boolean
      |  type: boolean
      |  required: true
      |""".stripMargin,
      "yaml")
  @default("False")
  required: Boolean = false,

  @undocumented
  direction: Direction = Input,

  @description("Treat the argument value as an array. Arrays can be passed using the delimiter `--foo=1:2:3` or by providing the same argument multiple times `--foo 1 --foo 2`. You can use a custom delimiter by using the [`multiple_sep`](#multiple_sep) property. `false` by default.")
  @example(
    """- name: --my_boolean
      |  type: boolean
      |  multiple: true
      |""".stripMargin,
      "yaml")
  @exampleWithDescription("my_component --my_boolean=true:true:false", "bash", "Here's an example of how to use this:")
  @default("False")
  multiple: Boolean = false,

  @description("The delimiter character for providing [`multiple`](#multiple) values. `:` by default.")
  @example(
    """- name: --my_boolean
      |  type: boolean
      |  multiple: true
      |  multiple_sep: ";"
      |""".stripMargin,
      "yaml")
  @exampleWithDescription("my_component --my_boolean=true,true,false", "bash", "Here's an example of how to use this:")
  @default(";")
  multiple_sep: String = ";",

  @undocumented
  dest: String = "par",
  `type`: String = "boolean"
) extends BooleanArgumentBase {
  @internalFunctionality
  val flagValue: Option[Boolean] = None

  def copyArg(
    `type`: String, 
    name: String, 
    alternatives: OneOrMore[String],
    label: Option[String],
    summary: Option[String],
    description: Option[String],
    info: Json,
    example: OneOrMore[Boolean],
    default: OneOrMore[Boolean],
    required: Boolean,
    direction: Direction,
    multiple: Boolean,
    multiple_sep: String,
    dest: String
  ): Argument[Boolean] = {
    copy(name, alternatives, label, summary, description, info, example, default, required, direction, multiple, multiple_sep, dest, `type`)
  }
}

@description("An argument of the `boolean_true` type acts like a `boolean` flag with a default value of `false`. When called as an argument it sets the `boolean` to `true`.")
@example(
  """arguments:
    |  - name: --silent
    |    type: boolean_true
    |    description: Ignore console output
    |    alternatives: ["-s"]
    |""".stripMargin,
    "yaml")
@subclass("boolean_true")
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
  @default("Empty")
  alternatives: OneOrMore[String] = Nil,

  @description("A clean notation of the argument's name. This is used when generating documentation.")
  @example("label: an argument", "yaml")
  @default("Empty")
  @since("Viash 0.9.0")
  label: Option[String] = None,

  @description("A short summary of the argument. This is used when generating documentation.")
  @example("summary: Use this argument to do X", "yaml")
  @default("Empty")
  @since("Viash 0.9.0")
  summary: Option[String] = None,

  @description("A description of the argument. This will be displayed with `--help`.")
  description: Option[String] = None,

  @description("Structured information. Can be any shape: a string, vector, map or even nested map.")
  @example(
    """info:
      |  category: cat1
      |  labels: [one, two, three]""".stripMargin, "yaml")
  @since("Viash 0.6.3")
  @default("Empty")
  info: Json = Json.Null,

  @undocumented
  direction: Direction = Input,

  dest: String = "par",
  `type`: String = "boolean_true"
) extends BooleanArgumentBase {
  @internalFunctionality
  val required: Boolean = false
  @internalFunctionality
  val flagValue: Option[Boolean] = Some(true)
  @internalFunctionality
  val default: OneOrMore[Boolean] = Nil
  @internalFunctionality
  val multiple: Boolean = false
  @internalFunctionality
  val multiple_sep: String = ";"
  @internalFunctionality
  val example: OneOrMore[Boolean] = Nil

  def copyArg(
    `type`: String, 
    name: String, 
    alternatives: OneOrMore[String],
    label: Option[String],
    summary: Option[String],
    description: Option[String],
    info: Json,
    default: OneOrMore[Boolean],
    example: OneOrMore[Boolean],
    required: Boolean,
    direction: Direction,
    multiple: Boolean,
    multiple_sep: String,
    dest: String
  ): Argument[Boolean] = {
    copy(name, alternatives, label, summary, description, info, direction, dest, `type`)
  }
}

@description("An argument of the `boolean_false` type acts like an inverted `boolean` flag with a default value of `true`. When called as an argument it sets the `boolean` to `false`.")
@example(
  """arguments:
    |  - name: --no-log
    |    type: boolean_false
    |    description: Disable logging
    |    alternatives: ["-nl"]
    |""".stripMargin,
    "yaml")
@subclass("boolean_false")
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
  @default("Empty")
  alternatives: OneOrMore[String] = Nil,

  @description("A clean notation of the argument's name. This is used when generating documentation.")
  @example("label: an argument", "yaml")
  @default("Empty")
  @since("Viash 0.9.0")
  label: Option[String] = None,

  @description("A short summary of the argument. This is used when generating documentation.")
  @example("summary: Use this argument to do X", "yaml")
  @default("Empty")
  @since("Viash 0.9.0")
  summary: Option[String] = None,

  @description("A description of the argument. This will be displayed with `--help`.")
  description: Option[String] = None,

  @description("Structured information. Can be any shape: a string, vector, map or even nested map.")
  @example(
    """info:
      |  category: cat1
      |  labels: [one, two, three]""".stripMargin, "yaml")
  @since("Viash 0.6.3")
  @default("Empty")
  info: Json = Json.Null,

  @undocumented
  direction: Direction = Input,

  dest: String = "par",
  `type`: String = "boolean_false"
) extends BooleanArgumentBase {

  @internalFunctionality
  val required: Boolean = false
  @internalFunctionality
  val flagValue: Option[Boolean] = Some(false)
  @internalFunctionality
  val default: OneOrMore[Boolean] = Nil
  @internalFunctionality
  val multiple: Boolean = false
  @internalFunctionality
  val multiple_sep: String = ";"
  @internalFunctionality
  val example: OneOrMore[Boolean] = Nil

  def copyArg(
    `type`: String, 
    name: String, 
    alternatives: OneOrMore[String],
    label: Option[String],
    summary: Option[String],
    description: Option[String],
    info: Json,
    default: OneOrMore[Boolean],
    example: OneOrMore[Boolean],
    required: Boolean,
    direction: Direction,
    multiple: Boolean,
    multiple_sep: String,
    dest: String
  ): Argument[Boolean] = {
    copy(name, alternatives, label, summary, description, info, direction, dest, `type`)
  }
}
