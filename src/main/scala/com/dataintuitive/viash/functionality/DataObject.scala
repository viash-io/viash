package com.dataintuitive.viash.functionality

import java.io.File

abstract class DataObject[Type] {
  val `type`: String
  val name: Option[String]
  val short: Option[Char]
  val description: Option[String]
  val default: Option[Type]
  val required: Option[Boolean]
  val direction: Option[Direction]
  val tag: Option[String]

  require(
    name.isDefined || short.isDefined,
    message = "At least either 'name' or 'short' must be defined."
  )

  def validate(value: Type): Boolean = {
    true
  }

}

case class StringObject(
    name: Option[String] = None,
    short: Option[Char] = None,
    description: Option[String] = None,
    default: Option[String] = None,
    required: Option[Boolean] = None,
    values: Option[List[String]] = None,
    tag: Option[String] = None,
    direction: Option[Direction] = None
) extends DataObject[String] {
  override val `type` = "string"
}

case class IntegerObject(
    name: Option[String] = None,
    short: Option[Char] = None,
    description: Option[String] = None,
    default: Option[Int] = None,
    required: Option[Boolean] = None,
    tag: Option[String] = None,
    direction: Option[Direction] = None
) extends DataObject[Int] {
  override val `type` = "integer"
}

case class DoubleObject(
    name: Option[String] = None,
    short: Option[Char] = None,
    description: Option[String] = None,
    default: Option[Double] = None,
    required: Option[Boolean] = None,
    tag: Option[String] = None,
    direction: Option[Direction] = None
) extends DataObject[Double] {
  override val `type` = "double"
}

case class BooleanObject(
    name: Option[String] = None,
    short: Option[Char] = None,
    description: Option[String] = None,
    default: Option[Boolean] = None,
    required: Option[Boolean] = None,
    flagValue: Option[Boolean] = None,
    tag: Option[String] = None,
    direction: Option[Direction] = None
) extends DataObject[Boolean] {
  override val `type` = "boolean"
}

case class FileObject(
    name: Option[String] = None,
    short: Option[Char] = None,
    description: Option[String] = None,
    default: Option[File] = None,
    mustExist: Option[Boolean] = None,
    required: Option[Boolean] = None,
    tag: Option[String] = None,
    direction: Option[Direction] = None
) extends DataObject[File] {
  override val `type` = "file"
  
  override def validate(value: File) = {
    mustExist == None || !mustExist.get || value.exists
  }
}

