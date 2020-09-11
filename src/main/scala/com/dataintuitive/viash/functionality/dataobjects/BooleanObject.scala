package com.dataintuitive.viash.functionality.dataobjects

abstract class BooleanObject extends DataObject[Boolean] {
  val flagValue: Option[Boolean]
}

case class BooleanObjectRegular(
  name: String,
  alternatives: List[String] = Nil,
  description: Option[String] = None,
  default: Option[Boolean] = None,
  required: Boolean = false,
  tag: Option[String] = None,
  direction: Direction = Input,
  multiple: Boolean = false,
  multiple_sep: Char = ':'
) extends BooleanObject {
  override val `type` = "boolean"

  val flagValue: Option[Boolean] = None
}

case class BooleanObjectTrue(
  name: String,
  alternatives: List[String] = Nil,
  description: Option[String] = None,
  tag: Option[String] = None,
  direction: Direction = Input,
) extends BooleanObject {
  override val `type` = "boolean_true"

  val required: Boolean = false
  val flagValue: Option[Boolean] = Some(true)
  val default: Option[Boolean] = None
  val multiple: Boolean = false
  val multiple_sep: Char = ':'
}

case class BooleanObjectFalse(
  name: String,
  alternatives: List[String] = Nil,
  description: Option[String] = None,
  tag: Option[String] = None,
  direction: Direction = Input,
) extends BooleanObject {
  override val `type` = "boolean_false"

  val required: Boolean = false
  val flagValue: Option[Boolean] = Some(false)
  val default: Option[Boolean] = None
  val multiple: Boolean = false
  val multiple_sep: Char = ':'
}
