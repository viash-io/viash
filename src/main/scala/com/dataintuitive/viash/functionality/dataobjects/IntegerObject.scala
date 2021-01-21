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

case class IntegerObject(
  name: String,
  alternatives: List[String] = Nil,
  description: Option[String] = None,
  default: Option[Int] = None,
  required: Boolean = false,
  tag: Option[String] = None,
  direction: Direction = Input,
  multiple: Boolean = false,
  multiple_sep: Char = ':'
) extends DataObject[Int] {
  override val `type` = "integer"
}
