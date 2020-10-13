package com.dataintuitive.viash.functionality.resources

case class PlainFile(
  path: Option[String] = None,
  text: Option[String] = None,
  dest: Option[String] = None,
  is_executable: Boolean = false
) extends Resource {
  override val `type` = "file"
  def copyResource(path: Option[String], text: Option[String], dest: Option[String], is_executable: Boolean): Resource = {
    copy(path = path, text = text, dest = dest, is_executable = is_executable)
  }
}