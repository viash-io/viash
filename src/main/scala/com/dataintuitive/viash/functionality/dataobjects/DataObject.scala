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

abstract class DataObject[Type] {
  val `type`: String
  val name: String
  val alternatives: List[String]
  val description: Option[String]
  val default: Option[Type]
  val required: Boolean
  val direction: Direction
  val tag: Option[String]
  val multiple: Boolean
  val multiple_sep: Char

  private val pattern = "^(-*)(.*)$".r
  val pattern(otype, plainName) = name

  val par: String = "par_" + plainName
  val VIASH_PAR: String = "VIASH_PAR_" + plainName.toUpperCase()
}
