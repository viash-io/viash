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

package io.viash.engines

import io.circe.{Decoder, Encoder, Json}
import cats.syntax.functor._ // for .widen

package object requirements {
  import io.viash.helpers.circe._

  implicit val encodeRRequirements: Encoder.AsObject[RRequirements] = deriveConfiguredEncoder
  implicit val decodeRRequirements: Decoder[RRequirements] = deriveConfiguredDecoderFullChecks

  implicit val encodePythonRequirements: Encoder.AsObject[PythonRequirements] = deriveConfiguredEncoder
  implicit val decodePythonRequirements: Decoder[PythonRequirements] = deriveConfiguredDecoderFullChecks

  implicit val encodeRubyRequirements: Encoder.AsObject[RubyRequirements] = deriveConfiguredEncoder
  implicit val decodeRubyRequirements: Decoder[RubyRequirements] = deriveConfiguredDecoderFullChecks

  implicit val encodeJavaScriptRequirements: Encoder.AsObject[JavaScriptRequirements] = deriveConfiguredEncoder
  implicit val decodeJavaScriptRequirements: Decoder[JavaScriptRequirements] = deriveConfiguredDecoderFullChecks

  implicit val encodeAptRequirements: Encoder.AsObject[AptRequirements] = deriveConfiguredEncoder
  implicit val decodeAptRequirements: Decoder[AptRequirements] = deriveConfiguredDecoderFullChecks

  implicit val encodeYumRequirements: Encoder.AsObject[YumRequirements] = deriveConfiguredEncoder
  implicit val decodeYumRequirements: Decoder[YumRequirements] = deriveConfiguredDecoderFullChecks

  implicit val encodeApkRequirements: Encoder.AsObject[ApkRequirements] = deriveConfiguredEncoder
  implicit val decodeApkRequirements: Decoder[ApkRequirements] = deriveConfiguredDecoderFullChecks

  implicit val encodeDockerRequirements: Encoder.AsObject[DockerRequirements] = deriveConfiguredEncoder
  implicit val decodeDockerRequirements: Decoder[DockerRequirements] = deriveConfiguredDecoderFullChecks

  implicit def encodeRequirements[A <: Requirements]: Encoder[A] = Encoder.instance {
    reqs =>
      val typeJson = Json.obj("type" -> Json.fromString(reqs.`type`))
      val objJson = reqs match {
        case s: ApkRequirements => encodeApkRequirements(s)
        case s: AptRequirements => encodeAptRequirements(s)
        case s: YumRequirements => encodeYumRequirements(s)
        case s: DockerRequirements => encodeDockerRequirements(s)
        case s: PythonRequirements => encodePythonRequirements(s)
        case s: RRequirements => encodeRRequirements(s)
        case s: JavaScriptRequirements => encodeJavaScriptRequirements(s)
        case s: RubyRequirements => encodeRubyRequirements(s)
      }
      objJson deepMerge typeJson
  }

  implicit def decodeRequirements: Decoder[Requirements] = Decoder.instance {
    cursor =>
      val decoder: Decoder[Requirements] =
        cursor.downField("type").as[String] match {
          case Right("apk") => decodeApkRequirements.widen
          case Right("apt") => decodeAptRequirements.widen
          case Right("yum") => decodeYumRequirements.widen
          case Right("docker") => decodeDockerRequirements.widen
          case Right("python") => decodePythonRequirements.widen
          case Right("r") => decodeRRequirements.widen
          case Right("javascript") => decodeJavaScriptRequirements.widen
          case Right("ruby") => decodeRubyRequirements.widen
          case Right(typ) =>
            DeriveConfiguredDecoderWithValidationCheck.invalidSubTypeDecoder[ApkRequirements](typ, List("apk", "apt", "yum", "docker", "python", "r", "javascript", "ruby")).widen
          case Left(exception) => throw exception
        }

      decoder(cursor)
  }
}
