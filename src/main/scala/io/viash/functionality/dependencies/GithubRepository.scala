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

@description("A GitHub repository where remote dependency components can be found.")
@example(
  """type: github
    |repo: openpipelines-bio/openpipeline
    |tag: 0.8.0
    |""".stripMargin,
  "yaml"
)
@example(
  """name: viash-testns
    |type: github
    |repo: viash-io/viash
    |tag: 0.7.1
    |path: src/test/resources/testns
    |""".stripMargin,
  "yaml"
  )
@subclass("github")
case class GithubRepository(
  name: String,

  @description("Defines the repository as a GitHub repository.")
  `type`: String = "github",

  @description("The name of the GitHub repository.")
  @example("repo: viash-io/viash", "yaml")
  repo: String,
  tag: Option[String],
  path: Option[String] = None,
  localPath: String = ""
) extends AbstractGitRepository {
  
  def copyRepo(
    name: String,
   `type`: String,
    tag: Option[String],
    path: Option[String],
    localPath: String
  ): GithubRepository = {
    copy(name, `type`, this.repo, tag, path, localPath)
  }

  override def getCheckoutUri(): String = {
    if (checkGitAuthentication(uri_nouser)) { 
      // First try https with bad user & password to disable asking credentials
      // If successful, do checkout without the dummy credentials, don't want to store them in the repo remote address
      uri
    } else if (checkGitAuthentication(uri_ssh)) {
      // Checkout with ssh key
      uri_ssh
    } else {
      uri
    }
  }

  lazy val uri = s"https://github.com/$repo.git"
  lazy val uri_ssh = s"git@github.com:$repo.git"
  val fakeCredentials = "nouser:nopass@" // obfuscate the credentials a bit so we don't trigger GitGuardian
  lazy val uri_nouser = s"https://${fakeCredentials}github.com/$repo.git"
}
