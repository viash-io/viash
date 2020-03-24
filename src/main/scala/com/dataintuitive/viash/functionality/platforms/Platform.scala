package com.dataintuitive.viash.functionality.platforms

import com.dataintuitive.viash.functionality.Functionality

trait Platform {
  val `type`: String
  
  def command(script: String): String
  
  def generateArgparse(functionality: Functionality): String
}

object Platform {
  def get(str: String) = {
    str match {
      case "R" => RPlatform
      case "Python" => PythonPlatform
      case s => throw new RuntimeException(s"Unrecognised platform '${s}'.") 
    }
  }
}