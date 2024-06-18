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

import io.viash.helpers.{IO, Exec, Logging, Git}
import java.io.File
import java.nio.file.Paths
import io.viash.exceptions.CheckoutException
import io.viash.helpers.SysEnv
import java.nio.file.Path

trait AbstractGitRepository extends Repository with Logging {
  val uri: String
  val storePath: String

  @inline
  protected def getLoggers(fn: String) = Seq[String => Unit] { str: String => debug(s"$fn: $str") }

  def copyRepo(
   `type`: String,
    tag: Option[String],
    path: Option[String],
    localPath: String
  ): AbstractGitRepository 
  
  // Get the repository part of where dependencies should be located in the target/dependencies folder
  def subOutputPath: String = Paths.get(`type`, storePath, tag.getOrElse("")).toString()

  def getCheckoutUri(): String
  def getCacheIdentifier(): Option[String]
  def fullCachePath: Option[Path] = {
    val cacheIdentifier = getCacheIdentifier()
    cacheIdentifier.map(cacheIdentifier => Paths.get(SysEnv.viashHome).resolve("repositories").resolve(cacheIdentifier))
  }

  def findInCache(): Option[AbstractGitRepository] = {
    val cachePath = fullCachePath
    cachePath match {
      case Some(path) if path.toFile.isDirectory() =>
        debug(s"Found in cache: $path")
        Some(copyRepo(localPath = path.toString))
      case _ => None
    }
  }

  // compare the remote hash with the local hash
  def checkCacheStillValid(): Boolean = {
    if (AbstractGitRepository.isValidatedCache(localPath))
      return true
    val uri = getCheckoutUri()
    val remoteHash = Git.getRemoteHash(uri, tag)
    val localHash = Git.getCommit(Paths.get(localPath).toFile())
    debug(s"remoteHash: $remoteHash localHash: $localHash")
    val res = remoteHash == localHash && remoteHash.isDefined
    if (res)
      AbstractGitRepository.markValidatedCache(localPath)
    res
  }

  // Clone of single branch with depth 1 but without checking out files
  def checkoutSparse(): AbstractGitRepository = {
    val temporaryFolder = IO.makeTemp("viash_hub_repo")
    val uri = getCheckoutUri()

    debug(s"temporaryFolder: $temporaryFolder uri: $uri")

    val out = Git.cloneSparseAndShallow(uri, tag, temporaryFolder.toFile)
    if (out.exitValue != 0)
      throw new CheckoutException(this)

    copyRepo(localPath = temporaryFolder.toString)
  }

  // Get cached repo if it exists and is still valid, otherwise checkout a new one
  // If a new one is checked out, copy it to the cache
  // If a cached repo is used, copy it to a new temporary folder
  def getSparseRepoInTemp(): AbstractGitRepository = {
    info(s"Fetching repo for $uri")
    findInCache() match {
      case Some(repo) if repo.checkCacheStillValid() => 
        debug(s"Using cached repo from ${repo.localPath}")
        val newTemp = IO.makeTemp("viash_hub_repo")
        IO.copyFolder(repo.localPath, newTemp.toString)
        repo.copyRepo(localPath = newTemp.toString)
      case _ =>
        debug(s"Cache either not present or outdated; checkout repository")
        val repo = checkoutSparse()
        repo.fullCachePath match {
          case Some(cachePath) =>
            debug(s"Copying repo to cache ${repo.fullCachePath}")
            val cachePathFile = cachePath.toFile()
            if (cachePathFile.exists())
              IO.deleteRecursively(cachePath)
            cachePathFile.mkdirs()
            IO.copyFolder(repo.localPath, cachePath.toString)
            AbstractGitRepository.markValidatedCache(cachePath.toString)
          case None => 
        }
        repo
    }
  }

  // Checkout of files from already cloned repository. Limit file checkout to the path that was specified
  def checkout(): AbstractGitRepository = {
    val localPathFile = Paths.get(localPath).toFile
    val checkoutName = tag match {
      case Some(name) if Git.hasBranch(name, localPathFile) => s"origin/$name"
      case Some(name) if Git.hasTag(name, localPathFile) => s"tags/$name"
      case _ => "origin/HEAD"
    }

    val out = Git.checkout(checkoutName, path, localPathFile)

    if (out.exitValue != 0)
      warn(s"checkout out: ${out.command} ${out.exitValue} ${out.output}")

    if (path.isDefined)
      copyRepo(localPath = Paths.get(localPath, path.get).toString)
    else
      // no changes to be made
      this
  }
}

object AbstractGitRepository extends Logging {
  private val validatedCaches = scala.collection.mutable.ListBuffer[String]()
  private def markValidatedCache(cacheIdentifier: String): Unit = {
    debug("Marking cache as validated: " + cacheIdentifier)
    if (!validatedCaches.contains(cacheIdentifier))
      validatedCaches += cacheIdentifier
  }
  private def isValidatedCache(cacheIdentifier: String): Boolean = {
    val res = validatedCaches.contains(cacheIdentifier)
    debug(s"Cache is validated: $cacheIdentifier $res")
    res
  }
}