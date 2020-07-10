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
    passthrough: Boolean = false,
    multiple: Boolean = false,
    multiple_sep: String = ":"
) extends DataObject[String] {
  override val `type` = "string"
}
