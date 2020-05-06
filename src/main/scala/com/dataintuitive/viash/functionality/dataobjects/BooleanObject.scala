package com.dataintuitive.viash.functionality.dataobjects

abstract class BooleanObject extends DataObject[Boolean] {
  val flagValue: Option[Boolean]
}

case class BooleanObjectRegular(
    name: String,
    alternatives: Option[List[String]] = None,
    description: Option[String] = None,
    default: Option[Boolean] = None,
    required: Option[Boolean] = None,
    tag: Option[String] = None,
    direction: Direction = Input,
    passthrough: Boolean = false
) extends BooleanObject {
  override val `type` = "boolean"

  val flagValue = None
}

case class BooleanObjectTrue(
    name: String,
    alternatives: Option[List[String]] = None,
    description: Option[String] = None,
    required: Option[Boolean] = None,
    tag: Option[String] = None,
    direction: Direction = Input,
    passthrough: Boolean = false
) extends BooleanObject {
  override val `type` = "boolean_true"

  val flagValue = Some(true)
  val default = None
}
case class BooleanObjectFalse(
    name: String,
    alternatives: Option[List[String]] = None,
    description: Option[String] = None,
    required: Option[Boolean] = None,
    tag: Option[String] = None,
    direction: Direction = Input,
    passthrough: Boolean = false
) extends BooleanObject {
  override val `type` = "boolean_false"

  val flagValue = Some(false)
  val default = None
}
