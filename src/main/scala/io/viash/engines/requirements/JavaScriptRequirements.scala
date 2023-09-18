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

package io.viash.engines.requirements

import io.viash.helpers.data_structures._
import io.viash.schemas._

@description("Specify which JavaScript packages should be available in order to run the component.")
@example(
  """setup:
    |  - type: javascript
    |    npm: packagename
    |    git: "https://some.git.repository/org/repo"
    |    github: "owner/repository"
    |    url: "https://github.com/org/repo/archive/HEAD.zip"
    |""".stripMargin,
    "yaml")
@subclass("javascript")
case class JavaScriptRequirements(
  @description("Specifies which packages to install from npm.")
  @example("packages: [ packagename ]", "yaml")
  @default("Empty")
  packages: OneOrMore[String] = Nil,

  @description("Specifies which packages to install from npm.")
  @example("npm: [ packagename ]", "yaml")
  @default("Empty")
  npm: OneOrMore[String] = Nil,

  @description("Specifies which packages to install using a Git URI.")
  @example("git: [ https://some.git.repository/org/repo ]", "yaml")
  @default("Empty")
  git: OneOrMore[String] = Nil,

  @description("Specifies which packages to install from GitHub.")
  @example("github: [ owner/repository ]", "yaml")
  @default("Empty")
  github: OneOrMore[String] = Nil,

  @description("Specifies which packages to install using a generic URI.")
  @example("url: [ https://github.com/org/repo/archive/HEAD.zip ]", "yaml")
  @default("Empty")
  url: OneOrMore[String] = Nil,
  `type`: String = "javascript"
) extends Requirements {
  private def generateCommands(prefix: String, values: List[String]) = {
    values.toList match {
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
