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
import io.viash.helpers.circe.DeriveConfiguredSumType
import io.viash.helpers.circe.DeriveConfiguredSumType.branch

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

  // must come after the individual encode*/decode* vals above: each branch() call resolves them
  // implicitly, and package object vals initialize in textual order
  private val requirementsBranches: List[DeriveConfiguredSumType.Branch[Requirements]] = List(
    branch[Requirements, ApkRequirements]("apk"),
    branch[Requirements, AptRequirements]("apt"),
    branch[Requirements, YumRequirements]("yum"),
    branch[Requirements, DockerRequirements]("docker"),
    branch[Requirements, PythonRequirements]("python"),
    branch[Requirements, RRequirements]("r"),
    branch[Requirements, JavaScriptRequirements]("javascript"),
    branch[Requirements, RubyRequirements]("ruby"),
  )

  implicit val encodeRequirements: Encoder[Requirements] = DeriveConfiguredSumType.encoder(requirementsBranches)

  implicit val decodeRequirements: Decoder[Requirements] = DeriveConfiguredSumType.decoder[Requirements](
    "type",
    requirementsBranches,
    whenInvalid = (typ, validTypes) => DeriveConfiguredDecoderWithValidationCheck.invalidSubTypeDecoder[ApkRequirements](typ, validTypes).widen
  )
}
