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

package io.viash.config

import io.viash.helpers.data_structures._

import io.circe.Json
import io.viash.schemas._

@description("Author metadata.")
@since("Viash 0.3.2")
@example(
  """name: Jane Doe
    |role: [author, maintainer]
    |email: jane@doe.com
    |info:
    |  github: janedoe
    |  twitter: janedoe
    |  orcid: XXAABBCCXX
    |  groups: [ one, two, three ]
    |""".stripMargin, "yaml")
case class Author(
  @description("Full name of the author, usually in the name of FirstName MiddleName LastName.")
  name: String,

  @description("E-mail of the author.")
  email: Option[String] = None,

  @description(
    """Role of the author. Suggested items:
      |
      |* `"author"`: Authors who have made substantial contributions to the component.
      |* `"maintainer"`: The maintainer of the component.
      |* `"contributor"`: Authors who have made smaller contributions (such as code patches etc.).
      |""".stripMargin)
  @default("Empty")
  roles: OneOrMore[String] = Nil,

  @description("Structured information. Can be any shape: a string, vector, map or even nested map.")
  @since("Viash 0.7.4")
  @default("Empty")
  info: Json = Json.Null
) {
  @description("Author properties. Must be a map of strings.")
  @removed("Use `info` instead.", "0.7.4", "0.8.0")
  @default("Empty")
  private val props: Map[String, String] = Map.empty

  override def toString: String = {
    name +
      email.map(" <" + _ + ">").getOrElse("") +
      { if (roles.isEmpty) "" else " (" + roles.mkString(", ") + ")"}
  }
}
