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

package io.viash.config.resources

import java.net.URI

import io.viash.helpers.IO
import io.viash.exceptions.MissingResourceFileException
import java.nio.file.{Path, Paths}
import java.nio.file.NoSuchFileException
import io.viash.schemas._

@description(
  """Resources are files that support the component. The first resource should be @[a script](scripting_languages) that will be executed when the component is run. Additional resources will be copied to the same directory.
    |
    |Common properties:
    |
    | * type: `file` / `r_script` / `python_script` / `bash_script` / `javascript_script` / `scala_script` / `csharp_script`, specifies the type of the resource. The first resource cannot be of type `file`. When the type is not specified, the default type is simply `file`.
    | * dest: filename, the resulting name of the resource.  From within a script, the file can be accessed at `meta["resources_dir"] + "/" + dest`. If unspecified, `dest` will be set to the basename of the `path` parameter.
    | * path: `path/to/file`, the path of the input file. Can be a relative or an absolute path, or a URI. Mutually exclusive with `text`.
    | * text: ...multiline text..., the content of the resulting file specified as a string. Mutually exclusive with `path`.
    | * is_executable: `true` / `false`, whether the resulting resource file should be made executable.
    |""".stripMargin)
@example(
  """resources:
    |  - type: r_script
    |    path: script.R
    |  - type: file
    |    path: resource1.txt
    |""".stripMargin,
    "yaml")
@subclass("BashScript")
@subclass("CSharpScript")
@subclass("Executable")
@subclass("JavaScriptScript")
@subclass("NextflowScript")
@subclass("PlainFile")
@subclass("PythonScript")
@subclass("RScript")
@subclass("ScalaScript")
trait Resource {
  @description("Specifies the type of the resource. The first resource cannot be of type `file`. When the type is not specified, the default type is simply `file`.")
  val `type`: String

  @description("""Resulting filename of the resource. From within a script, the file can be accessed at `meta["resources_dir"] + "/" + dest`. If unspecified, `dest` will be set to the basename of the `path` parameter.""")
  val dest: Option[String]

  @internalFunctionality
  val parent: Option[URI]

  @description("The path of the input file. Can be a relative or an absolute path, or a URI. Mutually exclusive with `text`.")
  val path: Option[String]

  @description("The content of the resulting file specified as a string. Mutually exclusive with `path`.")
  val text: Option[String]

  @description("Whether the resulting resource file should be made executable.")
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
    resolvedPath match {
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

  def resolvedPath: Option[String] = {
    if (this.isInstanceOf[Executable]) {
      return path
    }
    if (path.isEmpty || path.get.contains(":")) {
      return path
    }
    val pathStr = path.get
    if (pathStr.startsWith("/")) {
      if (parent.isEmpty) {
        throw new RuntimeException(s"One of the resources is relative to the package root ($path), but no package config file (_viash.yaml) could be found.")
      }
      val pathStr1 = IO.resolvePathWrtURI(pathStr, parent.get)
      return path
    }
    path
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
      getFolderNameRegex.replaceFirstIn(Paths.get(resolvedPath.get).normalize.toString, "$1")
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

  def copyWithAbsolutePath(parent: URI, packageDir: Option[URI]): Resource = {
    // don't modify if the resource represents a command that should be available in the PATH
    if (this.isInstanceOf[Executable]) {
      return this
    }
    // don't modify if the resource doesn't have a PATH or contains a URI
    if (path.isEmpty || path.get.contains(":")) {
      return this
    }

    // if the path starts with a /, resolve it w.r.t. to the package dir
    val pathStr = path.get
    if (pathStr.startsWith("/")) {
      if (packageDir.isEmpty) {
        throw new RuntimeException(s"One of the resources is relative to the package root ($path), but no package config file (_viash.yaml) could be found.")
      }
      // val pathStr1 = IO.resolvePathWrtURI(pathStr, packageDir.get)
      return copyResource(
        // path = Some(pathStr1),
        parent = packageDir
      )
    }
    
    // if the path is relative (which it probably should be),
    // set the directory of the config as the parent of this file.
    if (!Paths.get(pathStr).isAbsolute) {
      return copyResource(
        parent = Some(parent)
      )
    }

    this
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
