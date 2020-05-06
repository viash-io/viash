package com.dataintuitive.viash

import io.circe.{ Decoder, Encoder, Json }
//import io.circe.generic.extras.Configuration
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
//import io.circe.generic.extras.semiauto.{deriveConfiguredDecoder, deriveConfiguredEncoder}
import functionality.dataobjects._
import functionality.resources._

package object functionality {
//  implicit val customConfig: Configuration = Configuration.default.withDefaults

  // encoder and decoder for Functionality
  implicit val encodeFunctionality: Encoder[Functionality] = deriveEncoder
  implicit val decodeFunctionality: Decoder[Functionality] = deriveDecoder
}