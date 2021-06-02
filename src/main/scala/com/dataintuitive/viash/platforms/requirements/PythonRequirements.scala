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

case class PythonRequirements(
  user: Boolean = false,
  packages: List[String] = Nil,
  pip: List[String] = Nil,
  git: List[String] = Nil,
  github: List[String] = Nil,
  gitlab: List[String] = Nil,
  mercurial: List[String] = Nil,
  svn: List[String] = Nil,
  bazaar: List[String] = Nil,
  url: List[String] = Nil,
  script: List[String] = Nil,
  oType: String = "python"
) extends Requirements {
  assert(script.forall(!_.contains("'")))

  private val userFlag = if (user) " --user" else ""

  private def generateCommands(prefix: String, values: List[String], postFix: String = "") = {
    values match {
      case Nil => Nil
      case packs =>
        List(packs.mkString(
          s"""pip install$userFlag --no-cache-dir "$prefix""",
          postFix + "\" \"" + prefix,
          postFix + "\""))
    }
  }

  def installCommands: List[String] = {
    val installPip =
      s"""pip install$userFlag --upgrade pip"""

    val installPipPackages = generateCommands("", pip ::: packages)
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

    installPip :: installPipPackages ::: installGitPackages ::: installGithubPackages ::: installGitlabPackages :::
      installMercurialPackages ::: installSvnPackages ::: installBazaarPackages ::: installUrlPackages ::: installScript
  }
}
