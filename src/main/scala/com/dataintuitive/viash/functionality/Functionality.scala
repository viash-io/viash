package com.dataintuitive.viash.functionality

case class Functionality(
  name: String,
  description: Some[String],
  platform: String, 
  inputs: Seq[DataObject[_]],
  outputs: Seq[DataObject[_]],
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

import scala.io.Source
import io.circe.yaml.parser

object Functionality {
  def parse(filename: String): Functionality = {
    val str = Source.fromFile(filename).mkString
    val json = parser.parse(str)
    val plat = json match {
      case Right(js) => js.as[Functionality]
      case Left(e) => throw e
    }
    plat match {
      case Right(f) => f
      case Left(e) => throw e
    }
  }
}