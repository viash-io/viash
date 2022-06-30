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

abstract class BooleanArgument extends Argument[Boolean] {
  val flagValue: Option[Boolean]
}

case class BooleanArgumentRegular(
  name: String,
  alternatives: OneOrMore[String] = Nil,
  description: Option[String] = None,
  example: OneOrMore[Boolean] = Nil,
  default: OneOrMore[Boolean] = Nil,
  required: Boolean = false,
  direction: Direction = Input,
  multiple: Boolean = false,
  multiple_sep: Char = ':',
  `type`: String = "boolean"
) extends BooleanArgument {

  val flagValue: Option[Boolean] = None

  def copyArg(
    `type`: String, 
    name: String, 
    alternatives: OneOrMore[String],
    description: Option[String],
    example: OneOrMore[Boolean],
    default: OneOrMore[Boolean],
    required: Boolean,
    direction: Direction,
    multiple: Boolean,
    multiple_sep: Char
  ): Argument[Boolean] = {
    copy(name, alternatives, description, example, default, required, direction, multiple, multiple_sep, `type`)
  }
}

case class BooleanArgumentTrue(
  name: String,
  alternatives: OneOrMore[String] = Nil,
  description: Option[String] = None,
  direction: Direction = Input,
  `type`: String = "boolean_true"
) extends BooleanArgument {

  val required: Boolean = false
  val flagValue: Option[Boolean] = Some(true)
  val default: OneOrMore[Boolean] = Nil
  val multiple: Boolean = false
  val multiple_sep: Char = ':'
  val example: OneOrMore[Boolean] = Nil

  def copyArg(
    `type`: String, 
    name: String, 
    alternatives: OneOrMore[String],
    description: Option[String],
    default: OneOrMore[Boolean],
    example: OneOrMore[Boolean],
    required: Boolean,
    direction: Direction,
    multiple: Boolean,
    multiple_sep: Char
  ): Argument[Boolean] = {
    copy(name, alternatives, description, direction, `type`)
  }
}

case class BooleanArgumentFalse(
  name: String,
  alternatives: OneOrMore[String] = Nil,
  description: Option[String] = None,
  direction: Direction = Input,
  `type`: String = "boolean_false"
) extends BooleanArgument {

  val required: Boolean = false
  val flagValue: Option[Boolean] = Some(false)
  val default: OneOrMore[Boolean] = Nil
  val multiple: Boolean = false
  val multiple_sep: Char = ':'
  val example: OneOrMore[Boolean] = Nil

  def copyArg(
    `type`: String, 
    name: String, 
    alternatives: OneOrMore[String],
    description: Option[String],
    default: OneOrMore[Boolean],
    example: OneOrMore[Boolean],
    required: Boolean,
    direction: Direction,
    multiple: Boolean,
    multiple_sep: Char
  ): Argument[Boolean] = {
    copy(name, alternatives, description, direction, `type`)
  }
}
