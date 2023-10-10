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

package io.viash.platforms

import io.circe.{Decoder, Encoder, Json}
import io.circe.generic.extras.semiauto.{deriveConfiguredDecoder, deriveConfiguredEncoder}

package object nextflow {
  import io.viash.helpers.circe._
  import io.viash.helpers.circe.DeriveConfiguredDecoderFullChecks._

  implicit val encodeNextflowDirectives: Encoder.AsObject[NextflowDirectives] = deriveConfiguredEncoder
  implicit val decodeNextflowDirectives: Decoder[NextflowDirectives] = deriveConfiguredDecoderFullChecks

  implicit val encodeNextflowAuto: Encoder.AsObject[NextflowAuto] = deriveConfiguredEncoder
  implicit val decodeNextflowAuto: Decoder[NextflowAuto] = deriveConfiguredDecoderFullChecks[NextflowAuto].prepare(
    _.withFocus(_.mapObject{ conf => 
      conf.map {
        case ("publish", Json.True) => ("publish", Json.fromString("true"))
        case ("publish", Json.False) => ("publish", Json.fromString("false"))
        case (k, v) => (k, v)
      }})
  )

  implicit val encodeNextflowConfig: Encoder.AsObject[NextflowConfig] = deriveConfiguredEncoder
  implicit val decodeNextflowConfig: Decoder[NextflowConfig] = deriveConfiguredDecoderFullChecks
}
