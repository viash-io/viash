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

package com.dataintuitive.viash

import io.circe.{Decoder, Encoder, Json}
import io.circe.generic.extras.semiauto.{deriveConfiguredDecoder, deriveConfiguredEncoder}
import cats.syntax.functor._ // for .widen

package object platforms {
  import com.dataintuitive.viash.helpers.Circe._

  implicit val encodeDockerPlatform: Encoder.AsObject[DockerPlatform] = deriveConfiguredEncoder
  implicit val decodeDockerPlatform: Decoder[DockerPlatform] = deriveConfiguredDecoder

  implicit val encodeNextFlowPlatform: Encoder.AsObject[NextFlowPlatform] = deriveConfiguredEncoder
  implicit val decodeNextFlowPlatform: Decoder[NextFlowPlatform] = deriveConfiguredDecoder

  implicit val encodeNextflowPlatformPoc: Encoder.AsObject[NextflowPlatformPoc] = deriveConfiguredEncoder
  implicit val decodeNextflowPlatformPoc: Decoder[NextflowPlatformPoc] = deriveConfiguredDecoder

  implicit val encodeNativePlatform: Encoder.AsObject[NativePlatform] = deriveConfiguredEncoder
  implicit val decodeNativePlatform: Decoder[NativePlatform] = deriveConfiguredDecoder

  // there is no reason to parse debug platforms from yaml
  // implicit val encodeDebugPlatform: Encoder.AsObject[DebugPlatform] = deriveConfiguredEncoder
  // implicit val decodeDebugPlatform: Decoder[DebugPlatform] = deriveConfiguredDecoder

  implicit def encodePlatform[A <: Platform]: Encoder[A] = Encoder.instance {
    platform =>
      val typeJson = Json.obj("type" → Json.fromString(platform.oType))
      val objJson = platform match {
        case s: DockerPlatform => encodeDockerPlatform(s)
        case s: NextFlowPlatform => encodeNextFlowPlatform(s)
        case s: NextflowPlatformPoc => encodeNextflowPlatformPoc(s)
        case s: NativePlatform => encodeNativePlatform(s)
        // case s: DebugPlatform => encodeDebugPlatform(s)
      }
      objJson deepMerge typeJson
  }

  implicit def decodePlatform: Decoder[Platform] = Decoder.instance {
    cursor =>
      val decoder: Decoder[Platform] =
        cursor.downField("type").as[String] match {
          case Right("docker") => decodeDockerPlatform.widen
          case Right("native") => decodeNativePlatform.widen
          // case Right("debug") => decodeDebugPlatform.widen
          case Right("nextflow") => decodeNextFlowPlatform.widen
          case Right("nextflowpoc") => decodeNextflowPlatformPoc.widen
          case Right(typ) => throw new RuntimeException("Type " + typ + " is not recognised.")
          case Left(exception) => throw exception
        }

      decoder(cursor)
  }
}
