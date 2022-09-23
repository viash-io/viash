package io.viash.helpers

import java.nio.file.{ Path, Paths }
import io.viash.functionality.dependencies.Repository
import io.viash.config.Config

object DependencyResolver {

  // copied from functionality
  // perform some dependency operations
  {

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

    // Check all fun.repositories have valid names
    val repositories = config.functionality.repositories
    require(repositories.groupBy(r => r.name).map{ case(k, l) => l.length }.max == 1, "Repository names should be unique")
    require(repositories.filter(r => r.name.isEmpty()).length == 0, "Repository names can't be empty")


    // Convert all fun.dependency.repository with sugar syntax to full repositories
    // val repoRegex = raw"(\w+)://([A-Za-z0-9/_\-\.]+)@([A-Za-z0-9]+)".r  // TODO improve regex
    val repoRegex = raw"(\w+://[A-Za-z0-9/_\-\.]+@[A-Za-z0-9]*)".r
    config.copy(functionality = config.functionality.copy(
      dependencies = config.functionality.dependencies.map(d => 
        d.repository match {
          case Left(repoRegex(s)) => d.copy(repository = Right(Repository.fromSugarSyntax(s)))
          case _ => d
        }

    )))

    // Check all remaining fun.dependency.repository names (Left) refering to fun.repositories can be matched

    // Move repositories defined in dependencies to the list of repositories, link by string name


    // OLD code, works in the wrong direction
    // map the repository string to the repository class => Either.Right -> Either.Left
    // val config1 = config.copy(
    //   functionality = config.functionality.copy(
    //     dependencies = config.functionality.dependencies.map(d => 
    //       d.repository match {
    //         case Left(r) => d
    //         case Right(s) => d.copy(repository = Left(repositories.find(_.name == s).get)) // TODO if name not found, this blows up
    //       }
    //   )))

    // 

    // val actualRepo = 
    //   rawRepo match {
    //     case Left(repoRegex(protocol, repo, tag)) => 
    //       Repo(protocol, repo, tag)
    //     case Left(id) if id.matches("\\w+") =>
    //       throw new NotImplementedError("define lens")
    //     case Left(s) => throw new RuntimeException("unrecognised repo format " + s)
    //     case Right(r) => r
    //   }

    // get caches and store in repository classes
    config.copy(
      // provide local cache for all repositories
      functionality = config.functionality.copy(
        repositories = config.functionality.repositories.map{ repo =>
          val localRepoPath = cacheRepo(repo)
          repo.copyRepo(localPath = localRepoPath.toString)
        })
      )
  }
}
