package com.dataintuitive.viash.functionality.dataobjects

case class DoubleObject(
    name: String,
    alternatives: List[String] = Nil,
    description: Option[String] = None,
    default: Option[Double] = None,
    required: Boolean = false,
    tag: Option[String] = None,
    direction: Direction = Input,
    multiple: Boolean = false,
    multiple_sep: Char = ':'
) extends DataObject[Double] {
  override val `type` = "double"
}
