package com.dataintuitive.viash.platforms

import com.dataintuitive.viash.functionality.{Functionality}
import com.dataintuitive.viash.helpers.IOHelper
import io.circe.yaml.parser
import java.net.URI
import requirements._
import com.dataintuitive.viash.config.Version

trait Platform {
  val `type`: String
  val id: String
  val version: Option[Version]
  def modifyFunctionality(functionality: Functionality): Functionality

  val requirements: List[Requirements]
}

object Platform {
  def parse(uri: URI): Platform = {
    val str = IOHelper.read(uri)
    parser.parse(str)
      .fold(throw _, _.as[Platform])
      .fold(throw _, identity)
  }

  def read(path: String) = {
    parse(IOHelper.uri(path))
  }
}
