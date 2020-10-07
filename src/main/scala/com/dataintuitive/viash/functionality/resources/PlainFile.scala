package com.dataintuitive.viash.functionality.resources

case class PlainFile(
  name: Option[String] = None,
  path: Option[String] = None,
  text: Option[String] = None,
  is_executable: Boolean = false
) extends Resource {
  override val `type` = "file"
  def copyResource(name: Option[String], path: Option[String], text: Option[String], is_executable: Boolean): Resource = {
    copy(name, path, text, is_executable)
  }
}