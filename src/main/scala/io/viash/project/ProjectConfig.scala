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

package io.viash.project

import java.nio.file.{Files, Path, Paths}

import io.viash.schemas._
import io.viash.helpers.data_structures.OneOrMore
import io.viash.helpers.IO
import io.viash.helpers.circe._
import io.circe.Json
import java.net.URI
import io.viash.functionality.dependencies.RepositoryWithName
import io.viash.functionality.Author

@description("A Viash project configuration file. It's name should be `_viash.yaml`.")
@example(
  """viash_version: 0.9.0
    |source: src
    |target: target
    |version: 1.0
    |organization: viash-io
    |links:
    |  repository: 'https://github.com/viash-io/viash'
    |  docker_registry: 'ghcr.io'
    |config_mods: |
    |  .runners[.type == 'nextflow'].directives.tag := '$id'
    |  .runners[.type == 'nextflow'].config.script := 'includeConfig("configs/custom.config")'
    |""".stripMargin, "yaml"
)
@since("Viash 0.6.4")
case class ProjectConfig(
  @description("The name of the project.")
  @example("name: my_project", "yaml")
  @since("Viash 0.9.0")
  name: Option[String] = None,

  @description("The version of the project.")
  @example("version: 0.1.0", "yaml")
  @since("Viash 0.9.0")
  version: Option[String] = None,

  @description("A description of the project.")
  @example("description: My project", "yaml")
  @since("Viash 0.9.0")
  description: Option[String] = None,

  @description("Structured information. Can be any shape: a string, vector, map or even nested map.")
  @example(
    """info:
      |  twitter: wizzkid
      |  classes: [ one, two, three ]""".stripMargin, "yaml")
  @default("Empty")
  @since("Viash 0.9.0")
  info: Json = Json.Null,

  @description("Common repository definitions for component dependencies.")
  @example(
    """repositories:
      |  - name: openpipelines-bio
      |    type: github
      |    uri: openpipelines-bio/modules
      |    tag: 0.3.0
      |""".stripMargin,
      "yaml")
  @default("Empty")
  @since("Viash 0.9.0")
  repositories: List[RepositoryWithName] = Nil,

  @description("Which version of Viash to use.")
  @example("viash_versions: 0.6.4", "yaml")
  viash_version: Option[String] = None,

  // todo: turn this into path
  @description("Which source directory to use for the `viash ns` commands.")
  @example("source: src", "yaml")
  source: Option[String] = None,

  // todo: turn this into path
  @description("Which target directory to use for `viash ns build`.")
  @example("target: target", "yaml")
  target: Option[String] = None,

  // todo: make this a ConfigMods object
  // todo: link to config mods docs
  @description("Which config mods to apply.")
  @example("config_mods: \".functionality.name := 'foo'\"", "yaml")
  @default("Empty")
  config_mods: OneOrMore[String] = Nil,

  @description("Directory in which the _viash.yaml resides.")
  @internalFunctionality
  rootDir: Option[Path] = None,

  @description("The authors of the project.")
  @example(
    """authors:
      |  - name: Jane Doe
      |    role: [author, maintainer]
      |    email: jane@doe.com
      |    info:
      |      github: janedoe
      |      twitter: janedoe
      |      orcid: XXAABBCCXX
      |      groups: [ one, two, three ]
      |  - name: Tim Farbe
      |    roles: [author]
      |    email: tim@far.be
      |""".stripMargin, "yaml")
  @default("Empty")
  @since("Viash 0.9.0")
  authors: List[Author] = Nil,

  @description("The keywords of the project.")
  @example("keywords: [ bioinformatics, genomics ]", "yaml")
  @default("Empty")
  @since("Viash 0.9.0")
  keywords: List[String] = Nil,

  @description("The license of the project.")
  @example("license: MIT", "yaml")
  @default("Empty")
  @since("Viash 0.9.0")
  license: Option[String] = None,

  @description("The organization of the project.")
  @example("organization: viash-io", "yaml")
  @default("Empty")
  @since("Viash 0.9.0")
  organization: Option[String] = None,

  @description("References to external resources related to the project.")
  @example(
    """reference:
      |  doi: 10.1000/xx.123456.789
      |  bibtex: |
      |    @article{foo,
      |      title={Foo},
      |      author={Bar},
      |      journal={Baz},
      |      year={2024}
      |    }
      |""".stripMargin, "yaml")
  @default("Empty")
  @since("Viash 0.9.0")
  reference: ProjectConfigReferences = ProjectConfigReferences(),

  @description("External links of the project.")
  @example(
    """links:
      |  repository: "https://github.com/viash-io/viash"
      |  docker_registry: "https://ghcr.io"
      |  homepage: "https://viash.io"
      |  documentation: "https://viash.io/reference/"
      |  issue_tracker: "https://github.com/viash-io/viash/issues"
      |""".stripMargin, "yaml")
  @default("Empty")
  @since("Viash 0.9.0")
  links: ProjectConfigLinks = ProjectConfigLinks(),
)

object ProjectConfig {

  /**
    * Look for a Viash project file in a directory or its parents
    *
    * @param path The directory in which to look for a file called `_viash.yaml`
    * @return The path to the Viash project file, if found.
    */
  def findProjectFile(path: Path): Option[Path] = {
    val child = path.resolve("_viash.yaml")
    if (Files.isDirectory(path) && Files.exists(child)) {
      Some(child)
    } else {
      val parent = path.getParent()
      if (parent == null) {
        None
      } else {
        findProjectFile(path.getParent())
      }
    }
  }

  /**
    * Read the text from a Path and convert to a Json
    *
    * @param path The path to read out
    * @return A Json
    */
  def readJson(path: Path): Json = {
    // make URI
    val uri = path.toUri()

    // read yaml as string
    val projStr = IO.read(uri)
    val json0 = Convert.textToJson(projStr, path.toString())

    /* JSON 1: after inheritance */
    // apply inheritance if need be
    val json1 = json0.inherit(uri, projectDir = Some(uri))

    json1
  }

  /**
    * Read the text from a Path and convert to a ViashProject
    *
    * @param path The path to read out
    * @return A parsed project config
    */
  def read(
    path: Path
  ): ProjectConfig = {
    val json = readJson(path)

    /* PROJECT 0: converted from json */
    // convert Json into ViashProject
    val proj0 = Convert.jsonToClass[ProjectConfig](json, path.toString())

    /* PROJECT 1: make resources absolute */
    // make paths absolute
    // todo: move to separate helper function
    def rela(parent: Path, path: String): String = {
      val pth = Paths.get(path).toFile
      if (pth.isAbsolute) {
        path
      } else {
        parent.resolve(path).toString
      }
    }
    val source = proj0.source.map(rela(path.getParent(), _))
    val target = proj0.target.map(rela(path.getParent(), _))

    // copy resources with updated paths into config and return
    val proj1 = proj0.copy(
      source = source,
      target = target,
      rootDir = Some(path.getParent())
    )

    proj1
  }

  /**
    * Look for a Viash project file in a directory or its parents
    * and convert to a ViashProject object
    *
    * @param path The directory in which to look for a file called `_viash.yaml`
    * @return The project config, if found
    */
  def findViashProject(path: Path): ProjectConfig = {
    findProjectFile(path) match {
      case Some(projectPath) =>
        read(projectPath)
      case None => ProjectConfig()
    }
  }
}