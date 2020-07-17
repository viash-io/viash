package com.dataintuitive.viash.functionality.dataobjects

import java.io.File

case class FileObject(
    name: String,
    alternatives: List[String] = Nil,
    description: Option[String] = None,
    default: Option[File] = None,
    must_exist: Boolean = false,
    required: Boolean = false,
    tag: Option[String] = None,
    direction: Direction = Input,
    multiple: Boolean = false,
    multiple_sep: Char = ':'
) extends DataObject[File] {
  override val `type` = "file"
}
