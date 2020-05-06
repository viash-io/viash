package com.dataintuitive.viash.functionality.dataobjects

case class StringObject(
    name: String,
    alternatives: Option[List[String]] = None,
    description: Option[String] = None,
    default: Option[String] = None,
    required: Boolean = false,
    values: Option[List[String]] = None,
    tag: Option[String] = None,
    direction: Direction = Input,
    passthrough: Boolean = false
) extends DataObject[String] {
  override val `type` = "string"
}
