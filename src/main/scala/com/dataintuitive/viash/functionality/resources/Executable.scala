package com.dataintuitive.viash.functionality.resources

import com.dataintuitive.viash.functionality._

case class Executable(
  text: Option[String] = None,
  name: Option[String] = None,
  path: Option[String] = None,
  is_executable: Boolean = true
) extends Script {
  val `type` = "executable"

  val commentStr = "#"

  def command(script: String) = script
  def commandSeq(script: String) = Seq(script)

  def generateArgparse(functionality: Functionality): String = ""

}
