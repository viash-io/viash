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

import io.viash.helpers.Circe._
import io.viash.schemas._

@description("Specify which Python packages should be available in order to run the component.")
@example(
  """setup:
    |  - type: python
    |    pip: [ numpy ]
    |    git: [ https://some.git.repository/org/repo ]
    |    github: [ jkbr/httpie ]
    |    gitlab: [ foo/bar ]
    |    mercurial: [ http://... ]
    |    svn: [ http://...]
    |    bazaar: [ http://... ]
    |    url: [ http://... ]
    |""".stripMargin,
    "yaml")
case class PythonRequirements(
  @description("Sets the `--user` flag when set to true. Default: false")
  user: Boolean = false,

  @description("Specifies which packages to install from pip.")
  packages: OneOrMore[String] = Nil,

  @description("Specifies which packages to install from pip.")
  pip: OneOrMore[String] = Nil,

  @description("Specifies which packages to install from pip.")
  pypi: OneOrMore[String] = Nil,

  @description("Specifies which packages to install using a git uri.")
  git: OneOrMore[String] = Nil,
  
  @description("Specifies which packages to install from github.")
  github: OneOrMore[String] = Nil,

  @description("Specifies which packages to install from gitlab.")
  gitlab: OneOrMore[String] = Nil,

  @description("Specifies which packages to install using a mercurial uri.")
  mercurial: OneOrMore[String] = Nil,

  @description("Specifies which packages to install using an svn uri.")
  svn: OneOrMore[String] = Nil,

  @description("Specifies which packages to install using a bazaar uri.")
  bazaar: OneOrMore[String] = Nil,

  @description("Specifies which packages to install using a generic uri.")
  url: OneOrMore[String] = Nil,

  @description("Specifies which scripts to run as part of the build.")
  script: OneOrMore[String] = Nil,

  @description("Sets the `--upgrade` flag when set to true. Default: true")
  upgrade: Boolean = true,
  `type`: String = "python"
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

    val installPipPackages = generateCommands("", pip ::: packages ::: pypi)
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
