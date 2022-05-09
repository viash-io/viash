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

package com.dataintuitive.viash.functionality

import com.dataintuitive.viash.helpers.Circe._

case class Author(
  name: String,
  email: Option[String] = None,
  roles: OneOrMore[String] = Nil,
  props: Map[String, String] = Map.empty[String, String]
) {
  override def toString: String = {
    name +
      email.map(" <" + _ + ">").getOrElse("") +
      { if (roles.isEmpty) "" else " (" + roles.mkString(", ") + ")"} +
      { if (props.isEmpty) "" else " {" + props.map(a => a._1 + ": " + a._2).mkString(", ") + "}"}
  }
}
