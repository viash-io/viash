package com.dataintuitive.viash.functionality.platforms

import com.dataintuitive.viash.functionality.Functionality

case object PythonPlatform extends Platform {
  val `type` = "Python"
  
  def command(script: String) = {
    "python " + script
  }
  
  def generateArgparse(functionality: Functionality): String = {
    ""
  }
}
