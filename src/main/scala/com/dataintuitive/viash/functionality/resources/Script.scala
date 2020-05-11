package com.dataintuitive.viash.functionality.resources

import com.dataintuitive.viash.functionality.Functionality

trait Script extends Resource {
  def command(script: String): String

  def generateArgparse(functionality: Functionality): String

  val commentStr: String
}
