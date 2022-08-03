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
import io.viash.helpers.description
import io.viash.helpers.example
import io.viash.helpers.exampleWithDescription

@description("")
case class StringArgument(
  @description("")
  name: String,
  @description("")
  alternatives: OneOrMore[String] = Nil,
  @description("")
  description: Option[String] = None,
  
  @description("an example with examples! examples all the way down!")
  @example("example1 without descr", "yaml")
  @example("example2 without descr", "yaml")
  @exampleWithDescription("example3 with descr", "yaml", "whoa!")
  @exampleWithDescription("example4 with descr", "bloop", "nice!")
  example: OneOrMore[String] = Nil,
  @description("")
  default: OneOrMore[String] = Nil,
  @description("")
  required: Boolean = false,
  @description("")
  choices: List[String] = Nil,
  @description("")
  direction: Direction = Input,
  @description("")
  multiple: Boolean = false,
  @description("")
  multiple_sep: Char = ':',
  @description("")
  `type`: String = "string"
) extends Argument[String] {
  def copyArg(
    `type`: String, 
    name: String, 
    alternatives: OneOrMore[String],
    description: Option[String],
    example: OneOrMore[String],
    default: OneOrMore[String],
    required: Boolean,
    direction: Direction,
    multiple: Boolean,
    multiple_sep: Char
  ): Argument[String] = {
    copy(name, alternatives, description, example, default, required, this.choices, direction, multiple, multiple_sep, `type`)
  }
}
