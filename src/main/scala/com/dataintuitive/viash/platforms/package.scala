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

  implicit val encodeNextflowLegacyPlatform: Encoder.AsObject[NextflowLegacyPlatform] = deriveConfiguredEncoder
  implicit val decodeNextflowLegacyPlatform: Decoder[NextflowLegacyPlatform] = deriveConfiguredDecoder

  implicit val encodeNextflowNeoPlatform: Encoder.AsObject[NextflowNeoPlatform] = deriveConfiguredEncoder
  implicit val decodeNextflowNeoPlatform: Decoder[NextflowNeoPlatform] = deriveConfiguredDecoder

  implicit val encodeNativePlatform: Encoder.AsObject[NativePlatform] = deriveConfiguredEncoder
  implicit val decodeNativePlatform: Decoder[NativePlatform] = deriveConfiguredDecoder

  implicit def encodePlatform[A <: Platform]: Encoder[A] = Encoder.instance {
    platform =>
      val typeJson = Json.obj("type" → Json.fromString(platform.`type`))
      val objJson = platform match {
        case s: DockerPlatform => encodeDockerPlatform(s)
        case s: NextflowLegacyPlatform => encodeNextflowLegacyPlatform(s)
        case s: NextflowNeoPlatform => encodeNextflowNeoPlatform(s)
        case s: NativePlatform => encodeNativePlatform(s)
      }
      objJson deepMerge typeJson
  }

  implicit def decodeNextflowPlatform: Decoder[NextflowPlatform] = Decoder.instance {
    cursor =>
      val decoder: Decoder[NextflowPlatform] =
        cursor.downField("variant").as[String] match {
          case Right("legacy") => decodeNextflowLegacyPlatform.widen
          case Right("neo") => decodeNextflowNeoPlatform.widen
          case Right(typ) => throw new RuntimeException("Variant " + typ + " is not recognised.")
          case Left(exception) => decodeNextflowLegacyPlatform.widen // TODO: default is legacy, will be changed in Viash 1.0
        }

      decoder(cursor)
  }

  implicit def decodePlatform: Decoder[Platform] = Decoder.instance {
    cursor =>
      val decoder: Decoder[Platform] =
        cursor.downField("type").as[String] match {
          case Right("docker") => decodeDockerPlatform.widen
          case Right("native") => decodeNativePlatform.widen
          case Right("nextflow") => decodeNextflowPlatform.widen
          case Right(typ) => throw new RuntimeException("Type " + typ + " is not recognised.")
          case Left(exception) => throw exception
        }

      decoder(cursor)
  }
}
