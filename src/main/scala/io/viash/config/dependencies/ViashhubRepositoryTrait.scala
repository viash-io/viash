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
import io.viash.helpers.Git

trait ViashhubRepositoryTrait extends AbstractGitRepository {

  @description("The name of the Viash-Hub repository.")
  @example("repo: openpipelines-bio/openpipeline", "yaml")
  val repo: String

  def getCheckoutUri(): String = {
    if (Git.checkGitAuthentication(uri_nouser)) { 
      // First try https with bad user & password to disable asking credentials
      // If successful, do checkout without the dummy credentials, don't want to store them in the repo remote address
      uri
    } else if (Git.checkGitAuthentication(uri_ssh)) {
      // Checkout with ssh key
      uri_ssh
    } else {
      uri
    }
  }

  lazy val fullRepo = repo.contains("/") match {
    case true => repo
    case false => s"vsh/$repo"
  }

  def getCacheIdentifier(): Option[String] =
    Some(s"viashhub-${fullRepo.replace("/", "-")}${tag.map(_.prepended('-')).getOrElse("")}")

  lazy val uri = s"https://viash-hub.com/$fullRepo.git"
  lazy val uri_ssh = s"git@viash-hub.com:$fullRepo.git"
  val fakeCredentials = "nouser:nopass@" // obfuscate the credentials a bit so we don't trigger GitGuardian
  lazy val uri_nouser = s"https://${fakeCredentials}viash-hub.com/$fullRepo.git"

  val storePath = fullRepo // no need to add 'viash-hub.com' to the store path as 'type' (vsh) will be added
}
