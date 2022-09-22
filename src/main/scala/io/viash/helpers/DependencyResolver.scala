package io.viash.helpers

import java.nio.file.{ Path, Paths }
import io.viash.functionality.dependencies.Repository
import io.viash.config.Config

object DependencyResolver {

  // copied from functionality
  // perform some dependency operations
  {
    // map the repository to the repository class
    // dependencies.foreach(d => {
    //   d.linkedRepository = d.repository.flatMap(repoName => repositories.find(_.name == repoName))
    //   require(d.repository.isDefined == d.linkedRepository.isDefined, message = s"Could not find repository ${d.repository.get}")
    // })

    // val groupedDependencies = Dependency.groupByRepository(dependencies)
    
    // // TODO get remote repositories, pass to dependency.prepare?
    // // groupedDependencies.foreach(r => r.fetch)

    // dependencies.foreach(d => d.prepare())


    // println(s"grouped dependencies: $groupedDependencies")
  }




  // Download the repo and return the path to the local dir where it is stored
  def cacheRepo(repo: Repository): Path = { Paths.get("") }

  // Modify the config so all of the dependencies are available locally
  def modifyConfig(config: Config): Config = {
    config.copy(
      functionality = config.functionality.copy(
        repositories = config.functionality.repositories.map{ repo =>
          val localRepoPath = cacheRepo(repo)
          repo.copyRepo(localPath = localRepoPath.toString)
        })
      )
  }
}
