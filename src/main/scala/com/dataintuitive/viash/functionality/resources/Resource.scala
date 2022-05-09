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

package com.dataintuitive.viash.functionality.resources

import java.net.URI

import com.dataintuitive.viash.helpers.IO
import java.nio.file.{Path, Paths}

trait Resource {
  val `type`: String
  val dest: Option[String]
  val parent: Option[URI]
  val path: Option[String]
  val text: Option[String]
  val is_executable: Option[Boolean]

  require(
    path.isEmpty != text.isEmpty,
    message = s"Resource: either 'path' or 'text' should be defined, the other undefined."
  )
  require(
    dest.isDefined || path.isDefined,
    message = s"Resource: 'dest' needs to be defined if no 'path' is defined."
  )

  // val uri: Option[URI] = path.map(IO.uri)
  def uri: Option[URI] = {
    path match {
      case Some(pat) => {
        val newPath = parent match {
          case Some(par) => par.resolve(pat).toString
          case _ => pat
        }
        Some(IO.uri(newPath))
      }
      case None => None
    }
  }

  private val basenameRegex = ".*/".r

  /**
   * Get the path of the resource relative to the resources dir.
   * If the dest value is defined, use that. Otherwise, use the basename of
   * the path value.
   */
  def resourcePath: String = {
    if (dest.isDefined) {
      dest.get
    } else {
      basenameRegex.replaceFirstIn(path.get, "")
    }
  }
  /**
   * Get the 'basename' of the object
   */
  def filename: String = {
    basenameRegex.replaceFirstIn(resourcePath, "")
  }

  def read: Option[String] = {
    if (text.isDefined) {
      text
    } else {
      IO.readSome(uri.get)
    }
  }

  def write(path: Path, overwrite: Boolean) {
    if (text.isDefined) {
      IO.write(text.get, path, overwrite, executable = is_executable)
    } else {
      IO.write(uri.get, path, overwrite, executable = is_executable)
    }
  }

  def copyWithAbsolutePath(parent: URI): Resource = {
    if (this.isInstanceOf[Executable] || path.isEmpty || path.get.contains("://")) {
      this
    } else {
      val p = Paths.get(path.get).toFile
      if (p.isAbsolute) {
        this
      } else {
        this.copyResource(parent = Some(parent))
      }
    }
  }

  // TODO: This can probably be solved much nicer.
  def copyResource(
    path: Option[String] = this.path,
    text: Option[String] = this.text,
    dest: Option[String] = this.dest,
    is_executable: Option[Boolean] = this.is_executable,
    parent: Option[URI] = this.parent
  ): Resource
}