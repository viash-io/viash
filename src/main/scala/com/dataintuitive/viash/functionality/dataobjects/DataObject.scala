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
  val multiple: Boolean
  val multiple_sep: Char

  require(
    !required || !default.isDefined,
    s"parameter $name should not be required and also have a default parameter."
  )

  private val pattern = "^(-*)(.*)$".r
  val pattern(otype, plainName) = name

  val par = "par_" + plainName
  val VIASH_PAR = "VIASH_PAR_" + plainName.toUpperCase()
}
