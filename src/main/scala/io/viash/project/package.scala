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
import io.circe.generic.extras.semiauto.{deriveConfiguredDecoder, deriveConfiguredEncoder}

package object project {
  import io.viash.helpers.circe._
  import io.viash.helpers.circe.DeriveConfiguredDecoderFullChecks._

  implicit val encodeViashProjectLinks: Encoder.AsObject[ViashProjectLinks] = deriveConfiguredEncoder
  implicit val decodeViashProjectLinks: Decoder[ViashProjectLinks] = deriveConfiguredDecoderFullChecks

  implicit val encodeViashProjectReferences: Encoder.AsObject[ViashProjectReferences] = deriveConfiguredEncoder
  implicit val decodeViashProjectReferences: Decoder[ViashProjectReferences] = deriveConfiguredDecoderFullChecks

  implicit val encodeViashProject: Encoder.AsObject[ViashProject] = deriveConfiguredEncoder
  implicit val decodeViashProject: Decoder[ViashProject] = deriveConfiguredDecoderFullChecks
}
