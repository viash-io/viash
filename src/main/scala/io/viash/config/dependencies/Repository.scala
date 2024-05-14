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

package io.viash.config.dependencies

import io.viash.helpers.{IO, Logging}
import io.viash.schemas._
import java.nio.file.{Path, Paths, Files}

@description(
  """Specifies a repository where dependency components can be found.
    |
    | - @[local](repo_local)
    | - @[git](repo_git)
    | - @[github](repo_github)
    | - @[vsh](repo_vsh)
    |""".stripMargin)
@example(
  """repositories:
    |  - name: openpipelines-bio
    |    type: github
    |    repo: openpipelines-bio/modules
    |    tag: 0.3.0
    |""".stripMargin,
    "yaml")
@subclass("LocalRepository")
@subclass("GitRepository")
@subclass("GithubRepository")
@subclass("ViashhubRepository")
abstract class Repository extends CopyableRepo[Repository] {
  @description("Defines the repository type. This determines how the repository will be fetched and handled.")
  val `type`: String

  @description("Defines which version of the dependency component to use. Typically this can be a specific tag, branch or commit hash.")
  val tag: Option[String]

  @description("Defines a subfolder of the repository to use as base to look for the dependency components.")
  val path: Option[String]

  @internalFunctionality
  @description("Local path to the repository files.")
  val localPath: String

  def copyRepo(
    `type`: String = this.`type`,
    tag: Option[String] = this.tag,
    path: Option[String] = this.path,
    localPath: String = this.localPath
  ): Repository

  def subOutputPath: String
}
