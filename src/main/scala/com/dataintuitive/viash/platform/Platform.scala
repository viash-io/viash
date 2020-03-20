package com.dataintuitive.viash.platform

import scala.io.Source
import io.circe.yaml.parser

trait Platform{
  val `type`: String
  val r: Option[REnvironment]
  val python: Option[PythonEnvironment]
}

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