package com.dataintuitive.viash.targets

import io.circe.{ Decoder, Encoder }
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.{deriveConfiguredDecoder, deriveConfiguredEncoder}

package object environments {
  implicit val customConfig: Configuration = Configuration.default.withDefaults

  implicit val encodeREnvironment: Encoder.AsObject[REnvironment] = deriveConfiguredEncoder
  implicit val decodeREnvironment: Decoder[REnvironment] = deriveConfiguredDecoder

  implicit val encodePythonEnvironment: Encoder.AsObject[PythonEnvironment] = deriveConfiguredEncoder
  implicit val decodePythonEnvironment: Decoder[PythonEnvironment] = deriveConfiguredDecoder

  implicit val encodeAptEnvironment: Encoder.AsObject[AptEnvironment] = deriveConfiguredEncoder
  implicit val decodeAptEnvironment: Decoder[AptEnvironment] = deriveConfiguredDecoder
}