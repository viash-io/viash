package com.dataintuitive.viash.functionality.dataobjects

abstract class DataObject[Type] {
  val `type`: String
  val name: String
  val alternatives: List[String]
  val description: Option[String]
  val default: Option[Type]
  val required: Boolean
  val direction: Direction
  val tag: Option[String]
  val passthrough: Boolean
  val multiple: Boolean
  val multiple_sep: String

  private val pattern = "^(-*)(.*)$".r
  val pattern(otype, plainName) = name
}
