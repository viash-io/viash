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

package io.viash.functionality

import io.viash.helpers.data_structures._
import io.viash.functionality.arguments.Argument
import io.viash.schemas._

@description("A grouping of the @[arguments](argument), used to display the help message.")
case class ArgumentGroup(
  @description("The name of the argument group.")
  name: String,

  @description("Description of foo`, a description of the argument group. Multiline descriptions are supported.")
  description: Option[String] = None,

  @description("List of arguments.")
  arguments: List[Argument[_]] = Nil
)
