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

import io.viash.helpers.Exec
import io.viash.helpers.IO
import java.nio.file.Paths
import io.viash.schemas._
import java.nio.file.Path
import java.io.File
import java.nio.file.Files

@description("Specifies a repository where dependency components can be found.")
abstract class Repository {
  @description("The identifier used to refer to this repository from dependencies.")
  val name: String

  @description("Defines the repository type. This determines how the repository will be fetched and handled.")
  val `type`: String

  @description("Defines which version of the dependency component to use. Typically this can be a specific tag, branch or commit hash.")
  val tag: Option[String]

  @description("Defines a subfolder of the repository to use as base to look for the dependency components.")
  val path: Option[String]

  @internalFunctionality
  @description("Local path to the repository files.")
  val localPath: String

  def copyRepo(
    name: String = this.name,
    `type`: String = this.`type`,
    tag: Option[String] = this.tag,
    path: Option[String] = this.path,
    localPath: String = this.localPath
  ): Repository

  def subOutputPath: String
}

object Repository {
  def fromSugarSyntax(s: String):Repository = {
    val repoRegex = raw"(\w+)://([A-Za-z0-9/_\-\.]+)@([A-Za-z0-9]+)".r

    // TODO this match is totally not up to snuff
    s match {
      case repoRegex(protocol, repo, tag) if protocol == "github" => GithubRepository("TODO generate name", uri = repo, tag = Some(tag) )
      case repoRegex(protocol, repo, tag) if protocol == "local" => LocalRepository("TODO generate name")
    }
  }

  // A poor man's approach to caching. The cache is only valid within this run of Viash.
  // However, it solves the issue of having to fetch the same repository over and over again, now we just do it once per run.
  // When proper multi-session caching would need to check for changed code bases, now we get this virtually for free.
  // We just fetched a code base and we have to assume it will not change within this session.
  private val cachedRepos = scala.collection.mutable.ListBuffer[Repository]()
  private def getCachedRepository(repo: Repository): Option[Repository] = {
    // We can't compare names because they don't hold actual information and can change between configs but still point to the same code base.
    val anonymizedRepo = repo.copyRepo(name = "")
    // Compare anonymized repos. Don't compare localPath as that is the information we're looking for.
    val foundRepo = cachedRepos.find(p => p.copyRepo(localPath = "").equals(anonymizedRepo))
    // Map Some(foundRepo) to original repo but with localPath filled in, returns None if no cache found.
    foundRepo.map(r => repo.copyRepo(localPath = r.localPath))
  }
  private def storeRepositoryInCache(repo: Repository) = {
    // don't cache local repositories with a path relative to the config. Identical paths but to different configs *might* result in different resolved paths.
    repo match {
      case r: LocalRepository if r.path.isDefined && !r.path.get.startsWith("/") =>
        // don't do anything, this repo is not reliably cacheable
      case _ =>
        cachedRepos.append(repo.copyRepo(name = ""))
    }
  }

  def cache(repo: Repository, configDir: Path, projectRootDir: Option[Path]): Repository = {

    // Check if we can get a locally cached version of the repo
    val existingRepo = getCachedRepository(repo)
    if (existingRepo.isDefined)
      return existingRepo.get

    // No cache found so fetch it
    val newRepo = repo match {
      case r: GithubRepository => {
        val r2 = r.checkoutSparse()
        val r3 = r2.checkout()
        // Stopgap solution to be able to use built repositories which were not built with dependency aware Viash version.
        // TODO remove this section once it's deemed no longer necessary
        if (Paths.get(r3.localPath, "target").toFile().exists() && !Paths.get(r3.localPath, "target", ".build.yaml").toFile().exists()) {
          Console.err.println(s"${Console.YELLOW}Creating temporary 'target/.build.yaml' file for ${r3.name} as this file seems to be missing.${Console.RESET}")
          Files.createFile(Paths.get(r3.localPath, "target", ".build.yaml"))
        }
        r3
      }
      case r: LocalRepository if r.path.isDefined => {
        val localPath = r.path.get match {
          case s if s.startsWith("/") => 
            // resolve path relative to the project root
            IO.resolveProjectPath(s, projectRootDir.map(p => p.toUri())).getPath()
          case s =>
            // resolve path relative to the config file
            configDir.resolve(s).toString()
        }
        r.copyRepo(localPath = localPath)
      }
      case r => r
    }

    // Store the newly fetched repo in the cache
    storeRepositoryInCache(newRepo)
    newRepo
  }
}

@description("A GitHub repository where remote dependency components can be found.")
@example(
  """type: github
    |uri: openpipelines-bio/modules
    |tag: 0.3.0
    |""".stripMargin,
  "yaml"
)
@example(
  """name: viash-testns
    |type: github
    |uri: viash-io/viash
    |tag: 0.7.1
    |path: src/test/resources/testns
    |""".stripMargin,
  "yaml"
  )
case class GithubRepository(
  name: String,

  @description("Defines the repository as a GitHub repository.")
  `type`: String = "github",

  @description("The GitHub `organization/repository_name` of the repository.")
  @example("uri: viash-io/viash", "yaml")
  uri: String,
  tag: Option[String],
  path: Option[String] = None,
  localPath: String = ""
) extends Repository {
  
  def copyRepo(
    name: String,
   `type`: String,
    tag: Option[String],
    path: Option[String],
    localPath: String
  ): Repository = {
    copy(name, `type`, uri, tag, path, localPath)
  }

  lazy val fullUri = s"git@github.com:$uri.git"

  // Clone of single branch with depth 1 but without checking out files
  def checkoutSparse(): GithubRepository = {
    val temporaryFolder = IO.makeTemp("viash_hub_repo")
    val cwd = Some(temporaryFolder.toFile)

    Console.err.println(s"temporaryFolder: $temporaryFolder uri: $uri fullUri: $fullUri")

    val singleBranch = tag match {
      case None => List("--single-branch")
      case Some(value) => List("--single-branch", "--branch", value)
    }

    val loggers = Seq[String => Unit] { (str: String) => {Console.err.println(str)} }

    val out = Exec.runCatch(
      List("git", "clone", fullUri, "--no-checkout", "--depth", "1") ++ singleBranch :+ ".",
      cwd = cwd,
      loggers = loggers,
    )

    copy(localPath = temporaryFolder.toString)
  }

  def hasBranch(name: String, cwd: Option[File]): Boolean = {
    val out = Exec.runCatch(
      List("git", "show-ref", "--verify", "--quiet", s"refs/heads/$name"),
      cwd = cwd
    )
    out.exitValue == 0
  }

  def hasTag(name: String, cwd: Option[File]): Boolean = {
    val out = Exec.runCatch(
      List("git", "show-ref", "--verify", "--quiet", s"refs/tags/$name"),
      cwd = cwd
    )
    out.exitValue == 0
  }

  // Checkout of files from already cloned repository. Limit file checkout to the path that was specified
  def checkout(): GithubRepository = {
    val pathStr = path.getOrElse(".")
    val cwd = Some(Paths.get(localPath).toFile)
    val checkoutName = tag match {
      case Some(name) if hasBranch(name, cwd) => s"origin/$name"
      case Some(name) if hasTag(name, cwd) => s"tags/$name"
      case _ => "origin/HEAD"
    }

    val out = Exec.runCatch(
      List("git", "checkout", checkoutName, "--", pathStr),
      cwd = cwd
    )

    Console.err.println(s"checkout out: ${out.command} ${out.exitValue} ${out.output}")

    if (path.isDefined)
      copy(localPath = Paths.get(localPath, path.get).toString)
    else
      // no changes to be made
      this
  }

  // Get the repository part of where dependencies should be located in the target/dependencies folder
  def subOutputPath: String = Paths.get(`type`, uri, tag.getOrElse("")).toString()
}

@description(
  """Defines a locally present and available repository.
    |This can be used to define components from the same code base as the current component.
    |Alternatively, this can be used to refer to a code repository present on the local hard-drive instead of fetchable remotely, for example during development.
    |""".stripMargin
)
case class LocalRepository(
  name: String = "",

  @description("Defines the repository as a locally present and available repository.")
  `type`: String = "local",
  tag: Option[String] = None,
  path: Option[String] = None,
  localPath: String = ""
) extends Repository {

  def copyRepo(
    name: String,
    `type`: String,
    tag: Option[String],
    path: Option[String],
    localPath: String
  ): Repository = {
    copy(name, `type`, tag, path, localPath)
  }

  def subOutputPath: String = Paths.get(`type`, tag.getOrElse("")).toString()
}
