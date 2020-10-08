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
  val meta = Executable
  def copyResource(name: Option[String], path: Option[String], text: Option[String], is_executable: Boolean): Resource = {
    copy(name, path, text, is_executable)
  }

  def generatePlaceholder(functionality: Functionality): String = ""

  override def read: Option[String] = None

  override def write(path: Path, overwrite: Boolean) {}
}

object Executable extends ScriptObject {
  val commentStr = "#"
  val extension = "*"

  def command(script: String): String = {
    script
  }

  def commandSeq(script: String): Seq[String] = {
    Seq(script)
  }
}
