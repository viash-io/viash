package com.dataintuitive.viash.targets

import com.dataintuitive.viash.functionality.{Functionality, Resource}
import scala.io.Source
import java.io.File
import io.circe.yaml.parser
import com.dataintuitive.viash.targets.environments._

trait Target {
  val `type`: String
  def modifyFunctionality(functionality: Functionality): Functionality
}

object Target {
  def parse(file: File): Target = {
    import io.circe.generic.extras.Configuration
    implicit val customConfig: Configuration = Configuration.default.withDefaults
    
    val str = Source.fromFile(file).mkString
    val json = parser.parse(str)
    val value = json match {
      case Right(js) => js.as[Target]
      case Left(e) => throw e
    }
    value match {
      case Right(value) => value
      case Left(e) => throw e
    }
  }
}