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
import scala.util.Try

case class GitInfo(
  localRepo: Option[String],
  remoteRepo: Option[String],
  commit: Option[String],
  tag: Option[String]
)

object Git {
  def isGitRepo(path: File): Boolean = {
    Try {
      val out = Exec.run2(
        List("git", "rev-parse", "--is-inside-work-tree"),
        cwd = Some(path)
      )
      out.exitValue == 0
    }.getOrElse(false)
  }

  def getLocalRepo(path: File): Option[String] = {
    Try {
      val out = Exec.run2(
        List("git", "rev-parse", "--show-toplevel"),
        cwd = Some(path)
      )
      out.output.trim
    }.toOption
  }

  private val remoteRepoRegex = "(.*)\\s(.*)\\s(.*)".r

  def getRemoteRepo(path: File): Option[String] = {
    Try {
      val out = Exec.run2(
        List("git", "remote", "--verbose"),
        cwd = Some(path)
      )

      out.output
        .split("\n")
        .flatMap {
          case remoteRepoRegex(name, link, _) if name contains "origin" => Some(link)
          case _ => None
        }
        .head
    }.toOption
  }

  def getCommit(path: File): Option[String] = {
    Try {
      val out = Exec.run2(
        List("git", "rev-parse", "HEAD"),
        cwd = Some(path)
      )
      out.output.trim
    }.toOption
  }

  def getTag(path: File): Option[String] = {
    Try {
      val out = Exec.run2(
        List("git", "describe", "--tags"),
        cwd = Some(path)
      )
      out.exitValue match {
        case 0 => Some(out.output.trim)
        case _ => None
      }
    }.getOrElse(None)
  }

  def getInfo(path: File): GitInfo = {
    val igr = isGitRepo(path)

    if (igr) {
      val lgr = getLocalRepo(path)
      val rgr = getRemoteRepo(path)
      val gc = getCommit(path)
      val gt = getTag(path)

      GitInfo(lgr, rgr, gc, gt)
    } else {
      GitInfo(None, None, None, None)
    }
  }
}