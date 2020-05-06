package com.dataintuitive.viash.functionality.resources

case class PlainFile(
  name: Option[String] = None,
  path: Option[String] = None,
  text: Option[String] = None,
  is_executable: Boolean = false
) extends Resource {
  override val `type` = "file"
}