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

import java.nio.file.Path
import io.viash.helpers.Circe.OneOrMore
import io.viash.schemas._

@description(
  """A `file` type argument has a string value that points to a file or folder path.
    |  
    |Example:  
    |  
    | ```yaml  
    |arguments:
    |  - name: --input_csv
    |    type: file
    |    must_exist: true
    |    description: CSV file to read contents from
    |    alternatives: ["-i"]
    |```  
    |  
    |""".stripMargin)
case class FileArgument(
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

  @description("An example value for this argument. If no [`default`](#default) property was specified, this will be used for that purpose.")
  @example(
    """- name: --my_file
      |  type: file
      |  example: data.csv
      |""".stripMargin,
      "yaml")
  example: OneOrMore[Path] = Nil,

  @description("The default value when no argument value is provided. This will not work if the [`required`](#required) property is enabled.")
  @example(
    """- name: --my_file
      |  type: file
      |  default: data.csv
      |""".stripMargin,
      "yaml")
  default: OneOrMore[Path] = Nil,

  @description("The file or folder should exist before the start of execution. If set to `true`, an error will be produced if the file or folder wasn't found.")
  @example(
    """- name: --my_file
      |  type: file
      |  must_exist: true
      |""".stripMargin,
      "yaml")
  must_exist: Boolean = false,

  @description("Make the value for this argument required. If set to `true`, an error will be produced if no value was provided. `false` by default.")
  @example(
    """- name: --my_file
      |  type: file
      |  required: true
      |""".stripMargin,
      "yaml")
  required: Boolean = false,

  @description("Makes this argument an `input` or an `output`, as in does the file/folder needs to be read or written. `input` by default.")
  @example(
    """- name: --my_output_file
      |  type: file
      |  direction: output
      |""".stripMargin,
      "yaml")
  direction: Direction = Input,

  @description("Treat the argument value as an array. Arrays can be passed using the delimiter `--foo=1:2:3` or by providing the same argument multiple times `--foo 1 --foo 2`. You can use a custom delimiter by using the [`multiple_sep`](#multiple_sep) property. `false` by default.")
  @example(
    """- name: --my_files
      |  type: file
      |  multiple: true
      |""".stripMargin,
      "yaml")
  @exampleWithDescription("my_component --my_files=firstFile.csv:anotherFile.csv:yetAnother.csv", "bash", "Here's an example of how to use this:")
  multiple: Boolean = false,

  @description("The delimiter character for providing [`multiple`](#multiple) values. `:` by default.")
  @example(
    """- name: --my_files
      |  type: file
      |  multiple: true
      |  multiple_sep: ","
      |""".stripMargin,
      "yaml")
  @exampleWithDescription("my_component --my_files=firstFile.csv,anotherFile.csv,yetAnother.csv", "bash", "Here's an example of how to use this:")
  multiple_sep: Char = ':',

  `type`: String = "file"
) extends Argument[Path] {
  def copyArg(
    `type`: String, 
    name: String, 
    alternatives: OneOrMore[String],
    description: Option[String],
    example: OneOrMore[Path],
    default: OneOrMore[Path],
    required: Boolean,
    direction: Direction,
    multiple: Boolean,
    multiple_sep: Char
  ): Argument[Path] = {
    copy(name, alternatives, description, example, default, this.must_exist, required, direction, multiple, multiple_sep, `type`)
  }
}
