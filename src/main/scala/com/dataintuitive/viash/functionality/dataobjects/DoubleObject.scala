package com.dataintuitive.viash.functionality.dataobjects

case class DoubleObject(
    name: String,
    alternatives: List[String] = Nil,
    description: Option[String] = None,
    default: Option[Double] = None,
    required: Boolean = false,
    tag: Option[String] = None,
    direction: Direction = Input,
    passthrough: Boolean = false,
    multiple: Boolean = false,
    multiple_sep: String = ":"
) extends DataObject[Double] {
  override val `type` = "double"
}
