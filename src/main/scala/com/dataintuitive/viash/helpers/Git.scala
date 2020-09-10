package com.dataintuitive.viash.helpers

import java.io.File
import scala.util.Try

case class GitInfo(
  localRepo: Option[String],
  remoteRepo: Option[String],
  commit: Option[String]
)

object Git {
  def isGitRepo(path: File): Boolean = {
    Try(
      Exec.run2(
        List("git", "rev-parse", "--is-inside-work-tree"),
        cwd = Some(path)
      ).exitValue == 0
    ).getOrElse(false)
  }

  def getLocalRepo(path: File): Option[String] = {
    Try(
      Exec.run(
        List("git", "rev-parse", "--show-toplevel"),
        cwd = Some(path)
      ).trim
    ).toOption
  }

  private val remoteRepoRegex = "(.*)\\s(.*)\\s(.*)".r

  def getRemoteRepo(path: File): Option[String] = {
   Try(
      Exec.run(
        List("git", "remote", "--verbose"),
        cwd = Some(path)
      )
      .split("\n")
      .flatMap{
        case remoteRepoRegex(name, link, _) if name contains "origin" => Some(link)
        case _ => None
      }
      .headOption
      .getOrElse("No remote configured")
    ).toOption
  }

  def getCommit(path: File): Option[String] = {
    Try(
      Exec.run(
        List("git", "rev-parse", "HEAD"),
        cwd = Some(path)
      ).trim
    ).toOption
  }

  def getInfo(path: File): GitInfo = {
    val igr = isGitRepo(path)

    if (igr) {
      val lgr = getLocalRepo(path)
      val rgr = getRemoteRepo(path)
      val gc = getCommit(path)

      GitInfo(lgr, rgr, gc)
    } else {
      GitInfo(None, None, None)
    }
  }
}