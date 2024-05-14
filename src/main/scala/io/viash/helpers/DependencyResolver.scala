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
import io.viash.config.Config
import io.viash.lenses.ConfigLenses._
import io.viash.lenses.RepositoryLens._
import io.viash.config.dependencies.{Dependency, Repository, RepositoryWithoutName, GithubRepository}
import java.nio.file.Files
import java.io.IOException
import java.io.UncheckedIOException
import io.viash.helpers.{IO, Logging}
import io.circe.Json
import io.viash.config.Config._
import io.viash.ViashNamespace
import io.viash.config.resources.NextflowScript
import io.viash.exceptions.MissingDependencyException
import io.viash.helpers.circe.Convert

object DependencyResolver extends Logging {

  /**
    * Modify the config so all of the dependencies are available locally 
    *
    * @param config Component configuration
    * @param runnerId Used to create the path where to store retrieved dependencies
    * @param packageRootDir Location of the Package Config, used for relative referencing
    * @param namespaceConfigs Needed for local dependencies
    * @return A config with dependency information added
    */
  def modifyConfig(config: Config, runnerId: Option[String], packageRootDir: Option[Path], namespaceConfigs: List[Config] = Nil): Config = {

    // Check all fun.repositories have valid names
    val repositories = config.repositories
    require(repositories.isEmpty || repositories.groupBy(r => r.name).map{ case(k, l) => l.length }.max == 1, "Repository names should be unique")
    require(repositories.filter(r => r.name.isEmpty()).length == 0, "Repository names can't be empty")


    // Convert all fun.dependency.repository with sugar syntax to full repositories
    val config1 = dependenciesLens.modify(_.map(d =>
      d.repository match {
        case Left(RepositoryWithoutName(repo)) => d.copy(repository = Right(repo))
        case _ => d
      }
    ))(config)

    // Check all remaining fun.dependency.repository names (Left) refering to fun.repositories can be matched
    val dependencyRepoNames = dependenciesLens.get(config1).flatMap(_.repository.left.toOption)
    val definedRepoNames = repositoriesLens.get(config1).map(_.name)
    dependencyRepoNames.foreach(name =>
      require(definedRepoNames.contains(name), s"Named dependency repositories should exist in the list of repositories. '$name' not found.")
    )

    // Match repositories defined in dependencies by name to the list of repositories, fill in repository in dependency
    val config2 = dependenciesLens.modify(_
      .map(d => 
        d.repository match {
          case Left(name) => d.copy(repository = Right(repositoriesLens.get(config1).find(r => r.name == name).get.withoutName))
          case _ => d
        }
      )
      )(config1)

    // get caches and store in repository classes
    val config3 = dependenciesLens.modify(_
      .map{d =>
        val repo = d.repository.toOption.get
        val configDir = Paths.get(config2.build_info.get.config).getParent()
        val localRepoPath = RepositoryWithoutName.get(repo, configDir, packageRootDir)
        d.copy(repository = Right(localRepoPath))
      }
      )(config2)

    // find the referenced config in the locally cached repository
    val config4 = dependenciesLens.modify(_
      .map{dep =>
        val repo = dep.workRepository.get

        val config =
          if (dep.isLocalDependency) {
            findLocalConfig(repo.localPath.toString(), namespaceConfigs, dep.name, runnerId)
          } else {
            findRemoteConfig(repo.localPath.toString(), dep.name, runnerId)
          }

        dep.copy(
          foundConfigPath = config.map(_._1),
          configInfo = config.map(_._2).getOrElse(Map.empty)
        )
      }
      )(config3)

    // Check if all dependencies were found
    val missingDependencies = dependenciesLens.get(config4).filter(d => d.foundConfigPath.isEmpty || d.configInfo.isEmpty)
    if (missingDependencies.nonEmpty) {
      throw new MissingDependencyException(missingDependencies)
    }

    config4
  }

  def copyDependencies(config: Config, output: String, runnerId: String): Config = {
    dependenciesLens.modify(_.map(dep => {

      if (dep.isLocalDependency) {
        // Dependency solving will be done by building the component and dependencies of that component will be handled there.
        // However, we have to fill in writtenPath. This will be needed when this built component is used as a dependency and we have to resolve dependencies of dependencies.
        val writtenPath = ViashNamespace.targetOutputPath(output, runnerId, None, dep.name)
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
  def findLocalConfig(targetDir: String, namespaceConfigs: List[Config], name: String, runnerId: Option[String]): Option[(String, Map[String, String])] = {

    val config = namespaceConfigs.filter{ c => 
        val fullName = c.namespace.fold("")(n => n + "/") + c.name
        fullName == name
      }
      .headOption

    config.map{ c =>
      val path = c.build_info.get.config
      // fill in the location of the executable where it will be located
      // TODO: it would be better if this was already filled in somewhere else
      val executable = runnerId.map{ rid =>
          val executableName = rid match {
            case "nextflow" => "main.nf"
            case _ => c.name
          }
          Paths.get(ViashNamespace.targetOutputPath("", rid, c), executableName).toString()
      }
      val info = c.build_info.get.copy(
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
        ("name" -> c.name),
        ("namespace" -> c.namespace.getOrElse(""))
      )
      (path, map ++ map2)
    }
  }

  // Read built config artifact in a minimalistic way. This prevents minor schema changes breaking things.
  def findRemoteConfig(path: String, name: String, runnerId: Option[String]): Option[(String, Map[String, String])] = {
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
          (info("namespace"), info("name")) match {
            case (ns, n) if !ns.isEmpty() => s"$ns/$n" == name
            case (_, n) => n == name
          }
      }
      .filter{
        case(scriptPath, info) =>
          (runnerId, info.get("runner"), info.get("platform")) match { // also try matching on platform as fallback, fetch it already
            case (None, _, _) => true // if we don't filter for the incoming runnerId, we want all the output runners
            case (Some(id), Some(runner), _) => runner == id // default behaviour, filter for the incoming runnerId
            case (Some("executable"), _, Some("native")) => true // legacy code for platform, match executable runner to native platform
            case (Some(id), _, Some(plat)) => plat == id // legacy code for platform, filter for the incoming runnerId matching platform id
            case _ => false
          }
      }
      .headOption
  }

  // Read a config file from a built target. Get meta info, functionality name & namespace
  def getSparseConfigInfo(configPath: String): Map[String, String] = {
    try {
      // No support for package configs, config mods, ...
      // The yaml file in the target folder should be final
      // We're also assuming that the file will be proper yaml and an actual viash config file
      val yamlText = IO.read(IO.uri(configPath))
      val json = Convert.textToJson(yamlText, configPath)
      val legacyMode = json.hcursor.downField("functionality").succeeded

      def getName(json: Json): Option[String] = {
        if (legacyMode)
          json.hcursor.downField("functionality").downField("name").as[String].toOption
        else
          json.hcursor.downField("name").as[String].toOption
      }
      def getNamespace(json: Json): Option[String] = {
        if (legacyMode)
          json.hcursor.downField("functionality").downField("namespace").as[String].toOption
        else
          json.hcursor.downField("namespace").as[String].toOption
      }
      def getInfo(json: Json): Option[Map[String, String]] = {
        val info = 
          if (legacyMode)
            json.hcursor.downField("info")
          else
            json.hcursor.downField("build_info")
        info.keys.map(_.map(k => (k, info.downField(k).as[String].toOption.getOrElse("Not a string"))).toMap)
      }

      val info = getInfo(json).getOrElse(Map.empty) +
        ("name" -> getName(json).getOrElse("")) +
        ("namespace" -> getNamespace(json).getOrElse(""))

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
      val json = Convert.textToJson(yamlText, configPath)
      val legacyMode = json.hcursor.downField("functionality").succeeded

      val dependencies =
        if (legacyMode) {
          val jsonVec = json.hcursor.downField("functionality").downField("dependencies").focus.flatMap(_.asArray).get
          jsonVec.flatMap(_.hcursor.downField("writtenPath").as[String].toOption).toList
        }
        else {
          val jsonVec = json.hcursor.downField("build_info").downField("dependencies").focus.flatMap(_.asArray).get
          jsonVec.flatMap(_.hcursor.as[String].toOption).toList
        }
      dependencies
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

  // Get the runner to be used for dependencies. If the main script is a Nextflow script, use 'nextflow', otherwise use 'native'.
  // Exception is when there is no runner set for the config, then we must return 'None' too.
  def getDependencyRunnerId(config: Config, runner: Option[String]): Option[String] = {
    (config.mainScript, runner) match {
      case (_, None) => None
      case (None, _) => None
      case (Some(n: NextflowScript), _) => Some("nextflow")
      case _ => Some("executable")
    }
  }
}
