package com.dataintuitive.viash.helpers

import java.io.File
import java.nio.file.{Paths, Files, Path}
import scala.reflect.io.Directory

import java.net.URI
import scala.io.Source
import java.net.URL
import sys.process._
import java.nio.charset.StandardCharsets

object IOHelper {
  def makeTemp(name: String) = {
    val tempdir = Paths.get(scala.util.Properties.envOrElse("VIASH_TEMP", "/tmp"))
    Files.createDirectories(tempdir)
    val temp = Files.createTempDirectory(tempdir, name).toFile()
    temp.mkdirs()
    temp
  }

  def deleteRecursively(dir: File) {
    new Directory(dir).deleteRecursively()
  }

  def uri(path: String) = {
    new URI(if (path.contains("://")) path else "file://" + path)
  }

  def read(uri: URI): String = {
    Source.fromURI(uri).getLines.mkString("\n")
  }

  def write(uri: URI, path: Path, overwrite: Boolean): File = {
    val file = path.toFile()

    if (overwrite && file.exists()) {
      file.delete()
    }

    if (uri.getScheme == "file") {
      Files.copy(Paths.get(uri), path)
    } else if (uri.getScheme == "http" || uri.getScheme == "https") {
      val url = new URL(uri.toString)
      url #> file!!
    } else {
      throw new RuntimeException("Unsupported scheme: " + uri.getScheme)
    }

    file
  }

  def write(text: String, path: Path, overwrite: Boolean): File = {
    val file = path.toFile()

    if (overwrite && file.exists()) {
      file.delete()
    }

    Files.write(path, text.getBytes(StandardCharsets.UTF_8))

    file
  }
}