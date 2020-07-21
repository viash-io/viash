package com.dataintuitive.viash.platforms.requirements

trait Requirements {
  val `type`: String
  def installCommands: List[String]
  def dockerCommands: Option[String] = {
    if (installCommands.isEmpty) {
      None
    } else {
      Some(installCommands.mkString("RUN ", " && \\\n  ", "\n"))
    }
  }
}