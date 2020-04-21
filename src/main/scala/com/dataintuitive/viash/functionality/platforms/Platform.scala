package com.dataintuitive.viash.functionality.platforms

import com.dataintuitive.viash.functionality.{Functionality, Resource}

trait Platform {
  val `type`: String

  def command(script: String): String

  def generateArgparse(functionality: Functionality): String

  val commentStr: String
}

object Platform {
  def get(str: String) = {
    str.toLowerCase() match {
      case "r" => RPlatform
      case "python" => PythonPlatform
      case "bash" => BashPlatform
      case "native" => NativePlatform
      case "nextFlow" => NativePlatform
      case _ => throw new RuntimeException(s"Unrecognised platform '${str}'.")
    }
  }
}
