package com.dataintuitive.viash.functionality

import io.circe.{ Decoder, Encoder, Json }

package object platforms {
    // encoder and decoder for Platform
  implicit val encodePlatform: Encoder[Platform] = Encoder.instance {
    platform =>
      Json.fromString(platform.`type`)
  }
  implicit val decodePlatform: Decoder[Platform] = Decoder.instance {
    cursor =>
      cursor.value.as[String].map(Platform.get)
  }
}