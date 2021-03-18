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

package com.dataintuitive.viash.helpers

import java.io.File
import java.nio.file.{Files, Path, Paths}
import scala.reflect.io.Directory
import java.net.URI
import scala.io.Source
import java.net.URL
import sys.process._
import java.nio.charset.StandardCharsets
import com.dataintuitive.viash.functionality.resources.Resource

import java.nio.file.attribute.PosixFilePermission

object IO {
  def tempDir: File = {
    Paths.get(scala.util.Properties.envOrElse("VIASH_TEMP", "/tmp")).toFile
  }
  def makeTemp(name: String): File = {
    if (!tempDir.exists()) Files.createDirectories(tempDir.toPath)
    val temp = Files.createTempDirectory(tempDir.toPath, name).toFile
    temp.mkdirs()
    temp
  }

  def deleteRecursively(dir: File) {
    new Directory(dir).deleteRecursively()
  }

  private val uriRegex = "^[a-zA-Z0-9]*:".r

  def uri(path: String): URI = {
    val newURI = if (uriRegex.findFirstIn(path).isDefined) path else "file://" + new File(path).getAbsolutePath
    new URI(newURI)
  }

  def read(uri: URI): String = {
    val txtSource =
      if (uri.getScheme == "file") {
        Source.fromURI(uri)
      } else {
        Source.fromURL(uri.toURL)
      }
    try {
      txtSource.getLines.mkString("\n")
    } finally {
      txtSource.close()
    }
  }

  def readSome(uri: URI): Option[String] = {
    try {
      Some(read(uri))
    } catch {
      case _: Exception => {
        println(s"File at URI '$uri' not found")
        None
      }
    }
  }

  def write(uri: URI, path: Path, overwrite: Boolean, executable: Option[Boolean]): File = {
    val file = path.toFile

    if (overwrite && file.exists()) {
      file.delete()
    }

    if (uri.getScheme == "file") {
      val from = Paths.get(uri)
      // check if resource is a directory
      if (from.toFile.isDirectory) {
        Files.walk(from).forEach{file =>
          Files.copy(file, path.resolve(from.relativize(file)))
        }
      } else {
        Files.copy(from, path)
      }
    } else if (uri.getScheme == "http" || uri.getScheme == "https") {
      val url = new URL(uri.toString)
      url #> file !!
    } else {
      throw new RuntimeException("Unsupported scheme: " + uri.getScheme)
    }

    setPerms(path, executable)

    file
  }

  def write(text: String, path: Path, overwrite: Boolean, executable: Option[Boolean]): File = {
    val file = path.toFile

    if (overwrite && file.exists()) {
      file.delete()
    }

    Files.write(path, text.getBytes(StandardCharsets.UTF_8))

    setPerms(path, executable)

    file
  }

  def writeResources(
    resources: Seq[Resource],
    outputDir: java.io.File,
    overwrite: Boolean = true
  ) {
    // copy all files
    resources.foreach { resource =>
      // determine destination path
      val dest = Paths.get(outputDir.getAbsolutePath, resource.resourcePath)

      // create parent directory if it doesn't exist
      val parent = dest.toFile.getParentFile

      if (!parent.exists) {
        parent.mkdirs()
      }

      // write resource to path
      resource.write(dest, overwrite)
    }
  }

  def setPerms(path: Path, executable: Option[Boolean]): Unit = {
    if (executable.isDefined) {
      val perms = Files.getPosixFilePermissions(path)
      if (executable.get) {
        if (perms.contains(PosixFilePermission.OWNER_READ)) perms.add(PosixFilePermission.OWNER_EXECUTE)
        if (perms.contains(PosixFilePermission.GROUP_READ)) perms.add(PosixFilePermission.GROUP_EXECUTE)
        if (perms.contains(PosixFilePermission.OTHERS_READ)) perms.add(PosixFilePermission.OTHERS_EXECUTE)
      } else {
        perms.remove(PosixFilePermission.OWNER_EXECUTE)
        perms.remove(PosixFilePermission.GROUP_EXECUTE)
        perms.remove(PosixFilePermission.OTHERS_EXECUTE)
      }
      Files.setPosixFilePermissions(path, perms)
    }
  }
}
