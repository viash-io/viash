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
  def parse(file: java.io.File): Platform = {
    val str = Source.fromFile(file).mkString
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