package com.dataintuitive.viash.platforms.requirements

case class NextFlowRequirements(
  executor: Option[String] = None,
  publishSubdir: Option[Boolean]
) extends Requirements {
  val `type` = "nextflow"

  def installCommands: List[String] = {
    Nil
  }
}
