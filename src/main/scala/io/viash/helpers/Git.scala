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

import java.io.File

case class GitInfo(
  localRepo: Option[String],
  remoteRepo: Option[String],
  commit: Option[String],
  tag: Option[String]
)

object Git extends Logging {
  @inline
  protected def getLoggers(fn: String) = Seq[String => Unit] { str: String => trace(s"$fn: $str") }

  def isGitRepo(path: File): Boolean = {
    val out = Exec.runCatch(
      List("git", "rev-parse", "--is-inside-work-tree"),
      cwd = Some(path),
      loggers = getLoggers("isGitRepo"),
    )
    out.exitValue == 0
  }

  def getLocalRepo(path: File): Option[String] = {
    Exec.runOpt(
      List("git", "rev-parse", "--show-toplevel"),
      cwd = Some(path),
      loggers = getLoggers("getLocalRepo"),
    ).map(_.trim)
  }

  private val remoteRepoRegex = "(.*)\\s(.*)\\s(.*)".r
  private val removeCredentialsRegex = """^(\w*://|git@)?(\w*:?\w+@)?([^@]*)$""".r

  def getRemoteRepo(path: File): Option[String] = {
    Exec.runOpt(
      List("git", "remote", "--verbose"),
      cwd = Some(path),
      loggers = getLoggers("getRemoteRepo"),
    ).flatMap{ line => 
      line
        .split("\n")
        .flatMap {
          case remoteRepoRegex(name, link, _) if name contains "origin" => Some(link)
          case _ => None
        }
        .headOption
        .map(s => removeCredentialsRegex.replaceFirstIn(s, "$1$3"))
    }
  }

  def getCommit(path: File): Option[String] = {
    Exec.runOpt(
      List("git", "rev-parse", "HEAD"),
      cwd = Some(path),
      loggers = getLoggers("getCommit")
    ).map(_.trim)
  }

  def getTag(path: File): Option[String] = {
    Exec.runOpt(
      List("git", "describe", "--tags"),
      cwd = Some(path),
      loggers = getLoggers("getTag"),
    ).map(_.trim)
  }

  def getInfo(path: File): GitInfo = {
    if (isGitRepo(path)) {
      GitInfo(
        localRepo = getLocalRepo(path),
        remoteRepo = getRemoteRepo(path),
        commit = getCommit(path),
        tag = getTag(path)
      )
    } else {
      GitInfo(None, None, None, None)
    }
  }

  def hasBranch(name: String, path: File): Boolean = {
    val out = Exec.runCatch(
      List("git", "show-ref", "--verify", "--quiet", s"refs/heads/$name"),
      cwd = Some(path),
      loggers = getLoggers("hasBranch"),
    )
    out.exitValue == 0
  }

  def hasTag(name: String, path: File): Boolean = {
    val out = Exec.runCatch(
      List("git", "show-ref", "--verify", "--quiet", s"refs/tags/$name"),
      cwd = Some(path),
      loggers = getLoggers("hasTag"),
    )
    out.exitValue == 0
  }

  def getRemoteHash(uri: String, tag: Option[String]): Option[String] = {
    Exec.runOpt(
      List("git", "ls-remote", uri, tag.getOrElse("HEAD")),
      loggers = getLoggers("getRemoteHash"),
    ).map(_.split("\t").head)
  }

  def checkGitAuthentication(uri: String): Boolean = {
    val res = Exec.runCatch(
      List("git", "ls-remote", uri, "HEAD"),
      loggers = getLoggers("checkGitAuthentication"),
    )
    res.exitValue == 0
  }

  def cloneSparseAndShallow(uri: String, tag: Option[String], path: File): Exec.ExecOutput = {
    val singleBranch = tag match {
      case None => List("--single-branch")
      case Some(value) => List("--single-branch", "--branch", value)
    }
    Exec.runCatch(
      List("git", "clone", uri, "--no-checkout", "--depth", "1") ++ singleBranch :+ ".",
      cwd = Some(path),
      loggers = getLoggers("checkoutSparse"),
    )
  }

  def checkout(checkoutName: String, subPath: Option[String], path: File): Exec.ExecOutput = {
    Exec.runCatch(
      List("git", "checkout", checkoutName, "--", subPath.getOrElse(".")),
      cwd = Some(path),
      loggers = getLoggers("checkout"),
    )
  }

}
