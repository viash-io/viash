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
import io.viash.exceptions.MissingBuildYamlException

@description(
  """Specifies a Viash component (script or executable) that should be made available for the code defined in the functionality.
    |The dependency components are collected and copied to the output folder during the Viash build step.
    |""".stripMargin)
@exampleWithDescription(
  """name: qc/multiqc
    |repository: 
    |  type: github
    |  repo: openpipelines-bio/modules
    |  tag: 0.3.0
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
    |repository: "openpipelines-bio"
    |""".stripMargin,
  "yaml",
  "Definition of a dependency with a repository defined as 'openpipelines-bio' under `.functionality.repositories`."
)
@exampleWithDescription(
  """name: qc/multiqc
    |""".stripMargin,
  "yaml",
  "Definition of a local dependency. This dependency is present in the current code base and will be built when `viash ns build` is run."
)
case class Dependency(
  @description("The full name of the dependency component. This should include the namespace.")
  @example("""name: "my_namespace/component"""", "yaml")
  name: String,

  @description("An alternative name for the dependency component. This can include a namespace if so needed.")
  @example("alias: my_alias", "yaml")
  alias: Option[String] = None,
  
  @description(
    """Specifies the @[repository](repository) location where the dependency component can be found.
      |This must either be a full definition of the repository or the name of a repository referenced as it is defined under functionality.repositories.
      |Additionally, the full definition can be specified as a single string where all parameters such as repository type, url, branch or tag are specified.
      |Omitting the value sets the dependency as a local dependency, ie. the dependency is available in the same namespace as the component.
      |""".stripMargin)
  @default("Empty")
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
  if (alias.isDefined) {
    // check functionality name
    require(alias.get.matches("^[A-Za-z][A-Za-z0-9_]*$"), message = f"alias '${alias.get}' must begin with a letter and consist only of alphanumeric characters or underscores.")
  }

  // Shorthand for getting the actual repository from 'repository'.
  // The lefthand string notation is converted to the righthand object during dependency resolution.
  // So after that step we must always use the righthand object. This makes that much easier.
  def workRepository: Option[Repository] = repository.toOption

  // Name in BashWrapper
  def VIASH_DEP: String = s"VIASH_DEP_${alias.getOrElse(name).replace("/", "_").toUpperCase()}"
  // Name to be used in scripts
  def scriptName: String = alias.getOrElse(name).replace("/", "_")
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
      val relativePath = Dependency.getRelativePath(fullPath, Paths.get(workRepository.get.localPath))
      if (relativePath.isEmpty)
        throw new MissingBuildYamlException(fullPath, this)
      relativePath.flatMap(rp => workRepository.map(r => Paths.get(r.subOutputPath).resolve(rp).toString()))
    }
  }

  // Is this a dependency that will be built when `viash ns build` is run?
  def isLocalDependency: Boolean = workRepository.map{
    case r: LocalRepositoryTrait => (r.path == None || r.path == Some(".")) && r.tag == None
    case _ => false
  }.getOrElse(false)
}

object Dependency {

  /**
    * Relativize the writtenPath info from a dependency to a source and destination path so it can be copied.
    * 
    * This method is somewhat of a stopgap solution as this and the 'getRelativePath' functions in the case class and companion object should be revised.
    *
    * @param dependencyPath Path of the dependency as they are found in .config.vsh.yaml, relative to the original build location
    * @param output Output / target path as root for where the build artifacts should be located
    * @param repoPath Path of the repository where the dependency is found
    * @param mainDependency Top level dependency for which optionally dependencies of dependencies are being resolved. Used to relativize paths
    * @return Tuple with source and destination paths, relativized to current repository locations, ready to be copied
    */
  def getSourceAndDestinationFromWrittenPath(dependencyPath: String, output: Path, repoPath: Path, mainDependency: Dependency): (Path, Path) = {
    import scala.jdk.CollectionConverters._

    val sourcePath = repoPath.resolve(dependencyPath)
    // Split the path into chunks so we can manipulate them more easily
    val pathParts = Paths.get(dependencyPath).iterator().asScala.toList.map(p => p.toString())

    val destinationPath = if (pathParts.contains("dependencies")) {
      // Drop the other "target" folder from the found path. This can be multiple folders too
      val relativePath = Dependency.getRelativePath(sourcePath, repoPath)
        .fold(throw new MissingBuildYamlException(sourcePath, mainDependency))(identity)
      output.resolve(relativePath)
    } else {
      val subPath = mainDependency.getRelativePath(sourcePath)
        .fold(throw new MissingBuildYamlException(sourcePath, mainDependency))(identity)
      output.resolve("dependencies").resolve(subPath)
    }

    (sourcePath, destinationPath)
  }

  // From a built dependency's writtenPath, strip the target folder. Uses `.build.yaml` as reference.
  def getRelativePath(sourcePath: Path, repoPath: Path): Option[Path] = {
    val pathRoot = findBuildYamlFile(sourcePath, repoPath).map(_.getParent)
    pathRoot.map(pr => pr.relativize(sourcePath.toRealPath()))
  }

  // Traverse the folder upwards until a `.build.yaml` is found but do not traverse beyond `repoPath`.
  def findBuildYamlFile(pathPossiblySymlink: Path, repoPath: Path): Option[Path] = {
    val path = pathPossiblySymlink.toRealPath()
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
