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

case class DoubleArgument(
  name: String,
  alternatives: OneOrMore[String] = Nil,
  description: Option[String] = None,
  example: OneOrMore[Double] = Nil,
  default: OneOrMore[Double] = Nil,
  required: Boolean = false,
  min: Option[Double] = None,
  max: Option[Double] = None,
  direction: Direction = Input,
  multiple: Boolean = false,
  multiple_sep: Char = ':',
  `type`: String = "double"
) extends Argument[Double] {
  def copyArg(
    `type`: String, 
    name: String, 
    alternatives: OneOrMore[String],
    description: Option[String],
    example: OneOrMore[Double],
    default: OneOrMore[Double],
    required: Boolean,
    direction: Direction,
    multiple: Boolean,
    multiple_sep: Char
  ): Argument[Double] = {
    copy(name, alternatives, description, example, default, required, this.min, this.max, direction, multiple, multiple_sep, `type`)
  }
}