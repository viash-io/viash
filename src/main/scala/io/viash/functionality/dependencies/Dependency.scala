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

import java.nio.file.{Path, Paths}
import io.viash.config.Config
import io.viash.schemas._
import java.nio.file.Files
import io.viash.ViashNamespace

@description(
  """Specifies a Viash component (script or executable) that should be made available for the code defined in the functionality.
    |The dependency components are collected and copied to the output folder during the Viash build step.
    |""".stripMargin)
@exampleWithDescription(
  """name: qc/multiqc
    |  repository: 
    |    type: github
    |    uri: openpipelines-bio/modules
    |    tag: 0.3.0
    |""".stripMargin,
  "yaml",
  "Definition of dependency with a fully defined repository"
)
@exampleWithDescription(
  """name: qc/multiqc
    |repository: "github://openpipelines-bio/modules:0.3.0"
    |""".stripMargin,
  "yaml",
  "Definition of a dependency with a repository using sugar syntax."
)
@exampleWithDescription(
  """name: qc/multiqc
    |  repository: "openpipelines-bio"
    |""".stripMargin,
  "yaml",
  "Definition of a dependency with a repository defined as 'openpipelines-bio' under `.functionality.repositories`."
)
case class Dependency(
  @description("The full name of the dependency component. This should include the namespace.")
  @example("name: \"my_namespace\"component", "yaml")
  name: String,
  
  @description(
    """Specifies the location where the dependency component can be found.
      |This must either be a full definition of the repository or the name of a repository refenced as it is defined under functionality.repositories.
      |Additionally, the full definition can be specified as a single string where all parameters such as repository type, url, branch or tag are specified.
      |""".stripMargin)
  repository: Either[String, Repository] = Right(LocalRepository()),

  // internal stuff
  @internalFunctionality
  @description("Location of the config of this dependency component.")
  foundConfigPath: Option[String] = None,

  @internalFunctionality
  @description("Meta info of this dependency component.")
  configInfo: Map[String, String] = Map.empty,

  @internalFunctionality
  @description("Location of the dependency component artifacts are written ready to be used.")
  writtenPath: Option[String] = None,
) {
  // Shorthand for getting the actual repository from 'repository'.
  // The lefthand string notation is converted to the righthand object during dependency resolution.
  // So after that step we must always use the righthand object. This makes that much easier.
  def workRepository: Option[Repository] = repository.toOption

  // Name in BashWrapper
  def VIASH_DEP: String = s"VIASH_DEP_${name.replace("/", "_").toUpperCase()}"
  // Name to be used in scripts
  def scriptName: String = name.replace("/", "_")
  // Part of the folder structure where dependencies should be written to, contains the repository & dependency name
  def subOutputPath = foundConfigPath.flatMap(fcp => getRelativePath(Paths.get(fcp).getParent()))
  // Method to get a relative sub path for this dependency or a local dependency of this dependency
  def getRelativePath(fullPath: Path): Option[String] = {
    if (isLocalDependency) {
      // Local dependency so it will only exist once the component is built.
      // TODO improve this, for one, the platform id should be dynamic
      Some(ViashNamespace.targetOutputPath("", "native", None, name))
    } else {
      // Previous existing dependency. Use the location of the '.build.yaml' to determine the relative location.
      val pathRoot = findBuildYamlFile(fullPath, Paths.get(workRepository.get.localPath)).map(_.getParent())
      val relativePath = pathRoot.map(pr => fullPath.toString().stripPrefix(pr.toString()))
      relativePath.flatMap(rp => workRepository.map(r => Paths.get(r.subOutputPath, rp).toString()))
    }
  }

  // Is this a dependency that will be built when `viash ns build` is run?
  def isLocalDependency: Boolean = workRepository.map{
    case r: LocalRepository => (r.path == None || r.path == Some(".")) && r.tag == None
    case _ => false
  }.getOrElse(false)

  // Traverse the folder upwards until a `.build.yaml` is found but do not traverse beyond `repoPath`.
  def findBuildYamlFile(path: Path, repoPath: Path): Option[Path] = {
    val child = path.resolve(".build.yaml")
    if (Files.isDirectory(path) && Files.exists(child)) {
      Some(child)
    } else {
      val parent = path.getParent()
      if ((parent == null) || (parent == repoPath)) {
        None
      } else {
        findBuildYamlFile(path.getParent(), repoPath)
      }
    }
  }
}
