package com.dataintuitive.viash.functionality.resources

import com.dataintuitive.viash.functionality.Functionality

trait Script extends Resource {
  def command(script: String): String
  def commandSeq(script: String): Seq[String]

  def generateArgparse(functionality: Functionality): String

  val commentStr: String
}
