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
    |""")
@exampleWithDescription(
  """repositories:
    |  - name: openpipelines-bio
    |    type: github
    |    repo: openpipelines-bio/modules
    |    tag: 0.3.0
    |""",
    "yaml",
    "Definition of a repository in the component config or package config.")
@exampleWithDescription(
  """dependencies:
    |  - name: qc/multiqc
    |    repository: 
    |      type: github
    |      repo: openpipelines-bio/modules
    |      tag: 0.3.0
    |""",
    "yaml",
    "Definition of dependency with a fully defined repository")
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

object Repository extends Logging {
  private val sugarSyntaxRegex = raw"([a-zA-Z_0-9\+]+)://([\w/\-\.:]+)(@[A-Za-z0-9][\w\./]*)?".r
  private def getGitTag(tag: String): Option[String] = tag match {
    case null => None
    case s => Some(s.stripPrefix("@"))
  }

  def unapply(str: String): Option[Repository] = {
    str match {
      case sugarSyntaxRegex("git+https", uri, tag) =>
        Some(GitRepository(
          uri = "https://" + uri,
          tag = getGitTag(tag)
        ))
      case sugarSyntaxRegex("github", repo, tag) =>
        Some(GithubRepository(
          repo = repo,
          tag = getGitTag(tag)
        ))
      case sugarSyntaxRegex("vsh", repo, tag) =>
        Some(ViashhubRepository(
          repo = repo,
          tag = getGitTag(tag)
        ))
      case sugarSyntaxRegex("local", path, tag) =>
        Some(LocalRepository(
          path = Some(path),
          tag = getGitTag(tag)
        ))
      case "local" =>
        Some(LocalRepository())
      case _ => None
    }
  }

  def get(repo: Repository, configDir: Path, packageRootDir: Option[Path]): Repository = {

    repo match {
      case r: AbstractGitRepository => {
        val r2 = r.getSparseRepoInTemp()
        val r3 = r2.checkout()
        // Stopgap solution to be able to use built repositories which were not built with dependency aware Viash version.
        // TODO remove this section once it's deemed no longer necessary
        if (Paths.get(r3.localPath, "target").toFile().exists() && !Paths.get(r3.localPath, "target", ".build.yaml").toFile().exists()) {
          warn(s"Creating temporary 'target/.build.yaml' file for ${r3.`type`} as this file seems to be missing.")
          Files.createFile(Paths.get(r3.localPath, "target", ".build.yaml"))
        }
        r3
      }
      case r: LocalRepositoryTrait if r.path.isDefined => {
        val localPath = r.path.get match {
          case s if s.startsWith("/") => 
            // resolve path relative to the package root
            IO.resolvePackagePath(s, packageRootDir.map(p => p.toUri())).getPath()
          case s =>
            // resolve path relative to the config file
            configDir.resolve(s).toString()
        }
        r.copyRepo(localPath = localPath)
      }
      case r => r
    }

  }
}
