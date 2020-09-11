package com.dataintuitive.viash

import io.circe.{Decoder, Encoder, Json}
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.{deriveConfiguredDecoder, deriveConfiguredEncoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

package object config {
  implicit val customConfig: Configuration = Configuration.default.withDefaults

  // encoders and decoders for Config
  implicit val encodeConfig: Encoder.AsObject[Config] = deriveConfiguredEncoder

  implicit val decodeConfig: Decoder[Config] = deriveConfiguredDecoder

  // encoder and decoder for version
  implicit val encodeVersion: Encoder[Version] = Encoder.instance {
    version => Json.fromString(version.toString)
  }
  implicit val decodeVersion: Decoder[Version] = Decoder.instance {
    cursor => {

      // workaround for parsing
      val y = cursor.value.as[String] match {
        case Left(_) => cursor.value.as[Double].map(_.toString)
        case Right(r) => Right(r)
      }

      y.map(s =>
        Version(s)
      )
    }
  }

  implicit val encodeInfo: Encoder[Info] = deriveEncoder
  implicit val decodeInfo: Decoder[Info] = deriveDecoder
}
