package com.dataintuitive.viash.functionality.resources

import java.net.URI

import com.dataintuitive.viash.helpers.IO
import java.nio.file.{Path, Paths}

trait Resource {
  val `type`: String
  val dest: Option[String]
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

  val uri: Option[URI] = path.map(IO.uri)

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
    val file =
      if (text.isDefined) {
        IO.write(text.get, path, overwrite)
      } else {
        IO.write(uri.get, path, overwrite)
      }

    if (is_executable.isDefined) {
      file.setExecutable(is_executable.get)
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
        val newPath = Some(parent.resolve(path.get).toString)
        this.copyResource(path = newPath)
      }
    }
  }

  // TODO: This can probably be solved much nicer.
  def copyResource(
    path: Option[String] = this.path,
    text: Option[String] = this.text,
    dest: Option[String] = this.dest,
    is_executable: Option[Boolean] = this.is_executable
  ): Resource
}