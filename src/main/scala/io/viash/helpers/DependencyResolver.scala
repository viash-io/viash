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

package io.viash.helpers

import java.nio.file.{ Path, Paths }
import io.viash.functionality.dependencies.Repository
import io.viash.config.Config
import io.viash.lenses.ConfigLenses._
import io.viash.lenses.FunctionalityLenses._
import io.viash.lenses.RepositoryLens._
import io.viash.functionality.dependencies.GithubRepository
import java.nio.file.Files
import java.io.IOException
import java.io.UncheckedIOException
import io.viash.helpers.IO
import io.circe.yaml.parser
import io.circe.Json
import io.viash.config.Config._
import io.viash.ViashNamespace
import io.viash.functionality.dependencies.Dependency
import io.viash.functionality.resources.NextflowScript
import io.viash.exceptions.MissingDependencyException

object DependencyResolver {

  // Modify the config so all of the dependencies are available locally
  def modifyConfig(config: Config, platformId: Option[String], projectRootDir: Option[Path], namespaceConfigs: List[Config] = Nil): Config = {

    // Check all fun.repositories have valid names
    val repositories = config.functionality.repositories
    require(repositories.isEmpty || repositories.groupBy(r => r.name).map{ case(k, l) => l.length }.max == 1, "Repository names should be unique")
    require(repositories.filter(r => r.name.isEmpty()).length == 0, "Repository names can't be empty")


    // Convert all fun.dependency.repository with sugar syntax to full repositories
    val config1 = composedDependenciesLens.modify(_.map(d =>
      d.repository match {
        case Left(Repository(repo)) => d.copy(repository = Right(repo))
        case _ => d
      }
    ))(config)

    // Check all remaining fun.dependency.repository names (Left) refering to fun.repositories can be matched
    val dependencyRepoNames = composedDependenciesLens.get(config1).flatMap(_.repository.left.toOption)
    val definedRepoNames = composedRepositoriesLens.get(config1).map(_.name)
    dependencyRepoNames.foreach(name =>
      require(definedRepoNames.contains(name), s"Named dependency repositories should exist in the list of repositories. '$name' not found.")
    )

    // Match repositories defined in dependencies by name to the list of repositories, fill in repository in dependency
    val config2 = composedDependenciesLens.modify(_
      .map(d => 
        d.repository match {
          case Left(name) => d.copy(repository = Right(composedRepositoriesLens.get(config1).find(r => r.name == name).get))
          case _ => d
        }
      )
      )(config1)

    // get caches and store in repository classes
    val config3 = composedDependenciesLens.modify(_
      .map{d =>
        val repo = d.repository.toOption.get
        val configDir = Paths.get(config2.info.get.config).getParent()
        val localRepoPath = Repository.cache(repo, configDir, projectRootDir)
        d.copy(repository = Right(localRepoPath))
      }
      )(config2)

    // find the referenced config in the locally cached repository
    val config4 = composedDependenciesLens.modify(_
      .map{dep =>
        val repo = dep.workRepository.get

        val config =
          if (dep.isLocalDependency) {
            findLocalConfig(repo.localPath.toString(), namespaceConfigs, dep.name, platformId)
          } else {
            findRemoteConfig(repo.localPath.toString(), dep.name, platformId)
          }

        dep.copy(
          foundConfigPath = config.map(_._1),
          configInfo = config.map(_._2).getOrElse(Map.empty)
        )
      }
      )(config3)

    // Check if all dependencies were found
    val missingDependencies = composedDependenciesLens.get(config4).filter(d => d.foundConfigPath.isEmpty || d.configInfo.isEmpty)
    if (missingDependencies.nonEmpty) {
      throw new MissingDependencyException(missingDependencies)
    }

    config4
  }

  def copyDependencies(config: Config, output: String, platformId: String): Config = {
    composedDependenciesLens.modify(_.map(dep => {

      if (dep.isLocalDependency) {
        // Dependency solving will be done by building the component and dependencies of that component will be handled there.
        // However, we have to fill in writtenPath. This will be needed when this built component is used as a dependency and we have to resolve dependencies of dependencies.
        val writtenPath = ViashNamespace.targetOutputPath(output, platformId, None, dep.name)
        dep.copy(writtenPath = Some(writtenPath))
      } else {
        // copy the dependency to the output folder
        val dependencyOutputPath = Paths.get(output, "dependencies", dep.subOutputPath.get)
        if (dependencyOutputPath.toFile().exists())
          IO.deleteRecursively(dependencyOutputPath)
        Files.createDirectories(dependencyOutputPath)          

        val dependencyRepoPath = Paths.get(dep.foundConfigPath.get).getParent()
        // Copy dependencies
        IO.copyFolder(dependencyRepoPath, dependencyOutputPath)
        // Copy dependencies of dependencies, all the way down
        // Parse new location of the copied dependency. That way that path can be used to determine the new location of namespace dependencies
        recurseBuiltDependencies(Paths.get(output), Paths.get(dep.workRepository.get.localPath), dependencyOutputPath.toString(), dep)
        // Store location of the copied files
        dep.copy(writtenPath = Some(dependencyOutputPath.toString()))
      }

    }))(config)
  }

  // Find configs from the local repository. These still need to be built so we have to deduce the information we want.
  def findLocalConfig(targetDir: String, namespaceConfigs: List[Config], name: String, platformId: Option[String]): Option[(String, Map[String, String])] = {

    val config = namespaceConfigs.filter{ c => 
        val fullName = c.functionality.namespace.fold("")(n => n + "/") + c.functionality.name
        fullName == name
      }
      .headOption

    config.map{ c =>
      val path = c.info.get.config
      // fill in the location of the executable where it will be located
      // TODO: it would be better if this was already filled in somewhere else
      val executable = platformId.map{ pid =>
          val executableName = pid match {
            case "nextflow" => "main.nf"
            case _ => c.functionality.name
          }
          Paths.get(ViashNamespace.targetOutputPath("", pid, c.functionality.namespace, c.functionality.name), executableName).toString()
      }
      val info = c.info.get.copy(
        executable = executable
      )
      // Convert case class to map, do some extra conversions of Options while we're at it
      val map = (info.productElementNames zip info.productIterator).map{
          case (k, s: String) => (k, s)
          case (k, Some(s: String)) => (k, s)
          case (k, None) => (k, "")
        }.toMap
      // Add the functionality name and namespace to it
      val map2 = Map(
        ("functionalityName" -> c.functionality.name),
        ("functionalityNamespace" -> c.functionality.namespace.getOrElse(""))
      )
      (path, map ++ map2)
    }
  }

  // Read built config artifact in a minimalistic way. This prevents minor schema changes breaking things.
  def findRemoteConfig(path: String, name: String, platformId: Option[String]): Option[(String, Map[String, String])] = {
    if (!Files.exists(Paths.get(path)))
      return None

    val scriptFiles = IO.find(Paths.get(path), (path, attrs) => {
      path.toString.contains(".vsh.") &&
        path.toFile.getName.startsWith(".") &&
        attrs.isRegularFile
    })

    val scriptInfo = scriptFiles
      .map(_.toString())
      .map(scriptPath => (scriptPath, getSparseConfigInfo(scriptPath)))

    scriptInfo
      .filter{
        case(scriptPath, info) => 
          (info("functionalityNamespace"), info("functionalityName")) match {
            case (ns, n) if !ns.isEmpty() => s"$ns/$n" == name
            case (_, n) => n == name
          }
      }
      .filter{
        case(scriptPath, info) =>
          platformId match {
            case None => true
            case Some(p) => info("platform") == p
          }
      }
      .headOption
  }

  // Read a config file from a built target. Get meta info, functionality name & namespace
  def getSparseConfigInfo(configPath: String): Map[String, String] = {
    try {
      // No support for project configs, config mods, ...
      // The yaml file in the target folder should be final
      // We're also assuming that the file will be proper yaml and an actual viash config file
      val yamlText = IO.read(IO.uri(configPath))
      val json = parser.parse(yamlText).toOption.get

      def getFunctionalityName(json: Json): Option[String] = {
        json.hcursor.downField("functionality").downField("name").as[String].toOption
      }
      def getFunctionalityNamespace(json: Json): Option[String] = {
        json.hcursor.downField("functionality").downField("namespace").as[String].toOption
      }
      def getInfo(json: Json): Option[Map[String, String]] = {
        json.hcursor.downField("info").as[Map[String, String]].toOption
      }

      val functionalityName = getFunctionalityName(json)
      val functonalityNamespace = getFunctionalityNamespace(json)
      val info = getInfo(json).getOrElse(Map.empty) +
        ("functionalityName" -> functionalityName.getOrElse("")) +
        ("functionalityNamespace" -> functonalityNamespace.getOrElse(""))

      info
    }
    catch {
      case _: Throwable => Map.empty
    }
  }

  // Read a config file from a built target. Extract dependencies 'writtenPath'.
  def getSparseDependencyInfo(configPath: String): List[String] = {
    try {
      val yamlText = IO.read(IO.uri(configPath))
      val json = parser.parse(yamlText).toOption.get

      val dependencies = json.hcursor.downField("functionality").downField("dependencies").focus.flatMap(_.asArray).get
      dependencies.flatMap(_.hcursor.downField("writtenPath").as[String].toOption).toList
    } catch {
      case _: Throwable => Nil
    }
  }

  // Add the '.build.yaml' file as relative root marker. File content to be decided.
  def createBuildYaml(output: String): Unit = {
    Files.createDirectories(Paths.get(output))
    val filePath = Paths.get(output, ".build.yaml")
    if (Files.notExists(filePath))
      Files.createFile(filePath)
  }

  // Handle dependencies of dependencies. For a given already built component, get their dependencies, copy them to our new target folder and recurse into these.
  def recurseBuiltDependencies(output: Path, repoPath: Path, builtDependencyPath: String, dependency: Dependency, depth: Int = 0): Unit = {

    // Limit recursion depth to prevent infinite loops in e.g. cross dependencies (TODO)
    if (depth > 10)
      throw new RuntimeException("Copying dependencies traces too deep. Possibibly caused by a cross dependency.")

    // this returns paths relative to `repoPath` of dependencies to be copied to `output`
    val dependencyPaths = getSparseDependencyInfo(builtDependencyPath + "/.config.vsh.yaml")

    for (dp <- dependencyPaths) {
      // Get the source & destination path for the dependency, functionality depends whether it was a previous dependency or not.
      // Paths are relativized depending the original dependency.
      val (sourcePath, destPath) = Dependency.getSourceAndDestinationFromWrittenPath(dp, output, repoPath, dependency)

      // Make sure the destination is clean so first remove the destination folder if it exists
      if (destPath.toFile().exists())
        IO.deleteRecursively(destPath)
      // Then re-create it
      Files.createDirectories(destPath)
      // Then copy files to it
      IO.copyFolder(sourcePath, destPath)

      // Check for more dependencies
      recurseBuiltDependencies(output, repoPath, destPath.toString(), dependency, depth + 1)
    }
  }

  // Get the platform to be used for dependencies. If the main script is a Nextflow script, use 'nextflow', otherwise use 'native'.
  // Exception is when there is no platform set for the config, then we must return 'None' too.
  def getDependencyPlatformId(config: Config, platform: Option[String]): Option[String] = {
    (config.functionality.mainScript, platform) match {
      case (_, None) => None
      case (None, _) => None
      case (Some(n: NextflowScript), _) => Some("nextflow")
      case _ => Some("native")
    }
  }
}
