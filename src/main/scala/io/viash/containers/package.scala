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
import cats.syntax.functor._ // for .widen

package object containers {
  import io.viash.helpers.circe._
  import io.viash.helpers.circe.DeriveConfiguredDecoderFullChecks._

  implicit val encodeDockerContainer: Encoder.AsObject[DockerContainer] = deriveConfiguredEncoder
  implicit val decodeDockerContainer: Decoder[DockerContainer] = deriveConfiguredDecoderFullChecks

  implicit val encodeNativeContainer: Encoder.AsObject[NativeContainer] = deriveConfiguredEncoder
  implicit val decodeNativeContainer: Decoder[NativeContainer] = deriveConfiguredDecoderFullChecks

  implicit def encodeContainer[A <: Container]: Encoder[A] = Encoder.instance {
    container =>
//       val typeJson = Json.obj("type" -> Json.fromString(container.`type`))
      val objJson = container match {
        case s: DockerContainer => encodeDockerContainer(s)
//         case s: NativeContainer => encodeNativeContainer(s)
      }
      objJson
//       objJson deepMerge typeJson
  }

  implicit def decodeContainer: Decoder[Container] = Decoder.instance {
    cursor =>
      val decoder: Decoder[Container] =
        decodeDockerContainer.widen
//         cursor.downField("type").as[String] match {
//           case Right("docker") => decodeDockerContainer.widen
//           case Right("native") => decodeNativeContainer.widen
//           case Right(typ) => 
//             DeriveConfiguredDecoderWithValidationCheck.invalidSubTypeDecoder[NativeContainer](typ, List("docker", "native")).widen
//           case Left(exception) => throw exception
//         }

      decoder(cursor)
  }
}
