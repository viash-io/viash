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

package io.viash.runners

import io.circe.{Decoder, Encoder, Json}
import io.circe.derivation.ConfiguredDecoder
// import io.circe.generic.extras.semiauto.{deriveConfiguredDecoder, deriveConfiguredEncoder}

package object nextflow {
  import io.viash.helpers.circe._
  import io.viash.helpers.circe.DeriveConfiguredEncoder._
  import io.viash.helpers.circe.DeriveConfiguredDecoderFullChecks._

  // implicit val encodeEitherBooleanString: Encoder[Either[Boolean, String]] = Encoder.derived
  // implicit val decodeEitherBooleanString: Decoder[Either[Boolean, String]] = Decoder.derived
  // implicit val encodeEitherMapStringStringString: Encoder[Either[Map[String, String], String]] = Encoder.derived
  // implicit val decodeEitherMapStringStringString: Decoder[Either[Map[String, String], String]] = Decoder.derived
  // implicit val encodeEitherIntString: Encoder[Either[Int, String]] = Encoder.derived
  // implicit val decodeEitherIntString: Decoder[Either[Int, String]] = Decoder.derived
  // implicit val encodeEitherStringInt: Encoder[Either[String, Int]] = Encoder.derived
  // implicit val decodeEitherStringInt: Decoder[Either[String, Int]] = Decoder.derived
  // implicit val encodeEitherStringMapStringString: Encoder[Either[String, Map[String, String]]] = Encoder.derived
  // implicit val decodeEitherStringMapStringString: Decoder[Either[String, Map[String, String]]] = Decoder.derived


  implicit val encodeNextflowDirectives: Encoder.AsObject[NextflowDirectives] = deriveConfiguredEncoder
  implicit val decodeNextflowDirectives: Decoder[NextflowDirectives] = deriveConfiguredDecoderFullChecks

  implicit val encodeNextflowAuto: Encoder.AsObject[NextflowAuto] = deriveConfiguredEncoder
  implicit val decodeNextflowAuto: Decoder[NextflowAuto] = deriveConfiguredDecoderFullChecks

  implicit val encodeNextflowConfig: Encoder.AsObject[NextflowConfig] = deriveConfiguredEncoder
  implicit val decodeNextflowConfig: Decoder[NextflowConfig] = deriveConfiguredDecoderFullChecks
}
