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
  """Specifies a package where dependency components can be found.
    |
    | - @[local](package_local): This package (default).
    | - @[git](package_git): A remote git repository.
    | - @[github](package_github): A remote GitHub repository.
    | - @[vsh](package_vsh): A Viash Hub package.
    |""")
@exampleWithDescription(
  """packages:
    |  - name: biobox
    |    type: vsh
    |    tag: 0.3.0
    |""",
    "yaml",
    "Definition of a Viash Hub package.")
@exampleWithDescription(
  """packages:
    |  - name: openpipelines-bio
    |    type: github
    |    repo: openpipelines-bio/modules
    |    tag: 0.3.0
    |""",
    "yaml",
    "Definition of a package in the component config or package config.")
@exampleWithDescription(
  """dependencies:
    |  - name: arriba
    |    package: vsh://biobox@0.3.0
    |  - name: qc/multiqc
    |    package:
    |      type: github
    |      repo: openpipelines-bio/modules
    |      tag: 0.3.0
    |""",
    "yaml",
    "Definition of dependency with a fully defined package")
@subclass("LocalPackage")
@subclass("GitPackage")
@subclass("GithubPackage")
@subclass("ViashhubPackage")
abstract class Package extends CopyablePackage[Package] {
  @description("Defines the package type. This determines how the package will be fetched and handled.")
  val `type`: String

  @description("Defines which version of the dependency component to use. Typically this can be a specific tag, branch or commit hash.")
  val tag: Option[String]

  @description("Defines a subfolder of the package to use as base to look for the dependency components.")
  val path: Option[String]

  @internalFunctionality
  @description("Local path to the package files.")
  val localPath: String

  def copyPackage(
    `type`: String = this.`type`,
    tag: Option[String] = this.tag,
    path: Option[String] = this.path,
    localPath: String = this.localPath
  ): Package

  def subOutputPath: String
}

object Package extends Logging {
  private val sugarSyntaxRegex = raw"([a-zA-Z_0-9\+]+)://([\w/\-\.:]+)(@[A-Za-z0-9][\w\-\./]*)?".r
  private def getGitTag(tag: String): Option[String] = tag match {
    case null => None
    case s => Some(s.stripPrefix("@"))
  }

  def unapply(str: String): Option[Package] = {
    str match {
      case sugarSyntaxRegex("git+https", uri, tag) =>
        Some(GitPackage(
          uri = "https://" + uri,
          tag = getGitTag(tag)
        ))
      case sugarSyntaxRegex("github", repo, tag) =>
        Some(GithubPackage(
          repo = repo,
          tag = getGitTag(tag)
        ))
      case sugarSyntaxRegex("vsh", repo, tag) =>
        Some(ViashhubPackage(
          repo = repo,
          tag = getGitTag(tag)
        ))
      case sugarSyntaxRegex("local", path, tag) =>
        Some(LocalPackage(
          path = Some(path),
          tag = getGitTag(tag)
        ))
      case "local" =>
        Some(LocalPackage())
      case _ => None
    }
  }

  def get(pkg: Package, configDir: Path, packageRootDir: Option[Path]): Package = {

    pkg match {
      case r: AbstractGitPackage => {
        val r2 = r.getSparseRepoInTemp()
        val r3 = r2.checkout()
        // Stopgap solution to be able to use built packages which were not built with dependency aware Viash version.
        // TODO remove this section once it's deemed no longer necessary
        if (Paths.get(r3.localPath, "target").toFile().exists() && !Paths.get(r3.localPath, "target", ".build.yaml").toFile().exists()) {
          warn(s"Creating temporary 'target/.build.yaml' file for ${r3.`type`} as this file seems to be missing.")
          Files.createFile(Paths.get(r3.localPath, "target", ".build.yaml"))
        }
        r3
      }
      case r: LocalPackageTrait if r.path.isDefined => {
        val localPath = r.path.get match {
          case s if s.startsWith("/") => 
            // resolve path relative to the package root
            IO.resolvePackagePath(s, packageRootDir.map(p => p.toUri())).getPath()
          case s =>
            // resolve path relative to the config file
            configDir.resolve(s).toString()
        }
        r.copyPackage(localPath = localPath)
      }
      case r => r
    }

  }
}
