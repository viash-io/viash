package com.dataintuitive.viash

import io.circe.{ Decoder, Encoder, Json }
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.{deriveConfiguredDecoder, deriveConfiguredEncoder}

package object config {
  implicit val customConfig: Configuration = Configuration.default.withDefaults

  // encoders and decoders for Object
  implicit val encodeConfig: Encoder.AsObject[Config] = deriveConfiguredEncoder

  implicit val decodeConfig: Decoder[Config] = deriveConfiguredDecoder
}
