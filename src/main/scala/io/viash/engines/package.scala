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
import cats.syntax.functor._
// for .widen
import io.viash.helpers.circe.DeriveConfiguredSumType
import io.viash.helpers.circe.DeriveConfiguredSumType.branch

package object engines {
  import io.viash.helpers.circe._
  import io.viash.engines.requirements.{decodeRequirements, encodeRequirements}

  implicit val encodeDockerEngine: Encoder.AsObject[DockerEngine] = deriveConfiguredEncoder
  implicit val decodeDockerEngine: Decoder[DockerEngine] = deriveConfiguredDecoderFullChecks

  implicit val encodeNativeEngine: Encoder.AsObject[NativeEngine] = deriveConfiguredEncoder
  implicit val decodeNativeEngine: Decoder[NativeEngine] = deriveConfiguredDecoderFullChecks

  // must come after the individual encode*/decode* vals above: each branch() call resolves them
  // implicitly, and package object vals initialize in textual order
  private val engineBranches: List[DeriveConfiguredSumType.Branch[Engine]] = List(
    branch[Engine, DockerEngine]("docker"),
    branch[Engine, NativeEngine]("native"),
  )

  implicit val encodeEngine: Encoder[Engine] = DeriveConfiguredSumType.encoder(engineBranches)

  implicit val decodeEngine: Decoder[Engine] = DeriveConfiguredSumType.decoder[Engine](
    "type",
    engineBranches,
    whenInvalid = (typ, validTypes) => DeriveConfiguredDecoderWithValidationCheck.invalidSubTypeDecoder[NativeEngine](typ, validTypes).widen
  )
}
