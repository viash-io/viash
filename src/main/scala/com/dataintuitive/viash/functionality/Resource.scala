package com.dataintuitive.viash.functionality

trait ResourceTrait {
  val name: String
  val path: Option[String]
  val code: Option[String]
  val isExecutable: Boolean

  require(
    (path == None) != (code == None),
    message = s"Resource $name: exactly one of path and code must be defined, the other undefined."
  )
}

case class Resource(
  `type`: ResourceType,
  name: String,
  path: Option[String] = None,
  code: Option[String] = None,
  is_executable: Boolean = false
) extends ResourceTrait