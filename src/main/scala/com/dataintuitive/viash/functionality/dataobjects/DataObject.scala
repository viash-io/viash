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

abstract class DataObject[Type] {
  val oType: String
  val name: String
  val alternatives: List[String]
  val description: Option[String]
  val example: OneOrMore[Type]
  val default: OneOrMore[Type]
  val required: Boolean
  val direction: Direction
  val tag: Option[String]
  val multiple: Boolean
  val multiple_sep: Char

  private val pattern = "^(-*)(.*)$".r
  val pattern(otype, plainName) = name

  /** Common parameter name for this object */
  val par: String = "par_" + plainName

  /** Parameter name in bash scripts */
  val VIASH_PAR: String = "VIASH_PAR_" + plainName.toUpperCase()

  /** 
   * Access the parameters contents in a bash script,
   * taking into account that some characters need to be escaped.
   * 
   * Example: viash_par_escaped("'", """\'""", """\\\'""") 
   * results in '${VIASH_PAR//\'/\\\'}'. 
   * 
   * Sidenote: a '\' will be escaped as '\VIASH_SLASH\', so BashWrapper
   * substitutes it back for a '\' instead of escaping it as a '\\'.
   */
  def viash_par_escaped(quot: String, from: String, to: String) = {
    s"$quot$${$VIASH_PAR//$from/$to}$quot".replaceAll("\\\\", "\\\\VIASH_SLASH\\\\")
  }

  def copyDO(
    oType: String = this.oType,
    name: String = this.name,
    alternatives: List[String] = this.alternatives,
    description: Option[String] = this.description,
    example: OneOrMore[Type] = this.example,
    default: OneOrMore[Type] = this.default,
    required: Boolean = this.required,
    direction: Direction = this.direction,
    tag: Option[String] = this.tag,
    multiple: Boolean = this.multiple,
    multiple_sep: Char = this.multiple_sep
  ): DataObject[Type]

  assert(example.length <= 1 || multiple, s"Argument $name: 'example' should be length <= 1 if 'multiple' is false")
  assert(default.length <= 1 || multiple, s"Argument $name: 'default' should be length <= 1 if 'multiple' is false")
}
