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

import io.viash.schemas._
import io.viash.helpers.IO
import io.viash.helpers.Exec
import java.io.File
import java.nio.file.Paths

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
