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
import java.nio.file.Paths

@description(
  """For each argument, a type and a name must be specified. Depending on the type of argument, different properties can be set. See these reference pages per type for more information:  
    |
    | - @[string](arg_string)
    | - @[file](arg_file)
    | - @[integer](arg_integer)
    | - @[double](arg_double)
    | - @[boolean](arg_boolean)
    | - @[boolean_true](arg_boolean_true)
    | - @[boolean_false](arg_boolean_false)
    |""".stripMargin)
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
    |""".stripMargin,
    "yaml")
@subclass("BooleanArgument")
@subclass("BooleanTrueArgument")
@subclass("BooleanFalseArgument")
@subclass("DoubleArgument")
@subclass("FileArgument")
@subclass("IntegerArgument")
@subclass("LongArgument")
@subclass("StringArgument")
abstract class Argument[Type] {
  @description("Specifies the type of the argument.")
  val `type`: String
  val name: String
  val alternatives: OneOrMore[String]
  val description: Option[String]
  val info: Json
  val example: OneOrMore[Type]
  val default: OneOrMore[Type]
  val required: Boolean
  val direction: Direction
  val multiple: Boolean
  val multiple_sep: String
  
  @internalFunctionality
  val dest: String

  private val pattern = "^(-*)(.*)$".r
  val pattern(flags, plainName) = name

  /** Common parameter name for this argument */
  val par: String = dest + "_" + plainName

  /** Parameter name in bash scripts */
  val VIASH_PAR: String = "VIASH_" + dest.toUpperCase + "_" + plainName.toUpperCase()

  def copyArg(
    `type`: String = this.`type`,
    name: String = this.name,
    alternatives: OneOrMore[String] = this.alternatives,
    description: Option[String] = this.description,
    info: Json = this.info,
    example: OneOrMore[Type] = this.example,
    default: OneOrMore[Type] = this.default,
    required: Boolean = this.required,
    direction: Direction = this.direction,
    multiple: Boolean = this.multiple,
    multiple_sep: String = this.multiple_sep,
    dest: String = this.dest
  ): Argument[Type]

  def disableChecks: Argument[_] = {
    // todo: fix such that the output is Argument[Type]
    Some(this)
      // set required to false
      .map{
        case arg =>
          arg.copyArg(required = false)
      }
      // make sure the default is set. invent one if necessary.
      .map{
        // default is not needed
        case arg: BooleanTrueArgument => arg
        case arg: BooleanFalseArgument => arg
        // if available, use default
        case arg if arg.default.nonEmpty => 
          arg
        // else use example
        case arg if arg.example.nonEmpty => 
          arg.copyArg(default = arg.example)
        // else invent one
        case arg: BooleanArgument => 
          arg.copy(default = OneOrMore(true))
        case arg: DoubleArgument => 
          arg.copy(default = OneOrMore(123.0), min = None, max = None)
        case arg: FileArgument => 
          arg.copy(default = OneOrMore(Paths.get("/path/to/file")), must_exist = false)
        case arg: IntegerArgument =>
          arg.copy(default = OneOrMore(123), choices = Nil, min = None, max = None)
        case arg: LongArgument =>
          arg.copy(default = OneOrMore(123), choices = Nil, min = None, max = None)
        case arg: StringArgument => 
          arg.copy(default = OneOrMore("value"), choices = Nil)
        case arg => 
          throw new RuntimeException(
            f"Viash is missing a default for argument of type ${arg.`type`}. " +
              "Please report this error to the maintainers.")
      }
      // turn off must_exist and create_parent for fileargs
      .map{
        case arg: FileArgument =>
          arg.copy(must_exist = false, create_parent = false)
        case a => a
      }
      .get
  }

  assert(example.length <= 1 || multiple, s"Argument $name: 'example' should be length <= 1 if 'multiple' is false")
  assert(default.length <= 1 || multiple, s"Argument $name: 'default' should be length <= 1 if 'multiple' is false")
}
