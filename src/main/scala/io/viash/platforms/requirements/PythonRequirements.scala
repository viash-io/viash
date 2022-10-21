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
    |    pip: numpy
    |    github: [ jkbr/httpie, foo/bar ]
    |    url: "https://github.com/some_org/some_pkg/zipball/master"
    |""".stripMargin,
    "yaml")
case class PythonRequirements(
  @description("Sets the `--user` flag when set to true. Default: false.")
  user: Boolean = false,

  @description("Specifies which packages to install from pip.")
  @example("packages: [ numpy ]", "yaml")
  packages: OneOrMore[String] = Nil,

  @description("Specifies which packages to install from pip.")
  @example("pip: [ numpy ]", "yaml")
  pip: OneOrMore[String] = Nil,

  @description("Specifies which packages to install from PyPI using pip.")
  @example("pypi: [ numpy ]", "yaml")
  pypi: OneOrMore[String] = Nil,

  @description("Specifies which packages to install using a Git URI.")
  @example("git: [ https://some.git.repository/org/repo ]", "yaml")
  git: OneOrMore[String] = Nil,
  
  @description("Specifies which packages to install from GitHub.")
  @example("github: [ jkbr/httpie ]", "yaml")
  github: OneOrMore[String] = Nil,

  @description("Specifies which packages to install from GitLab.")
  @example("gitlab: [ foo/bar ]", "yaml")
  gitlab: OneOrMore[String] = Nil,

  @description("Specifies which packages to install using a Mercurial URI.")
  @example("mercurial: [ https://hg.myproject.org/MyProject/#egg=MyProject ]", "yaml")
  mercurial: OneOrMore[String] = Nil,

  @description("Specifies which packages to install using an SVN URI.")
  @example("svn: [ http://svn.repo/some_pkg/trunk/#egg=SomePackage ]", "yaml")
  svn: OneOrMore[String] = Nil,

  @description("Specifies which packages to install using a Bazaar URI.")
  @example("bazaar: [ http://bazaar.launchpad.net/some_pkg/some_pkg/release-0.1 ]", "yaml")
  bazaar: OneOrMore[String] = Nil,

  @description("Specifies which packages to install using a generic URI.")
  @example("url: [ https://github.com/some_org/some_pkg/zipball/master ]", "yaml")
  url: OneOrMore[String] = Nil,

  @description("Specifies a code block to run as part of the build.")
  @example("""script: |
    #  print("Running custom code")
    #  x = 1 + 1 == 2""".stripMargin('#'), "yaml")
  script: OneOrMore[String] = Nil,

  @description("Sets the `--upgrade` flag when set to true. Default: true.")
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
