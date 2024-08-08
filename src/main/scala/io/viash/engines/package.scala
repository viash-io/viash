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
// import io.circe.generic.extras.semiauto.{deriveConfiguredDecoder, deriveConfiguredEncoder}
import cats.syntax.functor._
// for .widen

package object engines {
  import io.viash.helpers.circe._
  import io.viash.helpers.circe.DeriveConfiguredDecoderFullChecks._

  // implicit val encodeDockerEngine: Encoder.AsObject[DockerEngine] = deriveConfiguredEncoder
  // implicit val decodeDockerEngine: Decoder[DockerEngine] = deriveConfiguredDecoderFullChecks

  // implicit val encodeNativeEngine: Encoder.AsObject[NativeEngine] = deriveConfiguredEncoder
  // implicit val decodeNativeEngine: Decoder[NativeEngine] = deriveConfiguredDecoderFullChecks

  // implicit def encodeEngine[A <: Engine]: Encoder[A] = Encoder.instance {
  //   engine =>
  //     val typeJson = Json.obj("type" -> Json.fromString(engine.`type`))
  //     val objJson = engine match {
  //       case s: DockerEngine => encodeDockerEngine(s)
  //       case s: NativeEngine => encodeNativeEngine(s)
  //     }
  //     objJson deepMerge typeJson
  // }

  // implicit def decodeEngine: Decoder[Engine] = Decoder.instance {
  //   cursor =>
  //     val decoder: Decoder[Engine] =
  //       cursor.downField("type").as[String] match {
  //         case Right("docker") => decodeDockerEngine.widen
  //         case Right("native") => decodeNativeEngine.widen
  //         case Right(typ) => 
  //           DeriveConfiguredDecoderWithValidationCheck.invalidSubTypeDecoder[NativeEngine](typ, List("docker", "native")).widen
  //         case Left(exception) => throw exception
  //       }

  //     decoder(cursor)
  // }
}
