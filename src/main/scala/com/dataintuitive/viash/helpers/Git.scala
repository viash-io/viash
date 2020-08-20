package com.dataintuitive.viash.helpers

import sys.process._
import java.io.File
import scala.util.Try

case class GitInfo(
  localRepo: Option[String],
  remoteRepo: Option[String],
  commit: Option[String]
)

object Git {
  def isGitRepo(path: File) = {
    Exec.run2(
      List("git", "rev-parse", "--is-inside-work-tree"),
      cwd = Some(path)
    ).exitValue == 0
  }

  // TODO: fix: remote git repo:    [Ljava.lang.String;@117159c0

  def localGitRepo(path: File) = {
    Try(
      Exec.run(
        List("git", "rev-parse", "--show-toplevel"),
        cwd = Some(path)
      ).trim
    ).toOption
  }

  def remoteGitRepo(path: File) = {
   Try(
      Exec.run(
        List("git", "remote", "--verbose"),
        cwd = Some(path)
      ).split("\n")
      .map(_.split("\\s"))
      .filter(_.headOption.getOrElse("NA") contains "origin")
      .headOption.getOrElse("No remote configured").toString
    ).toOption
  }

  def gitCommit(path: File) = {
    Try(
      Exec.run2(
        List("git", "log", "--oneline"),
        cwd = Some(path)
      ).output
      .split("\n")
      .head
      .split(" ")
      .head
    ).toOption
  }

  def getInfo(path: File) = {
    val igr = isGitRepo(path)

    if (igr) {
      val lgr = localGitRepo(path)
      val rgr = remoteGitRepo(path)
      val gc = gitCommit(path)

      GitInfo(lgr, rgr, gc)
    } else {
      GitInfo(None, None, None)
    }
  }
}