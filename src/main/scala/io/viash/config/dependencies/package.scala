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

package io.viash.config

import io.circe.{Decoder, Encoder, Json}
import cats.syntax.functor._
import dependencies.GithubRepository
import io.viash.helpers.circe.DeriveConfiguredSumType
import io.viash.helpers.circe.DeriveConfiguredSumType.branch

package object dependencies {

  import io.viash.helpers.circe._

  // encoders and decoders for Argument
  implicit val encodeDependency: Encoder.AsObject[Dependency] = deriveConfiguredEncoderStrict
  implicit val encodeGitRepository: Encoder.AsObject[GitRepository] = deriveConfiguredEncoderStrict
  implicit val encodeGithubRepository: Encoder.AsObject[GithubRepository] = deriveConfiguredEncoderStrict
  implicit val encodeViashhubRepository: Encoder.AsObject[ViashhubRepository] = deriveConfiguredEncoderStrict
  implicit val encodeLocalRepository: Encoder.AsObject[LocalRepository] = deriveConfiguredEncoderStrict

  implicit val encodeGitRepositoryWithName: Encoder.AsObject[GitRepositoryWithName] = deriveConfiguredEncoderStrict
  implicit val encodeGithubRepositoryWithName: Encoder.AsObject[GithubRepositoryWithName] = deriveConfiguredEncoderStrict
  implicit val encodeViashhubRepositoryWithName: Encoder.AsObject[ViashhubRepositoryWithName] = deriveConfiguredEncoderStrict
  implicit val encodeLocalRepositoryWithName: Encoder.AsObject[LocalRepositoryWithName] = deriveConfiguredEncoderStrict

  implicit val decodeDependency: Decoder[Dependency] = deriveConfiguredDecoderFullChecks
  implicit val decodeGitRepository: Decoder[GitRepository] = deriveConfiguredDecoderFullChecks
  implicit val decodeGithubRepository: Decoder[GithubRepository] = deriveConfiguredDecoderFullChecks
  implicit val decodeViashhubRepository: Decoder[ViashhubRepository] = deriveConfiguredDecoderFullChecks
  implicit val decodeLocalRepository: Decoder[LocalRepository] = deriveConfiguredDecoderFullChecks

  implicit val decodeGitRepositoryWithName: Decoder[GitRepositoryWithName] = deriveConfiguredDecoderFullChecks
  implicit val decodeGithubRepositoryWithName: Decoder[GithubRepositoryWithName] = deriveConfiguredDecoderFullChecks
  implicit val decodeViashhubRepositoryWithName: Decoder[ViashhubRepositoryWithName] = deriveConfiguredDecoderFullChecks
  implicit val decodeLocalRepositoryWithName: Decoder[LocalRepositoryWithName] = deriveConfiguredDecoderFullChecks

  // must come after the individual encode*/decode* vals above: each branch() call resolves them
  // implicitly, and package object vals initialize in textual order.
  //
  // Repository's own 4 direct subtypes, used for decoding (decodeRepository never produces a
  // _WithName instance) and as half of the encode branches (a _WithName instance is also a
  // Repository at runtime, so encodeRepository must be able to handle it too).
  private val repositoryOwnBranches: List[DeriveConfiguredSumType.Branch[Repository]] = List(
    branch[Repository, GitRepository]("git"),
    branch[Repository, GithubRepository]("github"),
    branch[Repository, ViashhubRepository]("vsh"),
    branch[Repository, LocalRepository]("local"),
  )
  private val repositoryWithNameAsRepositoryBranches: List[DeriveConfiguredSumType.Branch[Repository]] = List(
    branch[Repository, GitRepositoryWithName]("git"),
    branch[Repository, GithubRepositoryWithName]("github"),
    branch[Repository, ViashhubRepositoryWithName]("vsh"),
    branch[Repository, LocalRepositoryWithName]("local"),
  )

  implicit val encodeRepository: Encoder[Repository] =
    DeriveConfiguredSumType.encoder(repositoryOwnBranches ++ repositoryWithNameAsRepositoryBranches)

  implicit val decodeRepository: Decoder[Repository] = DeriveConfiguredSumType.decoder[Repository](
    "type",
    repositoryOwnBranches,
    whenInvalid = (typ, validTypes) => DeriveConfiguredDecoderWithValidationCheck.invalidSubTypeDecoder[LocalRepository](typ, validTypes).widen
  )

  private val repositoryWithNameBranches: List[DeriveConfiguredSumType.Branch[RepositoryWithName]] = List(
    branch[RepositoryWithName, GitRepositoryWithName]("git"),
    branch[RepositoryWithName, GithubRepositoryWithName]("github"),
    branch[RepositoryWithName, ViashhubRepositoryWithName]("vsh"),
    branch[RepositoryWithName, LocalRepositoryWithName]("local"),
  )

  implicit val encodeRepositoryWithName: Encoder[RepositoryWithName] = DeriveConfiguredSumType.encoder(repositoryWithNameBranches)

  implicit val decodeRepositoryWithName: Decoder[RepositoryWithName] = DeriveConfiguredSumType.decoder[RepositoryWithName](
    "type",
    repositoryWithNameBranches,
    whenInvalid = (typ, validTypes) => DeriveConfiguredDecoderWithValidationCheck.invalidSubTypeDecoder[LocalRepositoryWithName](typ, validTypes).widen
  )
}
