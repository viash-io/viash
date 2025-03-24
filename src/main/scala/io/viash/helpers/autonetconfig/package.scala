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

package io.viash.helpers

import io.circe.{Decoder, Encoder}
import io.circe.derivation.{ConfiguredEnumEncoder, ConfiguredEnumDecoder}

package object autonetconfig {
  import io.viash.helpers.circe._

  implicit val encodeAutoNetConfig: Encoder.AsObject[AutoNetConfig] = deriveConfiguredEncoder
  implicit val decodeAutoNetConfig: Decoder[AutoNetConfig] = deriveConfiguredDecoder

  implicit val encodeHostsStruct: Encoder.AsObject[Hosts] = deriveConfiguredEncoder
  implicit val decodeHostsStruct: Decoder[Hosts] = deriveConfiguredDecoder

  implicit val encodeValidationStruct: Encoder.AsObject[Validation] = deriveConfiguredEncoder
  implicit val decodeValidationStruct: Decoder[Validation] = deriveConfiguredDecoder

  implicit val encodeProtocol: Encoder[Protocol] = ConfiguredEnumEncoder.derive(_.toLowerCase())
  implicit val decodeProtocol: Decoder[Protocol] = ConfiguredEnumDecoder.derive[Protocol](_.toLowerCase()).prepare {
    _.withFocus(_.mapString(_.toLowerCase()))
  }

  implicit val encodeSourcesType: Encoder[SourcesType] = ConfiguredEnumEncoder.derive(_.toLowerCase())
  implicit val decodeSourcesType: Decoder[SourcesType] = ConfiguredEnumDecoder.derive[SourcesType](_.toLowerCase()).prepare {
    _.withFocus(_.mapString(_.toLowerCase()))
  }
}
