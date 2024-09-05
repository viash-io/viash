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
import cats.syntax.functor._ // for .widen

package object runners {
  import io.viash.helpers.circe._

  import io.viash.runners.executable.{decodeSetupStrategy, encodeSetupStrategy}
  import io.viash.runners.nextflow.{decodeNextflowDirectives, encodeNextflowDirectives}
  import io.viash.runners.nextflow.{decodeNextflowAuto, encodeNextflowAuto}
  import io.viash.runners.nextflow.{decodeNextflowConfig, encodeNextflowConfig}

  implicit val encodeExecutableRunner: Encoder.AsObject[ExecutableRunner] = deriveConfiguredEncoder
  implicit val decodeExecutableRunner: Decoder[ExecutableRunner] = deriveConfiguredDecoderFullChecks

  implicit val encodeNextflowRunner: Encoder.AsObject[NextflowRunner] = deriveConfiguredEncoder
  implicit val decodeNextflowRunner: Decoder[NextflowRunner] = deriveConfiguredDecoderFullChecks

  implicit def encodeRunner[A <: Runner]: Encoder[A] = Encoder.instance {
    runner =>
      val typeJson = Json.obj("type" -> Json.fromString(runner.`type`))
      val objJson = runner match {
        case s: ExecutableRunner => encodeExecutableRunner(s)
        case s: NextflowRunner => encodeNextflowRunner(s)
      }
      objJson deepMerge typeJson
  }

  implicit def decodeRunner: Decoder[Runner] = Decoder.instance {
    cursor =>
      val decoder: Decoder[Runner] =
        cursor.downField("type").as[String] match {
          case Right("executable") => decodeExecutableRunner.widen
          case Right("nextflow") => decodeNextflowRunner.widen
          case Right(typ) => 
            DeriveConfiguredDecoderWithValidationCheck.invalidSubTypeDecoder[ExecutableRunner](typ, List("executable", "nextflow")).widen
          case Left(exception) => throw exception
        }

      decoder(cursor)
  }
}
