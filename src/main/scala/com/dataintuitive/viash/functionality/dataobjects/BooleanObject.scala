package com.dataintuitive.viash.functionality.dataobjects

case class BooleanObject(
    name: String,
    alternatives: Option[List[String]] = None,
    description: Option[String] = None,
    default: Option[Boolean] = None,
    required: Option[Boolean] = None,
    flagValue: Option[Boolean] = None,
    tag: Option[String] = None,
    direction: Direction = Input,
    passthrough: Boolean = false
) extends DataObject[Boolean] {
  override val `type` = "boolean"

  require(
    (default == None) != (flagValue == None),
    message = s"Parameter $name: exactly one of default and flagValue must be defined, the other undefined."
  )
}

