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

abstract class Repository {
  val name: String
  val `type`: String
  val tag: Option[String]
  val path: Option[String]
  val localPath: String

  def copyRepo(
    name: String = this.name,
    `type`: String = this.`type`,
    tag: Option[String] = this.tag,
    path: Option[String] = this.path,
    localPath: String = this.localPath
  ): Repository

  // def isAlike(obj: Any): Boolean = {
  //   obj match {
  //     case o if o == this => true
  //     case o: Repository => this.equals(o.copy(name = this.name))
  //     case _ => false
  //   }
  // }
}



case class GithubRepository(
  name: String,
  `type`: String = "github",
  tag: Option[String],
  path: Option[String],
  localPath: String = ""
) extends Repository {
  def copyRepo(name: String, `type`: String, tag: Option[String], path: Option[String], localPath: String): Repository = copy(name, `type`, tag, path, localPath)
}

case class LocalRepository(
  name: String = "",
  `type`: String = "local",
  tag: Option[String] = None,
  path: Option[String] = None,
  localPath: String = ""
) extends Repository {
  def copyRepo(name: String, `type`: String, tag: Option[String], path: Option[String], localPath: String): Repository = copy(name, `type`, tag, path, localPath)
}
