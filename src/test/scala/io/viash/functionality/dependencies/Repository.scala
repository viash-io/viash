package io.viash.functionality.dependencies

import org.scalatest.funsuite.AnyFunSuite

class RepositoryTest extends AnyFunSuite {
  test("Repository.unapply: handles git+https syntax") {
    val repo = Repository.unapply("git+https://github.com/viash-io/viash@v1.0.0")
    assert(repo.isDefined)
    assert(repo.get.isInstanceOf[GitRepository])
    val gitRepo = repo.get.asInstanceOf[GitRepository]
    assert(gitRepo.uri == "https://github.com/viash-io/viash")
    assert(gitRepo.tag == Some("v1.0.0"))
  }

  test("Repository.unapply: handles github syntax") {
    val repo = Repository.unapply("github://viash-io/viash@v2.0.0")
    assert(repo.isDefined)
    assert(repo.get.isInstanceOf[GithubRepository])
    val githubRepo = repo.get.asInstanceOf[GithubRepository]
    assert(githubRepo.repo == "viash-io/viash")
    assert(githubRepo.tag == Some("v2.0.0"))
  }

  test("Repository.unapply: handles viashhub syntax") {
    val repo = Repository.unapply("vsh://viash-io/viash@v2.0.0")
    assert(repo.isDefined)
    assert(repo.get.isInstanceOf[ViashhubRepository])
    val viashhubRepo = repo.get.asInstanceOf[ViashhubRepository]
    assert(viashhubRepo.repo == "viash-io/viash")
    assert(viashhubRepo.tag == Some("v2.0.0"))
  }

  test("Repository.unapply: handles local syntax") {
    val repo = Repository.unapply("local://path/to/repo")
    assert(repo.isDefined)
    assert(repo.get.isInstanceOf[LocalRepository])
  }

  test("Repository.unapply: handles local dependency syntax") {
    val repo = Repository.unapply("local")
    assert(repo.isDefined)
    assert(repo.get.isInstanceOf[LocalRepository])
  }

  test("Repository.unapply: returns None for unrecognized syntax") {
    val repo = Repository.unapply("unknown://foo.bar")
    assert(repo.isEmpty)
  }
}
