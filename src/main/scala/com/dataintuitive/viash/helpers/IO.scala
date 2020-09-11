package com.dataintuitive.viash.helpers

import java.io.File
import java.nio.file.{Paths, Files, Path}
import scala.reflect.io.Directory

import java.net.URI
import scala.io.Source
import java.net.URL
import sys.process._
import java.nio.charset.StandardCharsets

import com.dataintuitive.viash.functionality.resources.Resource

object IO {
  def makeTemp(name: String): File = {
    val tempdir = Paths.get(scala.util.Properties.envOrElse("VIASH_TEMP", "/tmp"))
    if (!tempdir.toFile.exists()) Files.createDirectories(tempdir)
    val temp = Files.createTempDirectory(tempdir, name).toFile
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

  def write(uri: URI, path: Path, overwrite: Boolean): File = {
    val file = path.toFile

    if (overwrite && file.exists()) {
      file.delete()
    }

    if (uri.getScheme == "file") {
      Files.copy(Paths.get(uri), path)
    } else if (uri.getScheme == "http" || uri.getScheme == "https") {
      val url = new URL(uri.toString)
      url #> file !!
    } else {
      throw new RuntimeException("Unsupported scheme: " + uri.getScheme)
    }

    file
  }

  def write(text: String, path: Path, overwrite: Boolean): File = {
    val file = path.toFile

    if (overwrite && file.exists()) {
      file.delete()
    }

    Files.write(path, text.getBytes(StandardCharsets.UTF_8))

    file
  }

  def writeResources(
    resources: Seq[Resource],
    outputDir: java.io.File,
    overwrite: Boolean = true
  ) {
    // copy all files
    resources.foreach { resource =>
      val dest = Paths.get(outputDir.getAbsolutePath, resource.filename)
      resource.write(dest, overwrite)
    }
  }

}