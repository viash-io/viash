package com.dataintuitive.viash.functionality

case class Resource(
  name: String,
  path: Option[String] = None,
  code: Option[String] = None
) {
  require(
    (path == None) != (code == None), 
    message = "Exactly one of path and code must be defined, the other undefined."
  )
}