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

package io.viash.functionality.resources

import java.net.URI

import io.viash.helpers.{IO, MissingResourceFileException}
import java.nio.file.{Path, Paths}
import java.nio.file.NoSuchFileException
import io.viash.schemas._

trait Resource {
  @description("Specifies the type of the resource. The first resource cannot be of type `file`. When the type is not specified, the default type is simply `file`.")
  val `type`: String

  @description("Filename, the resulting name of the resource.")
  val dest: Option[String]

  @description("The folder from where to get the resource.")
  val parent: Option[URI]

  @description("The path of the input file. Can be a relative or an absolute path, or a URI.")
  val path: Option[String]

  @description("The raw content of the input file. Exactly one of path or text must be defined, the other undefined.")
  val text: Option[String]

  @description("Whether the resulting file is made executable.")
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
        val patEsc = pat.replaceAll(" ", "%20")
        val newPath = parent match {
          case Some(par) => par.resolve(patEsc).toString
          case _ => pat
        }
        Some(IO.uri(newPath))
      }
      case None => None
    }
  }

  private val basenameRegex = ".*/".r
  private val getFolderNameRegex = ".*?([^/]+|/)/*$".r
  

  /**
   * Get the path of the resource relative to the resources dir.
   * If the dest value is defined, use that. Otherwise, use the basename of
   * the path value.
   */
  def resourcePath: String = {
    if (dest.isDefined) {
      dest.get
    } else {
      getFolderNameRegex.replaceFirstIn(Paths.get(path.get).normalize.toString, "$1")
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

  def write(path: Path, overwrite: Boolean): Unit = {
    try {
      if (text.isDefined) {
        IO.write(text.get, path, overwrite, executable = is_executable)
      } else {
        IO.write(uri.get, path, overwrite, executable = is_executable)
      }
    } catch {
      case e: NoSuchFileException => 
        val configString = parent match {
          case Some(uri) => Some(uri.toString)
          case _ => None
        }
        throw MissingResourceFileException.apply(path.toString(), configString, e)
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
