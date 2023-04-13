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
import io.viash.platforms.Platform
import io.viash.helpers.IO
import io.circe.yaml.parser
import io.circe.Json
import io.viash.config.Config._
import io.viash.ViashNamespace


object DependencyResolver {

  // Modify the config so all of the dependencies are available locally
  def modifyConfig(config: Config, platform: Option[Platform], namespaceConfigs: List[Config] = Nil): Config = {

    // Check all fun.repositories have valid names
    val repositories = config.functionality.repositories
    require(repositories.isEmpty || repositories.groupBy(r => r.name).map{ case(k, l) => l.length }.max == 1, "Repository names should be unique")
    require(repositories.filter(r => r.name.isEmpty()).length == 0, "Repository names can't be empty")


    // Convert all fun.dependency.repository with sugar syntax to full repositories
    // val repoRegex = raw"(\w+)://([A-Za-z0-9/_\-\.]+)@([A-Za-z0-9]+)".r  // TODO improve regex
    val repoRegex = raw"(\w+://[A-Za-z0-9/_\-\.]+@[A-Za-z0-9]*)".r
    val config1 = composedDependenciesLens.modify(_.map(d =>
        d.repository match {
          case Left(repoRegex(s)) => d.copy(repository = Right(Repository.fromSugarSyntax(s)))
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
        val localRepoPath = Repository.cache(repo)
        d.copy(repository = Right(localRepoPath))
      }
      )(config2)

    // find the referenced config in the locally cached repository
    val config4 = composedDependenciesLens.modify(_
      .map{dep =>
        val repo = dep.workRepository.get
        val targetPath = Paths.get(repo.localPath, repo.path.getOrElse("")/*.stripPrefix("/")*/)
          
        val config = dep.isLocalDependency match {
          case true => findLocalConfig(targetPath.toString(), namespaceConfigs, dep.name, platform)
          case false => findRemoteConfig(targetPath.toString(), dep.name, platform)
        }

        dep.copy(foundConfigPath = config.map(_._1), configInfo = config.map(_._2).getOrElse(Map.empty))
      }
      )(config3)

    config4    
  }

  def copyDependencies(config: Config, output: String, platform: Option[Platform], buildingNamespace: Boolean = false): Config = {
    composedDependenciesLens.modify(_.map(dep => {

      if (dep.isLocalDependency && buildingNamespace) {
        dep
      } else {
        // copy the dependency to the output folder
        val dependencyOutputPath = Paths.get(output, "dependencies", dep.subOutputPath.get)
        if (dependencyOutputPath.toFile().exists())
          IO.deleteRecursively(dependencyOutputPath)
        Files.createDirectories(dependencyOutputPath)
        
        val platformId = platform.map(_.id).getOrElse("")
        if (dep.foundConfigPath.isDefined) {
          val dependencyRepoPath = Paths.get(dep.foundConfigPath.get).getParent()
          IO.copyFolder(dependencyRepoPath, dependencyOutputPath)
          // TODO copy the right subselection of 'dependencies' folders from the dependency repository into the main 'dependencies' folder
        }
        else {
          Console.err.println(s"Could not find dependency artifacts for ${dep.name}. Skipping copying dependency artifacts.")
        }

        // Store location of the copied files
        dep.copy(writtenPath = Some(dependencyOutputPath.toString()))
      }
    }))(config)
  }

  // Find configs from the local repository. These still need to be built so we have to deduce the information we want.
  def findLocalConfig(targetDir: String, namespaceConfigs: List[Config], name: String, platform: Option[Platform]): Option[(String, Map[String, String])] = {

    val config = namespaceConfigs.filter{ c => 
        val fullName = c.functionality.namespace.fold("")(n => n + "/") + c.functionality.name
        fullName == name
      }
      .headOption

    config.map{ c =>
      val path = c.info.get.config
      // fill in the location of the executable where it will be located
      val executable = ViashNamespace.targetOutputPath("", platform.get.id, c.functionality.namespace, c.functionality.name)
      val info = c.info.get.copy(executable = Some(executable))
      val map = (info.productElementNames zip info.productIterator).map{
          case (k, s: String) => (k, s)
          case (k, Some(s: String)) => (k, s)
          case (k, None) => (k, "")
        }.toMap
      (path, map)
    }
  }

  // Read built config artifact in a minimalistic way. This prevents minor schema changes breaking things.
  def findRemoteConfig(path: String, name: String, platform: Option[Platform]): Option[(String, Map[String, String])] = {
    if (!Files.exists(Paths.get(path)))
      return None

    val scriptFiles = IO.find(Paths.get(path), (path, attrs) => {
      path.toString.contains(".vsh.") &&
        path.toFile.getName.startsWith(".") &&
        attrs.isRegularFile
    })

    val scriptInfo = scriptFiles.map(scriptPath => {
      val info = getSparseConfigInfo(scriptPath.toString())
      (scriptPath, info)
    })

    val script = scriptInfo
      .filter{
        case(scriptPath, info) => 
          (info("functionalityNamespace"), info("functionalityName")) match {
            case (ns, n) if !ns.isEmpty() => s"$ns/$n" == name
            case (_, n) => n == name
          }
      }
      .filter{
        case(scriptPath, info) =>
          platform match {
            case None => true
            case Some(p) => info("platform") == p.id
          }
      }
      .headOption

    script.map(t => (t._1.toString(), t._2))
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

  def createBuildYaml(output: String): Unit = {
    Files.createDirectories(Paths.get(output))
    val filePath = Paths.get(output, ".build.yaml")
    if (Files.notExists(filePath))
      Files.createFile(filePath)
  }
}
