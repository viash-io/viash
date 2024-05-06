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

package io.viash.config.dependencies

import io.viash.helpers.{IO, Exec, Logging}
import java.io.File
import java.nio.file.Paths
import io.viash.exceptions.CheckoutException
import io.viash.helpers.SysEnv
import java.nio.file.Path

trait AbstractGitRepository extends Repository with Logging {
  val uri: String
  val storePath: String

  def copyRepo(
   `type`: String,
    tag: Option[String],
    path: Option[String],
    localPath: String
  ): AbstractGitRepository

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
  
  
  // Get the repository part of where dependencies should be located in the target/dependencies folder
  def subOutputPath: String = Paths.get(`type`, storePath, tag.getOrElse("")).toString()

  protected def doGitClone(uri: String, cwd: Option[File]): Exec.ExecOutput = {
    val singleBranch = tag match {
      case None => List("--single-branch")
      case Some(value) => List("--single-branch", "--branch", value)
    }

    val loggers = Seq[String => Unit] { (str: String) => {info(str)} }
    Exec.runCatch(
      List("git", "clone", uri, "--no-checkout", "--depth", "1") ++ singleBranch :+ ".",
      cwd = cwd,
      loggers = loggers,
    )
  }

  protected def checkGitAuthentication(uri: String): Boolean = {
    val res = Exec.runCatch(
      List("git", "ls-remote", uri),
    )
    res.exitValue == 0
  }

  protected def getLocalHash(): String = {
    val cwd = Some(Paths.get(localPath).toFile())
    val loggers = Seq[String => Unit] { (str: String) => {info(str)} }
    val res = Exec.runCatch(
      List("git", "rev-list", "-n", "1", "HEAD"),
      cwd = cwd,
      loggers = loggers,
    )
    res.exitValue match {
      case 0 => res.output.trim()
      case _ => "Local hash not found"
    }
  }

  protected def getRemoteHash(uri: String): String = {
    val res = Exec.runCatch(
      List("git", "ls-remote", uri, tag.getOrElse("HEAD")),
    )
    res.exitValue match {
      case 0 => res.output.split("\t").head
      case _ => "Remote hash not found"
    }
  }

  def getCheckoutUri(): String
  def getCacheIdentifier(): Option[String]
  def fullCachePath: Option[Path] = {
    val cacheIdentifier = getCacheIdentifier()
    cacheIdentifier.map(cacheIdentifier => Paths.get(SysEnv.viashHome).resolve("cache").resolve(cacheIdentifier))
  }

  def findInCache(): Option[AbstractGitRepository] = {
    val cachePath = fullCachePath
    val res = cachePath match {
      case Some(path) =>
        val pathFile = path.toFile()
        pathFile.exists() && pathFile.isDirectory() match {
          case true => Some(copyRepo(localPath = path.toString))
          case false => None
        }
      case None => None
    }
    info(s"findInCache: $res")
    res
  }

  def checkCacheStillValid(): Boolean = {
    val uri = getCheckoutUri()
    val remoteHash = getRemoteHash(uri)
    val localHash = getLocalHash()
    info(s"remoteHash: $remoteHash localHash: $localHash")
    remoteHash == localHash
  }

  // Clone of single branch with depth 1 but without checking out files
  def checkoutSparse(): AbstractGitRepository = {
    val temporaryFolder = IO.makeTemp("viash_hub_repo")
    val cwd = Some(temporaryFolder.toFile)

    val uri = getCheckoutUri()

    info(s"temporaryFolder: $temporaryFolder uri: $uri")

    val out = doGitClone(uri, cwd)
    if (out.exitValue != 0)
      throw new CheckoutException(this)

    copyRepo(localPath = temporaryFolder.toString)
  }

  // Get cached repo if it exists and is still valid, otherwise checkout a new one
  // If a new one is checked out, copy it to the cache
  // If a cached repo is used, copy it to a new temporary folder
  def getSparseRepoInTemp(): AbstractGitRepository = {
    findInCache() match {
      case Some(repo) if repo.checkCacheStillValid() => 
        info(s"Using cached repo in ${repo.localPath}")
        val newTemp = IO.makeTemp("viash_hub_repo")
        IO.copyFolder(repo.localPath, newTemp.toString)
        repo.copyRepo(localPath = newTemp.toString)
      case _ =>
        val repo = checkoutSparse()
        info(s"Copying repo to cache ${repo.fullCachePath}")
        repo.fullCachePath match {
          case Some(cachePath) =>
            val cachePathFile = cachePath.toFile()
            if (cachePathFile.exists())
              IO.deleteRecursively(cachePath)
            cachePathFile.mkdirs()
            IO.copyFolder(repo.localPath, cachePath.toString)
          case None => 
        }
        repo
    }
  }

  // Checkout of files from already cloned repository. Limit file checkout to the path that was specified
  def checkout(): AbstractGitRepository = {
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

    info(s"checkout out: ${out.command} ${out.exitValue} ${out.output}")

    if (path.isDefined)
      copyRepo(localPath = Paths.get(localPath, path.get).toString)
    else
      // no changes to be made
      this
  }
}
