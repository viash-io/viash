package io.viash.config.dependencies

import org.scalatest.funsuite.AnyFunSuite
import io.viash.config.dependencies.{GithubRepository, GitRepository, LocalRepository, ViashhubRepository}
import io.viash.helpers.IO
import io.viash.helpers.SysEnv

class RepositoryTest extends AnyFunSuite {
  test("Repository.unapply: handles git+https syntax") {
    val repo = RepositoryWithoutName.unapply("git+https://github.com/viash-io/viash@v1.0.0")
    assert(repo.isDefined)
    assert(repo.get.isInstanceOf[GitRepository])
    val gitRepo = repo.get.asInstanceOf[GitRepository]
    assert(gitRepo.uri == "https://github.com/viash-io/viash")
    assert(gitRepo.tag == Some("v1.0.0"))
  }

  test("Repository.unapply: handles github syntax") {
    val repo = RepositoryWithoutName.unapply("github://viash-io/viash@v2.0.0")
    assert(repo.isDefined)
    assert(repo.get.isInstanceOf[GithubRepository])
    val githubRepo = repo.get.asInstanceOf[GithubRepository]
    assert(githubRepo.repo == "viash-io/viash")
    assert(githubRepo.tag == Some("v2.0.0"))
  }

  test("Repository.unapply: handles viashhub syntax") {
    val repo = RepositoryWithoutName.unapply("vsh://viash-io/viash@v2.0.0")
    assert(repo.isDefined)
    assert(repo.get.isInstanceOf[ViashhubRepository])
    val viashhubRepo = repo.get.asInstanceOf[ViashhubRepository]
    assert(viashhubRepo.repo == "viash-io/viash")
    assert(viashhubRepo.tag == Some("v2.0.0"))
  }

  test("Repository.unapply: handles local syntax") {
    val repo = RepositoryWithoutName.unapply("local://path/to/repo")
    assert(repo.isDefined)
    assert(repo.get.isInstanceOf[LocalRepository])
  }

  test("Repository.unapply: handles local dependency syntax") {
    val repo = RepositoryWithoutName.unapply("local")
    assert(repo.isDefined)
    assert(repo.get.isInstanceOf[LocalRepository])
  }

  test("Repository.unapply: returns None for unrecognized syntax") {
    val repo = RepositoryWithoutName.unapply("unknown://foo.bar")
    assert(repo.isEmpty)
  }

  test("Repository caching") {
    val repo = RepositoryWithoutName.unapply("vsh://hendrik/dependency_test2")

    repo match {
      case Some(r: AbstractGitRepository) => {
        // Remove the cache if it exists
        val cachePath = r.fullCachePath
        assert(cachePath.isDefined)
        if (cachePath.get.toFile.exists())
          IO.deleteRecursively(cachePath.get)
        assert(r.findInCache().isEmpty)
        
        val newRepo = r.getSparseRepoInTemp()
        val cachedRepo = r.findInCache()
        assert(cachedRepo.isDefined, "Cache should be present")
        assert(cachedRepo.get.checkCacheStillValid(), "Cache should be valid")
      }
      case _ => assert(false, "Expected AbstractGitRepository")
    }

  }
      
}
