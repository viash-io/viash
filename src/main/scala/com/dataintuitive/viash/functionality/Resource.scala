package com.dataintuitive.viash.functionality

case class Resource(
  name: String,
  path: Option[String],
  code: Option[String]
) {
  require(
    (path == None) != (code == None), 
    "Exactly one of path and code must be defined, the other undefined."
  )
}