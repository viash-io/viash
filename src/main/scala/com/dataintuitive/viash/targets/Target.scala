package com.dataintuitive.viash.targets

import com.dataintuitive.viash.functionality.{Functionality, Resource}
import scala.io.Source
import java.io.File
import io.circe.yaml.parser

trait Target {
  val `type`: String
  val r: Option[REnvironment]
  val python: Option[PythonEnvironment]
  
  def setupResources(functionality: Functionality): Seq[Resource]
}

object Target {
  def parse(file: File): Target = {
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