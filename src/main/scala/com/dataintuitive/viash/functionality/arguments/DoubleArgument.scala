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

package com.dataintuitive.viash.functionality.arguments

import com.dataintuitive.viash.helpers.Circe.OneOrMore
import com.dataintuitive.viash.helpers.description

@description("")
case class DoubleArgument(
  @description("")
  name: String,
  @description("")
  alternatives: OneOrMore[String] = Nil,
  @description("")
  description: Option[String] = None,
  @description("")
  example: OneOrMore[Double] = Nil,
  @description("")
  default: OneOrMore[Double] = Nil,
  @description("")
  required: Boolean = false,
  @description("")
  min: Option[Double] = None,
  @description("")
  max: Option[Double] = None,
  @description("")
  direction: Direction = Input,
  @description("")
  multiple: Boolean = false,
  @description("")
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