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

abstract class BooleanObject extends DataObject[Boolean] {
  val flagValue: Option[Boolean]
}

case class BooleanObjectRegular(
  name: String,
  alternatives: List[String] = Nil,
  description: Option[String] = None,
  example: Option[Boolean] = None,
  default: Option[Boolean] = None,
  required: Boolean = false,
  tag: Option[String] = None,
  direction: Direction = Input,
  multiple: Boolean = false,
  multiple_sep: Char = ':',
  oType: String = "boolean"
) extends BooleanObject {

  val flagValue: Option[Boolean] = None

  def copyDO(
    oType: String, 
    name: String, 
    alternatives: List[String],
    description: Option[String],
    example: Option[Boolean],
    default: Option[Boolean],
    required: Boolean,
    direction: Direction,
    tag: Option[String],
    multiple: Boolean,
    multiple_sep: Char
  ): DataObject[Boolean] = {
    copy(name, alternatives, description, example, default, required, tag, direction, multiple, multiple_sep, oType)
  }
}

case class BooleanObjectTrue(
  name: String,
  alternatives: List[String] = Nil,
  description: Option[String] = None,
  tag: Option[String] = None,
  direction: Direction = Input,
  oType: String = "boolean_true"
) extends BooleanObject {

  val required: Boolean = false
  val flagValue: Option[Boolean] = Some(true)
  val default: Option[Boolean] = None
  val multiple: Boolean = false
  val multiple_sep: Char = ':'
  val example: Option[Boolean] = None

  def copyDO(
    oType: String, 
    name: String, 
    alternatives: List[String],
    description: Option[String],
    default: Option[Boolean],
    required: Boolean,
    direction: Direction,
    tag: Option[String],
    multiple: Boolean,
    multiple_sep: Char
  ): DataObject[Boolean] = {
    copy(name, alternatives, description, example, tag, direction, oType)
  }
}

case class BooleanObjectFalse(
  name: String,
  alternatives: List[String] = Nil,
  description: Option[String] = None,
  tag: Option[String] = None,
  direction: Direction = Input,
  oType: String = "boolean_false"
) extends BooleanObject {

  val required: Boolean = false
  val flagValue: Option[Boolean] = Some(false)
  val default: Option[Boolean] = None
  val multiple: Boolean = false
  val multiple_sep: Char = ':'
  val example: Option[Boolean] = None

  def copyDO(
    oType: String, 
    name: String, 
    alternatives: List[String],
    description: Option[String],
    default: Option[Boolean],
    required: Boolean,
    direction: Direction,
    tag: Option[String],
    multiple: Boolean,
    multiple_sep: Char
  ): DataObject[Boolean] = {
    copy(name, alternatives, description, example, tag, direction, oType)
  }
}
