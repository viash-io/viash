package com.dataintuitive.viash.functionality

import java.io.File

case class Resource(
  name: String,
  path: Option[File],
  code: Option[String]
) {
  require(
    (path == None) != (code == None), 
    message = "Exactly one of path and code must be defined, the other undefined."
  )
}