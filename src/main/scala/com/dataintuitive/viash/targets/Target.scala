package com.dataintuitive.viash.target

import scala.io.Source
import io.circe.yaml.parser

trait Target {
  val `type`: String
  val r: Option[REnvironment]
  val python: Option[PythonEnvironment]
}

object Target {
  def parse(file: java.io.File): Target = {
    val str = Source.fromFile(file).mkString
    val json = parser.parse(str)
    val plat = json match {
      case Right(js) => js.as[Target]
      case Left(e) => throw e
    }
    plat match {
      case Right(f) => f
      case Left(e) => throw e
    }
  }
}