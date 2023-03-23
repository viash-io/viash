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

object DependencyResolver {

  // Download the repo and return the repo with the local dir where it is stored filled in
  def cacheRepo(repo: Repository): Repository = 
    repo match {
      case r: GithubRepository => {
        val r2 = r.checkoutSparse()
        r2.checkout()
      }
      case r => r
    }

  // Modify the config so all of the dependencies are available locally
  def modifyConfig(config: Config, ttl: Integer = 10): Config = {

    // Check recursion level
    require(ttl >= 0, "Not all dependencies evaluated as the recursion is too deep")

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
        val localRepoPath = cacheRepo(repo)
        d.copy(repository = Right(localRepoPath))
      }
      )(config2)

    // find the referenced config in the locally cached repository
    val config4 = composedDependenciesLens.modify(_
      .map{dep =>
        val repo = dep.repository.toOption.get
        // search for configs in the repository and filter by namespace/name
        val configs = Config.readConfigs(
          source = repo.localPath,
          query = Some(s"^${dep.name}$$"),
          queryNamespace = None,
          queryName = None,
          configMods = Nil,
          addOptMainScript = false
        )
        val dependencyConfig = configs.flatMap(_.swap.toOption).headOption
        val configPath = dependencyConfig.flatMap(_.info).map(_.config)
        dep.copy(foundConfigPath = configPath, workConfig = dependencyConfig)
      }
      )(config3)
    
    // recurse through our dependencies to solve their dependencies
    composedDependenciesLens.modify(_
      .map{dep =>
        dep.workConfig match {
          case Some(depConf) =>
            dep.copy(workConfig = Some(modifyConfig(depConf, ttl - 1)))
          case _ =>
            dep
        }
      }
      )(config4)
  }

  def copyDependencies(config: Config, output: String): Unit = {
    for (dep <- composedDependenciesLens.get(config)) {
      // copy the dependency to the output folder
      val dependencyPath = Paths.get(output, dep.name)
      if (dependencyPath.toFile().exists())
        IO.deleteRecursively(dependencyPath)
      Files.createDirectories(dependencyPath)

      copyFolder(Paths.get(dep.repository.toOption.get.localPath), dependencyPath)

      // more recursion for the dependencies of dependencies
      copyDependencies(dep.workConfig.get, output)
    }
  }

  def copyFolder(src: Path, dest: Path): Unit = {
    val stream = Files.walk(src)

    try {
      stream.forEachOrdered((sourcePath: Path) => {

        try {
          val newPath = dest.resolve(src.relativize(sourcePath))
          if (sourcePath.toFile.isFile) {
            Files.copy(sourcePath, newPath)
          } else if (sourcePath.toFile.isDirectory) {
            newPath.toFile.mkdir()
          }

        } catch {
          case e: IOException =>
            throw new UncheckedIOException(e)
        }

      })

    } finally {
      stream.close()
    }
  }

}
