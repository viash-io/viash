package com.dataintuitive.viash.functionality.dataobjects

import java.io.File

case class FileObject(
    name: String,
    alternatives: Option[List[String]] = None,
    description: Option[String] = None,
    default: Option[File] = None,
    mustExist: Option[Boolean] = None,
    required: Option[Boolean] = None,
    tag: Option[String] = None,
    direction: Direction = Input,
    passthrough: Boolean = false
) extends DataObject[File] {
  override val `type` = "file"
}
