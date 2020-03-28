package com.dataintuitive.viash.functionality.platforms

import com.dataintuitive.viash.functionality._

case object NativePlatform extends Platform {

  val `type` = "Native"

  val commentStr = "#"

  def command(script: String) = script

  def generateArgparse(functionality: Functionality): String = ""

}
