package com.dataintuitive.viash.functionality.dataobjects

abstract class DataObject[Type] {
  val `type`: String
  val name: String
  val alternatives: Option[List[String]]
  val description: Option[String]
  val default: Option[Type]
  val required: Option[Boolean]
  val direction: Direction
  val tag: Option[String]
  val passthrough: Boolean

  private val pattern = "^(-*)(.*)$".r
  val pattern(otype, plainName) = name
}

