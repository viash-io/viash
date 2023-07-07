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

@description("A Viash-Hub repository where remote dependency components can be found.")
@example(
  """type: viashhub
    |repo: openpipelines-bio/openpipeline
    |tag: 0.8.0
    |""".stripMargin,
  "yaml"
)
@example(
  """name: viash-testns
    |type: viashhub
    |repo: openpipelines-bio/openpipeline
    |tag: 0.7.1
    |path: src/test/resources/testns
    |""".stripMargin,
  "yaml"
  )
case class ViashhubRepository(
  name: String,

  @description("Defines the repository as a Viash-Hub repository.")
  `type`: String = "vsh",

  @description("The name of the Viash-Hub repository.")
  @example("repo: openpipelines-bio/openpipeline", "yaml")
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
  ): ViashhubRepository = {
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

  lazy val uri = s"https://viash-hub.com/$repo.git"
  lazy val uri_ssh = s"git@viash-hub.com:$repo.git"
  val fakeCredentials = "nouser:nopass@" // obfuscate the credentials a bit so we don't trigger GitGuardian
  lazy val uri_nouser = s"https://${fakeCredentials}viash-hub.com/$repo.git"
}
