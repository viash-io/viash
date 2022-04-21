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

import com.dataintuitive.viash.helpers.Circe._

case class PythonRequirements(
  user: Boolean = false,
  packages: OneOrMore[String] = Nil,
  pip: OneOrMore[String] = Nil,
  pypi: OneOrMore[String] = Nil,
  git: OneOrMore[String] = Nil,
  github: OneOrMore[String] = Nil,
  gitlab: OneOrMore[String] = Nil,
  mercurial: OneOrMore[String] = Nil,
  svn: OneOrMore[String] = Nil,
  bazaar: OneOrMore[String] = Nil,
  url: OneOrMore[String] = Nil,
  script: OneOrMore[String] = Nil,
  upgrade: Boolean = true,
  oType: String = "python"
) extends Requirements {
  assert(script.forall(!_.contains("'")))

  private val userFlag = if (user) " --user" else ""
  private val upgradeFlag = if (upgrade) " --upgrade" else ""

  private def generateCommands(prefix: String, values: List[String], postFix: String = "") = {
    values match {
      case Nil => Nil
      case packs =>
        List(packs.mkString(
          s"""pip install$userFlag$upgradeFlag --no-cache-dir "$prefix""",
          postFix + "\" \"" + prefix,
          postFix + "\""))
    }
  }

  def installCommands: List[String] = {
    val installPip =
      s"""pip install$userFlag --upgrade pip"""

    val installPipPackages = generateCommands("", pip ::: packages)
    val installPypiPackages = generateCommands("", pypi ::: packages)
    val installGitPackages = generateCommands("git+", git)
    val installGithubPackages = generateCommands("git+https://github.com/", github)
    val installGitlabPackages = generateCommands("git+https://gitlab.com/", gitlab)
    val installMercurialPackages = generateCommands("hg+", mercurial)
    val installSvnPackages = generateCommands("svn+", svn)
    val installBazaarPackages = generateCommands("bzr+", bazaar)
    val installUrlPackages = generateCommands("", url)

    val installScript =
      if (script.nonEmpty) {
        script.map { line =>
          s"""python -c '$line'"""
        }
      } else {
        Nil
      }

    installPip :: installPipPackages ::: installPypiPackages ::: installGitPackages ::: installGithubPackages ::: installGitlabPackages :::
      installMercurialPackages ::: installSvnPackages ::: installBazaarPackages ::: installUrlPackages ::: installScript
  }
}
