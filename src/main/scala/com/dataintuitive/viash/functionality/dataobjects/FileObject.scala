package com.dataintuitive.viash.functionality.dataobjects

import java.io.File

case class FileObject(
    name: String,
    alternatives: List[String] = Nil,
    description: Option[String] = None,
    default: Option[File] = None,
    mustExist: Option[Boolean] = None,
    required: Boolean = false,
    tag: Option[String] = None,
    direction: Direction = Input,
    passthrough: Boolean = false
) extends DataObject[File] {
  override val `type` = "file"
}
