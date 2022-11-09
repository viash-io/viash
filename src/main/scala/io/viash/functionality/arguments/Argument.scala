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

import io.circe.Json
import io.viash.helpers.data_structures._
import io.viash.schemas._

abstract class Argument[Type] {
  @description("Specifies the type of the argument.")
  val `type`: String
  val name: String
  val alternatives: OneOrMore[String]
  val description: Option[String]
  val info: Json
  val example: OneOrMore[Type]
  val default: OneOrMore[Type]
  val required: Boolean
  val direction: Direction
  val multiple: Boolean
  val multiple_sep: String
  
  @internalFunctionality
  val dest: String

  private val pattern = "^(-*)(.*)$".r
  val pattern(flags, plainName) = name

  /** Common parameter name for this argument */
  val par: String = dest + "_" + plainName

  /** Parameter name in bash scripts */
  val VIASH_PAR: String = "VIASH_" + dest.toUpperCase + "_" + plainName.toUpperCase()

  def copyArg(
    `type`: String = this.`type`,
    name: String = this.name,
    alternatives: OneOrMore[String] = this.alternatives,
    description: Option[String] = this.description,
    info: Json = this.info,
    example: OneOrMore[Type] = this.example,
    default: OneOrMore[Type] = this.default,
    required: Boolean = this.required,
    direction: Direction = this.direction,
    multiple: Boolean = this.multiple,
    multiple_sep: String = this.multiple_sep,
    dest: String = this.dest
  ): Argument[Type]

  assert(example.length <= 1 || multiple, s"Argument $name: 'example' should be length <= 1 if 'multiple' is false")
  assert(default.length <= 1 || multiple, s"Argument $name: 'default' should be length <= 1 if 'multiple' is false")
}
