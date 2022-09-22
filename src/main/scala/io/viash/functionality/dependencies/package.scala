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
import cats.syntax.functor._

package object dependencies {

  import io.viash.helpers.Circe._

  // encoders and decoders for Argument
  implicit val encodeDependency: Encoder.AsObject[Dependency] = deriveConfiguredEncoder
  implicit val encodeGithubRepository: Encoder.AsObject[GithubRepository] = deriveConfiguredEncoder
  implicit val encodeLocalRepository: Encoder.AsObject[LocalRepository] = deriveConfiguredEncoder
  implicit def encodeRepository[A <: Repository]: Encoder[A] = Encoder.instance {
    par =>
      val typeJson = Json.obj("type" → Json.fromString(par.`type`))
      val objJson = par match {
        case s: GithubRepository => encodeGithubRepository(s)
        case s: LocalRepository => encodeLocalRepository(s)
      }
      objJson deepMerge typeJson
  }


  implicit val decodeDependency: Decoder[Dependency] = deriveConfiguredDecoder
  implicit val decodeGithubRepository: Decoder[GithubRepository] = deriveConfiguredDecoder
  implicit val decodeLocalRepository: Decoder[LocalRepository] = deriveConfiguredDecoder
  implicit def decodeRepository: Decoder[Repository] = Decoder.instance {
    cursor =>
      val decoder: Decoder[Repository] =
        cursor.downField("type").as[String] match {
          case Right("string") => decodeGithubRepository.widen
          case Right("integer") => decodeLocalRepository.widen
          case Right(typ) => throw new RuntimeException("Type " + typ + " is not recognised. Valid types are github, ..., ... and local.")
          case Left(exception) => throw exception
        }

      decoder(cursor)
  }

}