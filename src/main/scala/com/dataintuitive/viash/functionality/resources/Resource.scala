package com.dataintuitive.viash.functionality.resources

import com.dataintuitive.viash.functionality.Functionality
import com.dataintuitive.viash.helpers.IOHelper
import java.nio.file.{Paths, Path}

trait Resource {
  val `type`: String
  val name: Option[String]
  val path: Option[String]
  val text: Option[String]
  val is_executable: Boolean

  require(
    path.isEmpty != text.isEmpty,
    message = s"For each resource, either 'path' or 'text' should be defined, the other undefined."
  )
  require(
    name.isDefined || path.isDefined,
    message = s"For each resources, 'name' needs to be defined if no 'path' is defined."
  )

  val uri = path.map(IOHelper.uri)

  def filename = {
    if (name.isDefined) {
      name.get
    } else {
      val path = Paths.get(uri.get.getPath())
      path.getFileName().toString
    }
  }

  def read: Option[String] = {
    if (text.isDefined) {
      text
    } else {
      Some(IOHelper.read(uri.get))
    }
  }

  def write(path: Path, overwrite: Boolean) {
    val file =
      if (text.isDefined) {
        IOHelper.write(text.get, path, overwrite)
      } else {
        IOHelper.write(uri.get, path, overwrite)
      }

    file.setExecutable(is_executable)
  }
}
