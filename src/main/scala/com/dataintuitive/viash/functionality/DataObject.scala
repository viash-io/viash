package com.dataintuitive.viash.functionality

import java.io.File

trait DataObject[Type] {
  val `type`: String
  val name: String
  val description: Option[String]
  val default: Option[Type]
  val required: Option[Boolean]
  
  def validate(value: Type): Boolean = {
    true
  }
}

case class StringObject(
    name: String,
    description: Option[String] = None,
    default: Option[String] = None,
    required: Option[Boolean] = None,
    values: Option[List[String]] = None
) extends DataObject[String] {
  override val `type` = "string"
}

case class IntegerObject(
    name: String,
    description: Option[String] = None,
    default: Option[Int] = None,
    required: Option[Boolean] = None
) extends DataObject[Int] {
  override val `type` = "integer"
}

case class DoubleObject(
    name: String,
    description: Option[String] = None,
    default: Option[Double] = None,
    required: Option[Boolean] = None
) extends DataObject[Double] {
  override val `type` = "double"
}

case class BooleanObject(
    name: String,
    description: Option[String] = None,
    default: Option[Boolean] = None,
    required: Option[Boolean] = None
) extends DataObject[Boolean] {
  override val `type` = "boolean"
}

case class FileObject(
    name: String,
    description: Option[String] = None,
    default: Option[File] = None,
    mustExist: Option[Boolean] = None,
    required: Option[Boolean] = None
) extends DataObject[File] {
  override val `type` = "file"
  
  override def validate(value: File) = {
    mustExist == None || !mustExist.get || value.exists
  }
}

