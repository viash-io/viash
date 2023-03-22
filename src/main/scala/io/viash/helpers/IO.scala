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

import java.io.{BufferedOutputStream, FileOutputStream, File, IOException}
import java.nio.file.{FileVisitResult, Files, Path, Paths, SimpleFileVisitor}
import java.nio.file.attribute.BasicFileAttributes
import scala.reflect.io.Directory
import java.net.URI
import scala.io.{Codec, Source}
import java.net.URL
import sys.process._
import java.nio.charset.StandardCharsets
import io.viash.functionality.resources.Resource

import java.nio.file.attribute.PosixFilePermission
import java.util.Comparator
import java.io.FileNotFoundException
import scala.jdk.CollectionConverters._

object IO {
  def tempDir: Path = {
    Paths.get(scala.util.Properties.envOrElse("VIASH_TEMP", "/tmp")).toAbsolutePath()
  }
  def makeTemp(name: String, parentTempPath: Option[Path] = None): Path = {
    val workTempDir = parentTempPath.getOrElse(tempDir)
    if (!Files.exists(workTempDir)) Files.createDirectories(workTempDir)
    val temp = Files.createTempDirectory(workTempDir, name)
    Files.createDirectories(temp)
    temp
  }

  def deleteRecursively(dir: Path): Unit = {
    Files.walkFileTree(dir, new SimpleFileVisitor[Path] {
      override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
        Files.delete(file)
        FileVisitResult.CONTINUE
      }
      override def postVisitDirectory(dir: Path, exc: IOException): FileVisitResult = {
        Files.delete(dir)
        FileVisitResult.CONTINUE
      }
    })
  }

  private val uriRegex = "^[a-zA-Z0-9]*:".r

  def uri(path: String): URI = {
    if (uriRegex.findFirstIn(path).isDefined) new URI(path) else new File(path).toURI()
  }

  def read(uri: URI): String = {
    val txtSource =
      if (uri.getScheme == "file") {
        Source.fromURI(uri)
      } else {
        Source.fromURL(uri.toURL)
      }
    try {
      txtSource.getLines().mkString("\n")
    } finally {
      txtSource.close()
    }
  }

  def readSome(uri: URI): Option[String] = {
    try {
      Some(read(uri))
    } catch {
      case _: Exception =>
        println(s"File at URI '$uri' not found")
        // println()
        // println("###################")
        // println("Exception:")
        // println(e)
        // println("###################")

        // def getListOfFiles(d: File): List[File] = {
        //   if (d.exists && d.isDirectory) {
        //     d.listFiles.filter(_.isFile).toList
        //   } else {
        //     List[File]()
        //   }
        // }
        // println("Files in directory:")
        // val dir = Paths.get(uri).getParent().toFile()
        // println(getListOfFiles(dir).mkString("  * ", "\n  * ", ""))
        // println("###################")

        None
      }
  }

  def write(uri: URI, path: Path, overwrite: Boolean, executable: Option[Boolean]): Path = {
    if (overwrite && Files.exists(path)) {
      if (Files.isDirectory(path)) {
        deleteRecursively(path)
      } else {
        Files.delete(path)
      }
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
      // Download the file in a safe-ish way that will throw exceptions
      // when the file can't be downloaded (vs. using e.g. `#>`)
      try {
        val out = new BufferedOutputStream(new FileOutputStream(path.toFile))
        val bytes = Source.fromURL(uri.toString)(Codec.ISO8859).map(_.toByte).toArray
        out.write(bytes)
        out.close()
      }
      catch {
        case e: FileNotFoundException =>
          throw new RuntimeException("Could not download file: " + uri.toString)
      }
    } else {
      throw new RuntimeException("Unsupported scheme: " + uri.getScheme)
    }

    setPerms(path, executable)

    path
  }

  def write(
    text: String, 
    path: Path, 
    overwrite: Boolean = false,
    executable: Option[Boolean] = None
  ): Path = {
    if (overwrite && Files.exists(path)) {
      if (Files.isDirectory(path)) {
        deleteRecursively(path)
      } else {
        Files.delete(path)
      }
    }

    Files.write(path, text.getBytes(StandardCharsets.UTF_8))

    setPerms(path, executable)

    // sleep to avoid concurrency issue where
    // file is executed to build docker containers
    // but apparently still in the process of being written
    Thread.sleep(50)

    path
  }

  def writeResources(
    resources: Seq[Resource],
    outputDir: Path,
    overwrite: Boolean = true
  ): Unit = {
    // copy all files
    resources.foreach { resource =>
      // determine destination path
      val dest = outputDir.resolve(resource.resourcePath).toAbsolutePath

      // create parent directory if it doesn't exist
      val parent = dest.getParent

      if (!Files.exists(parent)) {
        Files.createDirectories(parent)
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
  
  /**
   * Find all files in a directory and filter according to their properties.
   */
  def find(sourceDir: Path, filter: (Path, BasicFileAttributes) => Boolean): List[Path] = {
    val it = Files.find(sourceDir, Integer.MAX_VALUE, (p, b) => filter(p, b)).iterator()
    it.asScala.toList
  }

  /**
    * TODO: write doc string
    */
  def preResolveProject(path: String, projectURI: Option[URI]): (String, URI) = {
    assert(path.startsWith("/"), "prePresolveProject() should only be called when path starts with a '/'.")
    if (projectURI.isEmpty) {
      throw new RuntimeException(s"One of the resources is relative to the project root ($path), but no project config file (_viash.yaml) could be found.")
    }
    (path.stripPrefix("/"), projectURI.get)
  }
  def resolveProject(path: String, projectURI: Option[URI]): URI = {
    val (path1, proj) = preResolveProject(path, projectURI)
    proj.resolve(path1)
  }
}
