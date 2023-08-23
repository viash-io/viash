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

package object executors {
  import io.viash.helpers.circe._
  import io.viash.helpers.circe.DeriveConfiguredDecoderFullChecks._

  implicit val encodeExecutableExecutor: Encoder.AsObject[ExecutableExecutor] = deriveConfiguredEncoder
  implicit val decodeExecutableExecutor: Decoder[ExecutableExecutor] = deriveConfiguredDecoderFullChecks

  implicit val encodeNextflowExecutor: Encoder.AsObject[NextflowExecutor] = deriveConfiguredEncoder
  implicit val decodeNextflowExecutor: Decoder[NextflowExecutor] = deriveConfiguredDecoderFullChecks

  implicit def encodePlatform[A <: Executor]: Encoder[A] = Encoder.instance {
    platform =>
      val typeJson = Json.obj("type" -> Json.fromString(platform.`type`))
      val objJson = platform match {
        case s: ExecutableExecutor => encodeExecutableExecutor(s)
        case s: NextflowExecutor => encodeNextflowExecutor(s)
      }
      objJson deepMerge typeJson
  }

  implicit def decodePlatform: Decoder[Executor] = Decoder.instance {
    cursor =>
      val decoder: Decoder[Executor] =
        cursor.downField("type").as[String] match {
          case Right("executable") => decodeExecutableExecutor.widen
          case Right("nextflow") => decodeNextflowExecutor.widen
          case Right(typ) => 
            DeriveConfiguredDecoderWithValidationCheck.invalidSubTypeDecoder[ExecutableExecutor](typ, List("executable", "nextflow")).widen
          case Left(exception) => throw exception
        }

      decoder(cursor)
  }
}
