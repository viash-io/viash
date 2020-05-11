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
    passthrough: Boolean = false
) extends BooleanObject {
  override val `type` = "boolean"

  val flagValue = None
}

case class BooleanObjectTrue(
    name: String,
    alternatives: List[String] = Nil,
    description: Option[String] = None,
    tag: Option[String] = None,
    direction: Direction = Input,
    passthrough: Boolean = false
) extends BooleanObject {
  override val `type` = "boolean_true"

  val required = false
  val flagValue = Some(true)
  val default = None
}
case class BooleanObjectFalse(
    name: String,
    alternatives: List[String] = Nil,
    description: Option[String] = None,
    tag: Option[String] = None,
    direction: Direction = Input,
    passthrough: Boolean = false
) extends BooleanObject {
  override val `type` = "boolean_false"

  val required = false
  val flagValue = Some(false)
  val default = None
}
