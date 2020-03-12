package com.dataintuitive.viash.functionality

case class Functionality(
  name: String,
  description: Some[String],
  platform: String, 
  parameters: Seq[Parameter[_]], 
  resources: Seq[Resource]
) {
  val supportedPlatforms = List("R", "Python")
  
  require(
    supportedPlatforms contains platform,
    message = "platform must be 'R' or 'Python'."
  )
  
  require(
    resources.count(_.name.startsWith("main")) == 1,
    message = "Define exactly one resource whose name begins with 'main'."
  )
}