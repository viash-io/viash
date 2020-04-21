package com.dataintuitive.viash.functionality

import java.io.File

abstract class DataObject[Type] {
  val `type`: String
  val name: String
  val alternatives: Option[List[String]]
  val description: Option[String]
  val default: Option[Type]
  val required: Option[Boolean]
  val direction: Direction
  val tag: Option[String]

  private val pattern = "^(-*)(.*)$".r
  val pattern(otype, plainName) = name

  def validate(value: Type): Boolean = {
    true
  }

}

case class StringObject(
    name: String,
    alternatives: Option[List[String]] = None,
    description: Option[String] = None,
    default: Option[String] = None,
    required: Option[Boolean] = None,
    values: Option[List[String]] = None,
    tag: Option[String] = None,
    direction: Direction = Input
) extends DataObject[String] {
  override val `type` = "string"
}

case class IntegerObject(
    name: String,
    alternatives: Option[List[String]] = None,
    description: Option[String] = None,
    default: Option[Int] = None,
    required: Option[Boolean] = None,
    tag: Option[String] = None,
    direction: Direction = Input
) extends DataObject[Int] {
  override val `type` = "integer"
}

case class DoubleObject(
    name: String,
    alternatives: Option[List[String]] = None,
    description: Option[String] = None,
    default: Option[Double] = None,
    required: Option[Boolean] = None,
    tag: Option[String] = None,
    direction: Direction = Input
) extends DataObject[Double] {
  override val `type` = "double"
}

case class BooleanObject(
    name: String,
    alternatives: Option[List[String]] = None,
    description: Option[String] = None,
    default: Option[Boolean] = None,
    required: Option[Boolean] = None,
    flagValue: Option[Boolean] = None,
    tag: Option[String] = None,
    direction: Direction = Input
) extends DataObject[Boolean] {
  override val `type` = "boolean"
}

case class FileObject(
    name: String,
    alternatives: Option[List[String]] = None,
    description: Option[String] = None,
    default: Option[File] = None,
    mustExist: Option[Boolean] = None,
    required: Option[Boolean] = None,
    tag: Option[String] = None,
    direction: Direction = Input
) extends DataObject[File] {
  override val `type` = "file"

  override def validate(value: File) = {
    mustExist == None || !mustExist.get || value.exists
  }
}
