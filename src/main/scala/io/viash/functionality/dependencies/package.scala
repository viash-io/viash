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

package io.viash.functionality

import io.circe.{Decoder, Encoder, Json}
import io.circe.generic.extras.semiauto.{deriveConfiguredDecoder, deriveConfiguredEncoder}
import io.viash.helpers.circe.DeriveConfiguredDecoderFullChecks._
import cats.syntax.functor._

package object dependencies {

  import io.viash.helpers.circe._
  import io.viash.helpers.circe.DeriveConfiguredEncoderStrict._

  // encoders and decoders for Argument
  implicit val encodeDependency: Encoder.AsObject[Dependency] = deriveConfiguredEncoderStrict
  implicit val encodeGitRepository: Encoder.AsObject[GitRepository] = deriveConfiguredEncoderStrict
  implicit val encodeGithubRepository: Encoder.AsObject[GithubRepository] = deriveConfiguredEncoderStrict
  implicit val encodeViashhubRepository: Encoder.AsObject[ViashhubRepository] = deriveConfiguredEncoderStrict
  implicit val encodeLocalRepository: Encoder.AsObject[LocalRepository] = deriveConfiguredEncoderStrict
  implicit def encodeRepository[A <: Repository]: Encoder[A] = Encoder.instance {
    par =>
      val typeJson = Json.obj("type" -> Json.fromString(par.`type`))
      val objJson = par match {
        case s: GitRepository => encodeGitRepository(s)
        case s: GithubRepository => encodeGithubRepository(s)
        case s: ViashhubRepository => encodeViashhubRepository(s)
        case s: LocalRepository => encodeLocalRepository(s)
      }
      objJson deepMerge typeJson
  }


  implicit val decodeDependency: Decoder[Dependency] = deriveConfiguredDecoderFullChecks
  implicit val decodeGitRepository: Decoder[GitRepository] = deriveConfiguredDecoderFullChecks
  implicit val decodeGithubRepository: Decoder[GithubRepository] = deriveConfiguredDecoderFullChecks
  implicit val decodeViashhubRepository: Decoder[ViashhubRepository] = deriveConfiguredDecoderFullChecks
  implicit val decodeLocalRepository: Decoder[LocalRepository] = deriveConfiguredDecoderFullChecks
  implicit def decodeRepository: Decoder[Repository] = Decoder.instance {
    cursor =>
      val decoder: Decoder[Repository] =
        cursor.downField("type").as[String] match {
          case Right("git") => decodeGitRepository.widen
          case Right("github") => decodeGithubRepository.widen
          case Right("vsh") => decodeViashhubRepository.widen
          case Right("local") => decodeLocalRepository.widen
          case Right(typ) =>
            DeriveConfiguredDecoderWithValidationCheck.invalidSubTypeDecoder[LocalRepository](typ, List("git", "github", "vsh", "local")).widen
          case Left(exception) => throw exception
        }

      decoder(cursor)
  }

}