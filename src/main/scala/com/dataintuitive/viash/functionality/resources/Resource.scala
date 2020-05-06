package com.dataintuitive.viash.functionality.resources

import com.dataintuitive.viash.functionality.Functionality
import java.io.File

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

  val filename = name.getOrElse(new File(path.get).getName())
}
