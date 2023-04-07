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

package io.viash.platforms

import io.circe.{Decoder, Encoder, Json}
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.{deriveConfiguredDecoder, deriveConfiguredEncoder}
import cats.syntax.functor._ // for .widen


package object requirements {
  import io.viash.helpers.circe._
  import io.viash.helpers.circe.DeriveConfiguredDecoderWithDeprecationCheck._

  implicit val encodeRRequirements: Encoder.AsObject[RRequirements] = deriveConfiguredEncoder
  implicit val decodeRRequirements: Decoder[RRequirements] = deriveConfiguredDecoderWithDeprecationCheck

  implicit val encodePythonRequirements: Encoder.AsObject[PythonRequirements] = deriveConfiguredEncoder
  implicit val decodePythonRequirements: Decoder[PythonRequirements] = deriveConfiguredDecoderWithDeprecationCheck

  implicit val encodeRubyRequirements: Encoder.AsObject[RubyRequirements] = deriveConfiguredEncoder
  implicit val decodeRubyRequirements: Decoder[RubyRequirements] = deriveConfiguredDecoderWithDeprecationCheck

  implicit val encodeJavaScriptRequirements: Encoder.AsObject[JavaScriptRequirements] = deriveConfiguredEncoder
  implicit val decodeJavaScriptRequirements: Decoder[JavaScriptRequirements] = deriveConfiguredDecoderWithDeprecationCheck

  implicit val encodeAptRequirements: Encoder.AsObject[AptRequirements] = deriveConfiguredEncoder
  implicit val decodeAptRequirements: Decoder[AptRequirements] = deriveConfiguredDecoderWithDeprecationCheck

  implicit val encodeYumRequirements: Encoder.AsObject[YumRequirements] = deriveConfiguredEncoder
  implicit val decodeYumRequirements: Decoder[YumRequirements] = deriveConfiguredDecoderWithDeprecationCheck

  implicit val encodeApkRequirements: Encoder.AsObject[ApkRequirements] = deriveConfiguredEncoder
  implicit val decodeApkRequirements: Decoder[ApkRequirements] = deriveConfiguredDecoderWithDeprecationCheck

  implicit val encodeDockerRequirements: Encoder.AsObject[DockerRequirements] = deriveConfiguredEncoder
  implicit val decodeDockerRequirements: Decoder[DockerRequirements] = deriveConfiguredDecoderWithDeprecationCheck

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
          case Right(typ) => throw new RuntimeException("Type " + typ + " is not recognised.")
          case Left(exception) => throw exception
        }

      decoder(cursor)
  }
}