package io.viash.config.dependencies

import org.scalatest.funsuite.AnyFunSuite
import io.viash.config.dependencies.{GithubPackage, GitPackage, LocalPackage, ViashhubPackage}
import io.viash.helpers.IO
import io.viash.helpers.SysEnv

class PackageTest extends AnyFunSuite {
  test("Package.unapply: handles git+https syntax") {
    val pkg = Package.unapply("git+https://github.com/viash-io/viash@v1.0.0")
    assert(pkg.isDefined)
    assert(pkg.get.isInstanceOf[GitPackage])
    val gitPkg = pkg.get.asInstanceOf[GitPackage]
    assert(gitPkg.uri == "https://github.com/viash-io/viash")
    assert(gitPkg.tag == Some("v1.0.0"))
  }

  test("Package.unapply: handles github syntax") {
    val pkg = Package.unapply("github://viash-io/viash@v2.0.0")
    assert(pkg.isDefined)
    assert(pkg.get.isInstanceOf[GithubPackage])
    val githubPkg = pkg.get.asInstanceOf[GithubPackage]
    assert(githubPkg.repo == "viash-io/viash")
    assert(githubPkg.tag == Some("v2.0.0"))
    assert(githubPkg.uri == "https://github.com/viash-io/viash.git")
  }

  test("Package.unapply: handles viashhub syntax") {
    val pkg = Package.unapply("vsh://viash-io/viash@v2.0.0")
    assert(pkg.isDefined)
    assert(pkg.get.isInstanceOf[ViashhubPackage])
    val viashhubPkg = pkg.get.asInstanceOf[ViashhubPackage]
    assert(viashhubPkg.repo == "viash-io/viash")
    assert(viashhubPkg.tag == Some("v2.0.0"))
    assert(viashhubPkg.uri == "https://packages.viash-hub.com/viash-io/viash.git")
  }

  test("Package.unapply: handles viashhub syntax with implicit vsh organization") {
    val pkg = Package.unapply("vsh://viash@v2.0.0")
    assert(pkg.isDefined)
    assert(pkg.get.isInstanceOf[ViashhubPackage])
    val viashhubPkg = pkg.get.asInstanceOf[ViashhubPackage]
    assert(viashhubPkg.repo == "viash")
    assert(viashhubPkg.tag == Some("v2.0.0"))
    assert(viashhubPkg.uri == "https://packages.viash-hub.com/vsh/viash.git")
  }

  test("Package.unapply: handles viashhub syntax with explicit vsh organization") {
    val pkg = Package.unapply("vsh://vsh/viash@v2.0.0")
    assert(pkg.isDefined)
    assert(pkg.get.isInstanceOf[ViashhubPackage])
    val viashhubPkg = pkg.get.asInstanceOf[ViashhubPackage]
    assert(viashhubPkg.repo == "vsh/viash")
    assert(viashhubPkg.tag == Some("v2.0.0"))
    assert(viashhubPkg.uri == "https://packages.viash-hub.com/vsh/viash.git")
  }

  test("Package.unapply: handles local syntax") {
    val pkg = Package.unapply("local://path/to/repo")
    assert(pkg.isDefined)
    assert(pkg.get.isInstanceOf[LocalPackage])
  }

  test("Package.unapply: handles local dependency syntax") {
    val pkg = Package.unapply("local")
    assert(pkg.isDefined)
    assert(pkg.get.isInstanceOf[LocalPackage])
  }

  test("Package.unapply: returns None for unrecognized syntax") {
    val pkg = Package.unapply("unknown://foo.bar")
    assert(pkg.isEmpty)
  }

  test("Package caching") {
    val pkg = Package.unapply("vsh://hendrik/dependency_test2")

    pkg match {
      case Some(r: AbstractGitPackage) => {
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
      case _ => assert(false, "Expected AbstractGitPackage")
    }

  }

}
