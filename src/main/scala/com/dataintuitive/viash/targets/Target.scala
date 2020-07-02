package com.dataintuitive.viash.targets

import com.dataintuitive.viash.functionality.{Functionality}
import com.dataintuitive.viash.helpers.IOHelper
import io.circe.yaml.parser
import com.dataintuitive.viash.targets.environments._
import java.net.URI

trait Target {
  val `type`: String
  def modifyFunctionality(functionality: Functionality): Functionality
}

object Target {
  def parse(uri: URI): Target = {
    val str = IOHelper.read(uri)
    parser.parse(str)
      .fold(throw _, _.as[Target])
      .fold(throw _, identity)
  }
}