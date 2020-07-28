package com.dataintuitive.viash

import io.circe.{ Decoder, Encoder, Json }
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

import com.dataintuitive.viash.functionality.resources.PlainFile
import com.dataintuitive.viash.functionality._
import com.dataintuitive.viash.platforms._
import io.circe.yaml.Printer

package object meta {

  case class StrippedMeta(
    version: String,
    git_remote: String,
    git_commit: String,
    functionality_path: String,
    platform_path: String,
    output_path: String,
    executable_path: String
  )
  case class Nested(
    info: StrippedMeta,
    functionality: Functionality,
    platform: Platform
  )

  implicit val encodeStrippedMeta: Encoder[StrippedMeta] = deriveEncoder
  implicit val encodeNested: Encoder[Nested] = deriveEncoder

}
