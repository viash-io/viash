package com.dataintuitive.viash.platforms.requirements

trait Requirements {
  val `type`: String
  def installCommands: List[String]
}