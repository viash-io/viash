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

package io.viash.platforms.requirements

import io.viash.helpers.data_structures._
import io.viash.schemas._

@description("Specify which apt packages should be available in order to run the component.")
@example(
  """setup:
    |  - type: apt
    |    packages: [ sl ]
    |""".stripMargin,
    "yaml")
case class AptRequirements(
  @description("Specifies which packages to install.")
  @example("packages: [ sl ]", "yaml")
  @default("Empty")
  packages: OneOrMore[String] = Nil,

  @description("If `false`, the Debian frontend is set to non-interactive (recommended). Default: false.")
  @default("False")
  interactive: Boolean = false,
  `type`: String = "apt"
) extends Requirements {
  def installCommands: List[String] = {
    val aptUpdate =
      """apt-get update"""

    val interactiveEnv = if (!interactive) "DEBIAN_FRONTEND=noninteractive " else ""
    val installPackages =
      packages.toList match {
        case Nil => Nil
        case packs =>
          List(packs.mkString(
            s"${interactiveEnv}apt-get install -y ",
            " ",
            ""
          ))
      }

    val clean = "rm -rf /var/lib/apt/lists/*"

    aptUpdate :: installPackages ::: List(clean)
  }
}
