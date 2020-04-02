package com.dataintuitive.viash.functionality

trait ResourceTrait {
  val name: String
  val path: Option[String]
  val code: Option[String]
  val isExecutable: Boolean
  
  require(
    (path == None) != (code == None), 
    message = "Exactly one of path and code must be defined, the other undefined."
  )
}

case class Resource(
  name: String,
  path: Option[String] = None,
  code: Option[String] = None,
  isExecutable: Boolean = false
) extends ResourceTrait