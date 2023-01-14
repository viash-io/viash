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

case class ArgumentGroup(
  name: String,
  description: Option[String] = None,
  arguments: List[Either[String, Argument[_]]] = Nil
) {
  if (arguments.exists(_.isLeft)) {
    Console.err.println(
      f"""Warning: specifying strings in the .argument field of argument group '$name' is deprecated. The .arguments field of an argument group should only contain arguments.
         |To solve this issue, copy the arguments ${arguments.flatMap(_.left.toOption).mkString("'", "', '", "'")} directly into the argument group.""".stripMargin
    )

  }

  def stringArguments: List[String] = {
    arguments.flatMap{_.left.toOption}
  }

  def argumentArguments: List[Argument[_]] = {
    arguments.flatMap{_.toOption}
  }
}
