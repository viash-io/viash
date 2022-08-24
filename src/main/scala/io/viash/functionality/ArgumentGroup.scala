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

import io.viash.helpers.Circe.OneOrMore
import io.viash.functionality.arguments.Argument

case class ArgumentGroup(
  name: String,
  description: Option[String] = None,
  arguments: OneOrMore[Either[String, Argument[_]]] = Nil
) {
  if (arguments.exists(_.isLeft)) {
    Console.err.println("Notice: The '.arguments' field should be a list of arguments and should not contain strings.")
  }

  def stringArguments: OneOrMore[String] = {
    arguments.flatMap{_.left.toOption}
  }

  def argumentArguments: OneOrMore[Argument[_]] = {
    arguments.flatMap{_.right.toOption}
  }
}
