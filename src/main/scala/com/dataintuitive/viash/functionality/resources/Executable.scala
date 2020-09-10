package com.dataintuitive.viash.functionality.resources

import com.dataintuitive.viash.functionality._
import java.nio.file.Path

case class Executable(
  text: Option[String] = None,
  name: Option[String] = None,
  path: Option[String] = None,
  is_executable: Boolean = true
) extends Script {
  val `type` = "executable"

  val commentStr = "#"

  def command(script: String): String = script
  def commandSeq(script: String): Seq[String] = Seq(script)

  def generatePlaceholder(functionality: Functionality): String = ""

  override def read: Option[String] = None
  override def write(path: Path, overwrite: Boolean) { }
}
