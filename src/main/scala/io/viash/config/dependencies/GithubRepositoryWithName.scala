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

import io.viash.schemas._
import io.viash.helpers.IO
import io.viash.helpers.Exec
import java.io.File
import java.nio.file.Paths

@description("A GitHub repository where remote dependency components can be found.")
@example(
  """name: openpipeline
    |type: github
    |repo: openpipelines-bio/openpipeline
    |tag: 0.8.0
    |""",
  "yaml"
)
@example(
  """name: viash-testns
    |type: github
    |repo: viash-io/viash
    |tag: 0.7.1
    |path: src/test/resources/testns
    |""",
  "yaml"
  )
@subclass("githubwithname")
case class GithubRepositoryWithName(
  name: String,

  @description("Defines the repository as a GitHub repository.")
  `type`: String = "github",

  repo: String,
  tag: Option[String],
  path: Option[String] = None,
  localPath: String = ""
) extends RepositoryWithName with GithubRepositoryTrait {
  
  def copyRepo(
   `type`: String,
    tag: Option[String],
    path: Option[String],
    localPath: String
  ): GithubRepositoryWithName = {
    copy("", `type`, this.repo, tag, path, localPath)
  }

  def withoutName = GithubRepository(`type`, repo, tag, path, localPath)

}
