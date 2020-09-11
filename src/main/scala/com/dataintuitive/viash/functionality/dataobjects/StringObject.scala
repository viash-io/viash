package com.dataintuitive.viash.functionality.dataobjects

case class StringObject(
  name: String,
  alternatives: List[String] = Nil,
  description: Option[String] = None,
  default: Option[String] = None,
  required: Boolean = false,
  values: Option[List[String]] = None,
  tag: Option[String] = None,
  direction: Direction = Input,
  multiple: Boolean = false,
  multiple_sep: Char = ':'
) extends DataObject[String] {
  override val `type` = "string"
}
