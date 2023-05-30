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

import io.viash.helpers.IO
import io.viash.helpers.Exec
import java.io.File
import java.nio.file.Paths

abstract class AbstractGitRepository extends Repository {
  val uri: String

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
  def subOutputPath: String = Paths.get(`type`, uri, tag.getOrElse("")).toString()
}
