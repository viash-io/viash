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

package io.viash.functionality.dependencies

import io.viash.schemas._
import io.viash.helpers.IO
import io.viash.helpers.Exec
import java.io.File
import java.nio.file.Paths

@description("A Git repository where remote dependency components can be found.")
@example(
  """name: openpipeline
    |type: git
    |uri: git+https://github.com/openpipelines-bio/openpipeline.git
    |tag: 0.8.0
    |""".stripMargin,
  "yaml"
)
@example(
  """name: viash-testns
    |type: git
    |uri: git+https://gitlab.com/viash-io/viash.git
    |tag: 0.7.1
    |path: src/test/resources/testns
    |""".stripMargin,
  "yaml"
  )
@subclass("gitwithname")
case class GitRepositoryWithName(
  name: String,

  @description("Defines the repository as a Git repository.")
  `type`: String = "git",

  @description("The URI of the Git repository.")
  @example("uri: \"git+https://github.com/openpipelines-bio/openpipeline.git\"", "yaml")
  uri: String,
  tag: Option[String],
  path: Option[String] = None,
  localPath: String = ""
) extends RepositoryWithName with GitRepositoryTrait {
  
  def copyRepo(
   `type`: String,
    tag: Option[String],
    path: Option[String],
    localPath: String
  ): GitRepositoryWithName = {
    copy("", `type`, uri, tag, path, localPath)
  }

  def copyRepoWithName(
    name: String,
   `type`: String,
    tag: Option[String],
    path: Option[String],
    localPath: String
  ): GitRepositoryWithName = {
    copy(name, `type`, uri, tag, path, localPath)
  }

}
