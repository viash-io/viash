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

package com.dataintuitive.viash.functionality.dataobjects

import com.dataintuitive.viash.helpers.Circe.OneOrMore

case class StringObject(
  name: String,
  alternatives: List[String] = Nil,
  description: Option[String] = None,
  example: OneOrMore[String] = Nil,
  default: OneOrMore[String] = Nil,
  required: Boolean = false,
  values: List[String] = Nil,
  direction: Direction = Input,
  multiple: Boolean = false,
  multiple_sep: Char = ':',
  `type`: String = "string"
) extends DataObject[String] {
  def copyDO(
    `type`: String, 
    name: String, 
    alternatives: List[String],
    description: Option[String],
    example: OneOrMore[String],
    default: OneOrMore[String],
    required: Boolean,
    direction: Direction,
    multiple: Boolean,
    multiple_sep: Char
  ): DataObject[String] = {
    copy(name, alternatives, description, example, default, required, values, direction, multiple, multiple_sep, `type`)
  }
}