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

@description("")
case class IntegerArgument(
  @description("")
  name: String,
  @description("")
  alternatives: OneOrMore[String] = Nil,
  @description("")
  description: Option[String] = None,
  @description("")
  example: OneOrMore[Int] = Nil,
  @description("")
  default: OneOrMore[Int] = Nil,
  @description("")
  required: Boolean = false,
  @description("")
  choices: List[Int] = Nil,
  @description("")
  min: Option[Int] = None,
  @description("")
  max: Option[Int] = None,
  @description("")
  direction: Direction = Input,
  @description("")
  multiple: Boolean = false,
  @description("")
  multiple_sep: Char = ':',
  @description("")
  `type`: String = "integer"
) extends Argument[Int] {
  def copyArg(
    `type`: String, 
    name: String, 
    alternatives: OneOrMore[String],
    description: Option[String],
    example: OneOrMore[Int],
    default: OneOrMore[Int],
    required: Boolean,
    direction: Direction,
    multiple: Boolean,
    multiple_sep: Char
  ): Argument[Int] = {
    copy(name, alternatives, description, example, default, required, this.choices, this.min, this.max, direction, multiple, multiple_sep, `type`)
  }
}
