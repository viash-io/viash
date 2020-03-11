package com.dataintuitive.viash.parameters

import java.io.File

trait Parameter[Type] {
  val `type`: String
  val name: String
  val description: Option[String]
  val default: Option[Type]
  
  def validate(value: Type): Boolean = {
    true
  }
}

case class StringParameter(
    name: String,
    description: Option[String] = None,
    default: Option[String] = None
) extends Parameter[String] {
  override val `type` = "string"
}

case class IntegerParameter(
    name: String,
    description: Option[String] = None,
    default: Option[Int] = None
) extends Parameter[Int] {
  override val `type` = "integer"
}

case class DoubleParameter(
    name: String,
    description: Option[String] = None,
    default: Option[Double] = None
) extends Parameter[Double] {
  override val `type` = "double"
}

case class BooleanParameter(
    name: String,
    description: Option[String] = None,
    default: Option[Boolean] = None
) extends Parameter[Boolean] {
  override val `type` = "boolean"
}

case class FileParameter(
    name: String,
    description: Option[String] = None,
    default: Option[File] = None,
    must_exist: Boolean = false
) extends Parameter[File] {
  override val `type` = "file"
  
  override def validate(value: File) = {
    !must_exist || value.exists
  }
}

