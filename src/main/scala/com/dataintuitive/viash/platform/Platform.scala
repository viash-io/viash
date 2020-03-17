package com.dataintuitive.viash.platform

case class Platform(
  native: Option[NativePlatform] = None,
  docker: Option[DockerPlatform] = None
) {
  require(
    (native == None) != (docker == None), 
    message = "Define either native or docker, not both or neither."
  )
}

import scala.io.Source
import io.circe.yaml.parser

object Platform {
  def parse(filename: String): Platform = {
    val str = Source.fromFile(filename).mkString
    val json = parser.parse(str)
    val plat = json match {
      case Right(js) => js.as[Platform]
      case Left(e) => throw e
    }
    plat match {
      case Right(f) => f
      case Left(e) => throw e
    }
  }
}