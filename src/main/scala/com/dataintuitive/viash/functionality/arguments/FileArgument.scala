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

import java.nio.file.Path
import com.dataintuitive.viash.helpers.Circe.OneOrMore

case class FileArgument(
  name: String,
  alternatives: OneOrMore[String] = Nil,
  description: Option[String] = None,
  example: OneOrMore[Path] = Nil,
  default: OneOrMore[Path] = Nil,
  must_exist: Boolean = false,
  required: Boolean = false,
  direction: Direction = Input,
  multiple: Boolean = false,
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