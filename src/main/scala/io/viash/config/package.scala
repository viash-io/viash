/*
 * Copyright (C) 2020  Data Intuitive
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.viash

import io.circe.{Decoder, Encoder, Json}
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.{deriveConfiguredDecoder, deriveConfiguredEncoder}

package object config {
  import io.viash.helpers.circe.DeriveConfiguredDecoderWithDeprecationCheck._

  implicit val customConfig: Configuration = Configuration.default.withDefaults

  // encoders and decoders for Config
  implicit val encodeConfig: Encoder.AsObject[Config] = deriveConfiguredEncoder
  implicit val decodeConfig: Decoder[Config] = deriveConfiguredDecoderWithDeprecationCheck

  implicit val encodeInfo: Encoder[Info] = deriveConfiguredEncoder
  implicit val decodeInfo: Decoder[Info] = deriveConfiguredDecoderWithDeprecationCheck
}
