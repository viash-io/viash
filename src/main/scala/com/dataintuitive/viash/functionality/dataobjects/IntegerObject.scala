package com.dataintuitive.viash.functionality.dataobjects

case class IntegerObject(
    name: String,
    alternatives: Option[List[String]] = None,
    description: Option[String] = None,
    default: Option[Int] = None,
    required: Option[Boolean] = None,
    tag: Option[String] = None,
    direction: Direction = Input,
    passthrough: Boolean = false
) extends DataObject[Int] {
  override val `type` = "integer"
}
