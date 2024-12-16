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
import java.nio.file.Path
import io.viash.helpers.data_structures._
import io.viash.schemas._

@description("A `file` type argument has a string value that points to a file or folder path.")
@example(
  """arguments:
    |  - name: --input_csv
    |    type: file
    |    must_exist: true
    |    description: CSV file to read contents from
    |    alternatives: ["-i"]
    |""",
    "yaml")
@subclass("file")
case class FileArgument(
  @description(
    """The name of the argument. Can be in the formats `--foo`, `-f` or `foo`. The number of dashes determines how values can be passed:  
      |
      |  - `--foo` is a long option, which can be passed with `executable_name --foo=value` or `executable_name --foo value`
      |  - `-f` is a short option, which can be passed with `executable_name -f value`
      |  - `foo` is an argument, which can be passed with `executable_name value`  
      |""")
  name: String,

  @description("List of alternative format variations for this argument.")
  @default("Empty")
  alternatives: OneOrMore[String] = Nil,

  @description("A clean version of the argument's name. This is only used for documentation.")
  @example("label: \"My argument\"", "yaml")
  @default("Empty")
  @since("Viash 0.9.0")
  label: Option[String] = None,

  @description("A one-sentence summary of the argument. This is only used for documentation.")
  @example("summary: \"This argument sets XYZ.\"", "yaml")
  @default("Empty")
  @since("Viash 0.9.0")
  summary: Option[String] = None,

  @description("A description of the argument. This is only used for documentation. Multiline descriptions are supported.")
  @example(
    """description: |
      |  A (multiline) description of the purpose of
      |  this argument.""", "yaml")
  @default("Empty")
  description: Option[String] = None,

  @description("Structured information. Can be any shape: a string, vector, map or even nested map.")
  @example(
    """info:
      |  category: cat1
      |  labels: [one, two, three]""", "yaml")
  @since("Viash 0.6.3")
  @default("Empty")
  info: Json = Json.Null,

  @description("An example value for this argument. If no [`default`](#default) property was specified, this will be used for that purpose.")
  @example(
    """- name: --my_file
      |  type: file
      |  example: data.csv
      |""",
      "yaml")
  @default("Empty")
  example: OneOrMore[Path] = Nil,

  @description("The default value when no argument value is provided. This will not work if the [`required`](#required) property is enabled.")
  @example(
    """- name: --my_file
      |  type: file
      |  default: data.csv
      |""",
      "yaml")
  @default("Empty")
  default: OneOrMore[Path] = Nil,

  @description(
    """Checks whether the file or folder exists. For input files, this check will happen
      |before the execution of the script, while for output files the check will happen afterwards.""")
  @example(
    """- name: --my_file
      |  type: file
      |  must_exist: true
      |""",
      "yaml")
  @default("True")
  must_exist: Boolean = true,

  @description("If the output filename is a path and it does not exist, create it before executing the script (only for `direction: output`).")
  @example(
    """- name: --my_file
      |  type: file
      |  direction: output
      |  create_parent: true
      |""",
      "yaml")
  @default("True")
  create_parent: Boolean = true,

  @description("Make the value for this argument required. If set to `true`, an error will be produced if no value was provided. `false` by default.")
  @example(
    """- name: --my_file
      |  type: file
      |  required: true
      |""",
      "yaml")
  @default("False")
  required: Boolean = false,

  @description("Makes this argument an `input` or an `output`, as in does the file/folder needs to be read or written. `input` by default.")
  @example(
    """- name: --my_output_file
      |  type: file
      |  direction: output
      |""",
      "yaml")
  @default("Input")
  direction: Direction = Input,

  @description(
    """Allow for multiple values (`false` by default).
      |
      |For input arguments, this will be treated as a list of values. For example, values
      |can be passed using the delimiter `--foo=1:2:3` or by providing the same argument 
      |multiple times `--foo 1 --foo 2`. You can use a custom delimiter by using the 
      |[`multiple_sep`](#multiple_sep) property.
      |
      |For output file arguments, the passed value needs to contain a wildcard. For example,
      |`--foo 'foo_*.txt'` will be treated as a list of files that match the pattern. Note that in Bash,
      | the wildcard will need to be in quotes (`"foo_*.txt"` or `'foo_*.txt'`) or else Bash will
      | automatically attempt to expand the expression.
      |
      |Other output arguments (e.g. integer, double, ...) are not supported yet.
      |""")
  @example(
    """- name: --my_files
      |  type: file
      |  multiple: true
      |""",
      "yaml")
  @exampleWithDescription("my_component --my_files=firstFile.csv:anotherFile.csv:yetAnother.csv", "bash", "Here's an example of how to use this:")
  @default("False")
  multiple: Boolean = false,

  @description("The delimiter character for providing [`multiple`](#multiple) values. `:` by default.")
  @example(
    """- name: --my_files
      |  type: file
      |  multiple: true
      |  multiple_sep: ";"
      |""",
      "yaml")
  @exampleWithDescription("my_component --my_files=firstFile.csv,anotherFile.csv,yetAnother.csv", "bash", "Here's an example of how to use this:")
  @default(";")
  multiple_sep: String = ";",

  dest: String = "par",
  `type`: String = "file"
) extends Argument[Path] {
  def copyArg(
    `type`: String, 
    name: String, 
    alternatives: OneOrMore[String],
    label: Option[String],
    summary: Option[String],
    description: Option[String],
    info: Json,
    example: OneOrMore[Path],
    default: OneOrMore[Path],
    required: Boolean,
    direction: Direction,
    multiple: Boolean,
    multiple_sep: String,
    dest: String
  ): Argument[Path] = {
    copy(
      name, alternatives, label, summary, description, info, example, default, 
      this.must_exist, this.create_parent, required, direction, 
      multiple, multiple_sep, dest, `type`
    )
  }
}
