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

package com.dataintuitive.viash.platforms.requirements

case class JavaScriptRequirements(
  packages: List[String] = Nil,
  npm: List[String] = Nil,
  git: List[String] = Nil,
  github: List[String] = Nil,
  url: List[String] = Nil
) extends Requirements {

  val `type` = "javascript"

  private def generateCommands(prefix: String, values: List[String]) = {
    values match {
      case Nil => Nil
      case packs =>
        List(packs.mkString(
          "npm install -g \"" + prefix,
          "\" \"" + prefix,
          "\""))
    }
  }

  def installCommands: List[String] = {
    val installNpmPackages = generateCommands("", npm ::: packages)
    val installGitPackages = generateCommands("git+", git)
    val installGithubPackages = generateCommands("", github)
    val installUrlPackages = generateCommands("", url)

    installNpmPackages ::: installGitPackages ::: installGithubPackages ::: installUrlPackages
  }
}
