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

import io.circe.generic.extras.semiauto.{deriveConfiguredDecoder, deriveConfiguredEncoder}
import io.circe.{Decoder, Encoder, Json}
import io.circe.ACursor

package object functionality {
  // import implicits

  import functionality.arguments._
  import functionality.resources._
  import functionality.Status._
  import io.viash.helpers.circe._
  import io.viash.helpers.circe.DeriveConfiguredDecoderWithDeprecationCheck._

  // encoder and decoder for Functionality
  implicit val encodeFunctionality: Encoder.AsObject[Functionality] = deriveConfiguredEncoder

  // add file & direction defaults for inputs & outputs
  implicit val decodeFunctionality: Decoder[Functionality] = deriveConfiguredDecoderWithDeprecationCheck

  // encoder and decoder for Author
  implicit val encodeAuthor: Encoder.AsObject[Author] = deriveConfiguredEncoder
  implicit val decodeAuthor: Decoder[Author] = deriveConfiguredDecoderWithDeprecationCheck

  // encoder and decoder for Requirements
  implicit val encodeComputationalRequirements: Encoder.AsObject[ComputationalRequirements] = deriveConfiguredEncoder
  implicit val decodeComputationalRequirements: Decoder[ComputationalRequirements] = deriveConfiguredDecoderWithDeprecationCheck
  
  // encoder and decoder for ArgumentGroup
  implicit val encodeArgumentGroup: Encoder.AsObject[ArgumentGroup] = deriveConfiguredEncoder
  implicit val decodeArgumentGroup: Decoder[ArgumentGroup] = deriveConfiguredDecoderWithDeprecationCheck

  // encoder and decoder for Status, make string lowercase before decoding
  implicit val encodeStatus: Encoder[Status] = Encoder.encodeEnumeration(Status)
  implicit val decodeStatus: Decoder[Status] = Decoder.decodeEnumeration(Status).prepare {
    _.withFocus(_.mapString(_.toLowerCase()))
  }

}
