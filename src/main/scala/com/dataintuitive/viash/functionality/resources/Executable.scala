package com.dataintuitive.viash.functionality.resources

import com.dataintuitive.viash.functionality._
import java.nio.file.Path

case class Executable(
  path: Option[String] = None,
  text: Option[String] = None,
  dest: Option[String] = None,
  is_executable: Boolean = true
) extends Script {
  val `type` = "executable"
  val meta = Executable
  def copyResource(path: Option[String], text: Option[String], dest: Option[String], is_executable: Boolean): Resource = {
    copy(path = path, text = text, dest = dest, is_executable = is_executable)
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
