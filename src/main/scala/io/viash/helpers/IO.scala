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
import java.nio.charset.MalformedInputException
import io.viash.exceptions.{MalformedInputException => ViashMalformedInputException}
import io.viash.helpers.Logging

/**
 * IO helper object for handling various file and directory operations.
 */
object IO extends Logging {

  /**
   * Returns the temporary directory path.
   *
   * @return a Path representing the temporary directory
   */
  def tempDir: Path = {
    Paths.get(scala.util.Properties.envOrElse("VIASH_TEMP", "/tmp")).toAbsolutePath()
  }

  /**
   * Creates a temporary directory with the specified name in the parent directory.
   *
   * @param name the name of the temporary directory
   * @param parentTempPath the optional parent directory for the temporary directory
   * @return the temporary directory path
   */
  def makeTemp(name: String, parentTempPath: Option[Path] = None): Path = {
    val workTempDir = parentTempPath.getOrElse(this.tempDir)
    if (!Files.exists(workTempDir)) Files.createDirectories(workTempDir)
    val temp = Files.createTempDirectory(workTempDir, name)
    Files.createDirectories(temp)
    temp
  }

  /**
   * Deletes a directory and its contents recursively.
   *
   * @param dir the path of the directory to be deleted
   */
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

  def copyFolder(src: Path, dest: Path): Unit = {
    Files.walkFileTree(src, new SimpleFileVisitor[Path] {
      override def preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult = {
        val newPath = dest.resolve(src.relativize(dir))
        newPath.toFile.mkdir()
        FileVisitResult.CONTINUE
      }
      override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
        val newPath = dest.resolve(src.relativize(file))
        Files.copy(file, newPath)
        FileVisitResult.CONTINUE
      }
    })
  }

  def copyFolder(src: String, dest: String): Unit = {
    copyFolder(Paths.get(src), Paths.get(dest))
  }

  private val uriRegex = "^[a-zA-Z0-9]*:".r

  /**
   * Creates a URI object from a given path.
   *
   * @param path the path to create the URI from
   * @return a URI object
   */
  def uri(path: String): URI = {
    if (uriRegex.findFirstIn(path).isDefined) new URI(path) else new File(path).toURI()
  }

  /**
   * Reads the content of a URI.
   *
   * @param uri the URI to read from
   * @return a String containing the content of the URI
   */
  def read(uri: URI): String = {
    val txtSource =
      if (uri.getScheme == "file") {
        Source.fromURI(uri)
      } else {
        Source.fromURL(uri.toURL)
      }
    try {
      txtSource.getLines().mkString("\n")
    } catch {
      case e: MalformedInputException => throw new ViashMalformedInputException(uri.toString(), e)
      case e: Throwable => throw e
    } finally {
      txtSource.close()
    }
  }

  /**
   * Reads the content of a URI and returns an Option.
   *
   * @param uri the URI to read from
   * @return an Option containing the content of the URI or None if an exception occurs
   */
  def readSome(uri: URI): Option[String] = {
    try {
      Some(read(uri))
    } catch {
      case _: Exception =>
        info(s"File at URI '$uri' not found")
        None
    }
  }

  /**
   * Writes content from a URI to a path.
   *
   * @param uri the URI containing the content to be written
   * @param path the path to write the content to
   * @param overwrite flag to indicate whether to overwrite existing content
   * @param executable an optional flag to set the file as executable
   * @return the destination path
   */
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

  /**
   * Writes a string to a specified path.
   *
   * @param text the content to be written
   * @param path the destination path
   * @param overwrite flag to indicate whether to overwrite existing content
   * @param executable an optional flag to set the file as executable
   * @return the destination path
   */
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

  /**
   * Writes resources to an output directory.
   *
   * @param resources a sequence of resources to be written
   * @param outputDir the output directory path
   * @param overwrite flag to indicate whether to overwrite existing content
   */
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

  /**
   * Sets file permissions for a given path.
   *
   * @param path the file path
   * @param executable an optional flag to set the file as executable
   */
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
   * Finds files in a directory based on the specified filter.
   *
   * @param sourceDir the source directory path
   * @param filter a function to filter the files based on their attributes
   * @return a list of paths that match the filter
   */
  def find(sourceDir: Path, filter: (Path, BasicFileAttributes) => Boolean): List[Path] = {
    val it = Files.find(sourceDir, Integer.MAX_VALUE, (p, b) => filter(p, b)).iterator()
    it.asScala.toList
  }

  /**
    * Resolve a path with respect to a URI.
    *
    * @param path A string containing an absolute path
    * @param uri The URI to resolve to.
    * @return A modified path.
    */
  def resolvePathWrtURI(path: String, uri: URI): String = {
    assert(path.startsWith("/"), "resolvePathWrtURI() should only be called when path starts with a '/'.")
    path.stripPrefix("/")
  }

  /**
    * Resolve a project-relative path w.r.t. the project uri
    * 
    * @param path A string containing an absolute path
    * @param projectURI The project URI to resolve to
    * @return A modified path as a URI.
    */
  def resolveProjectPath(path: String, projectURI: Option[URI]): URI = {
    if (projectURI.isEmpty) {
      throw new RuntimeException(s"One of the resources is relative to the project root ($path), but no project config file (_viash.yaml) could be found.")
    }
    val uri = projectURI.get
    val newPath = resolvePathWrtURI(path, uri)
    uri.resolve(newPath)
  }

  /**
    * Relativize a path w.r.t. a base path
    * 
    * If the path is not relative to the base path, the path is returned unchanged
    *
    * @param basePath The base path of the project
    * @param path The path to relativize
    * @return A relativized path or the original path if it was not relative to the base path
    */
  def anonymizePath(basePath: Option[Path], path: String): String = {    
    val pathPath = Paths.get(path)

    if (pathPath.isAbsolute()) {
      val relative = basePath.map(_.relativize(pathPath).toString())

      relative match {
        case Some(rel) if rel.startsWith("..") => Paths.get("[anonymized]", pathPath.toFile().getName()).toString()
        case Some(rel) => rel
        case None => Paths.get("[anonymized]", pathPath.toFile().getName()).toString()
      }
    } else {
      path
    }
  }
}
