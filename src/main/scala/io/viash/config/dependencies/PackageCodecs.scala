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

// Note: this is a regular object (not a `package object`) even though the file is named
// `package.scala` for consistency with sibling packages. A `package object dependencies`
// would compile to a class literally named `package$`/`package`, which on case-insensitive
// filesystems (e.g. macOS, used in CI) collides with the `Package`/`Package$` class files
// generated for the `Package` class and its companion object defined in this same package.
package io.viash.config.dependencies

import io.circe.{ACursor, Decoder, Encoder, Json}
import cats.syntax.functor._
import io.viash.helpers.circe.DeriveConfiguredSumType
import io.viash.helpers.circe.DeriveConfiguredSumType.branch

object PackageCodecs {

  import io.viash.helpers.circe._
  import io.viash.helpers.circe.DeriveConfiguredDecoderWithDeprecationCheck.checkDeprecation
  import io.viash.helpers.circe.DeriveConfiguredDecoderWithValidationCheck.deriveConfiguredDecoderWithValidationCheck
  // ScopeEnum's codecs live in io.viash.config's package object; needed here since Dependency
  // has an (internal) field of type ScopeEnum.
  import io.viash.config.{encodeScopeEnum, decodeScopeEnum}

  // Backwards compatibility: 'repositories' was renamed to 'packages' (issue #847).
  // If both keys are present, values are merged: existing 'packages' entries first, then legacy 'repositories' entries appended.
  def renameRepositoriesToPackages(cursor: ACursor): ACursor = {
    cursor.withFocus(_.mapObject { jo =>
      (jo.apply("packages"), jo.apply("repositories")) match {
        case (_, None) => jo
        case (None, Some(old)) => jo.remove("repositories").add("packages", old)
        case (Some(neu), Some(old)) =>
          val merged = Json.fromValues(neu.asArray.getOrElse(Vector.empty) ++ old.asArray.getOrElse(Vector.empty))
          jo.remove("repositories").add("packages", merged)
      }
    })
  }

  // Backwards compatibility: the per-dependency 'repository' field was renamed to 'package' (issue #847).
  // If both are present, the new 'package' field wins and 'repository' is dropped.
  def renameRepositoryToPackageField(cursor: ACursor): ACursor = {
    cursor.withFocus(_.mapObject { jo =>
      (jo.apply("package"), jo.apply("repository")) match {
        case (_, None) => jo
        case (None, Some(old)) => jo.remove("repository").add("package", old)
        case (Some(_), Some(_)) => jo.remove("repository")
      }
    })
  }

  // encoders and decoders for Argument
  implicit val encodeDependency: Encoder.AsObject[Dependency] = deriveConfiguredEncoderStrict
  implicit val encodeGitPackage: Encoder.AsObject[GitPackage] = deriveConfiguredEncoderStrict
  implicit val encodeGithubPackage: Encoder.AsObject[GithubPackage] = deriveConfiguredEncoderStrict
  implicit val encodeViashhubPackage: Encoder.AsObject[ViashhubPackage] = deriveConfiguredEncoderStrict
  implicit val encodeLocalPackage: Encoder.AsObject[LocalPackage] = deriveConfiguredEncoderStrict

  implicit val encodeGitPackageWithName: Encoder.AsObject[GitPackageWithName] = deriveConfiguredEncoderStrict
  implicit val encodeGithubPackageWithName: Encoder.AsObject[GithubPackageWithName] = deriveConfiguredEncoderStrict
  implicit val encodeViashhubPackageWithName: Encoder.AsObject[ViashhubPackageWithName] = deriveConfiguredEncoderStrict
  implicit val encodeLocalPackageWithName: Encoder.AsObject[LocalPackageWithName] = deriveConfiguredEncoderStrict

  implicit val decodeDependency: Decoder[Dependency] = deriveConfiguredDecoderWithValidationCheck[Dependency]
    .prepare(renameRepositoryToPackageField)
    .prepare(checkDeprecation[Dependency](_))
  implicit val decodeGitPackage: Decoder[GitPackage] = deriveConfiguredDecoderFullChecks
  implicit val decodeGithubPackage: Decoder[GithubPackage] = deriveConfiguredDecoderFullChecks
  implicit val decodeViashhubPackage: Decoder[ViashhubPackage] = deriveConfiguredDecoderFullChecks
  implicit val decodeLocalPackage: Decoder[LocalPackage] = deriveConfiguredDecoderFullChecks

  implicit val decodeGitPackageWithName: Decoder[GitPackageWithName] = deriveConfiguredDecoderFullChecks
  implicit val decodeGithubPackageWithName: Decoder[GithubPackageWithName] = deriveConfiguredDecoderFullChecks
  implicit val decodeViashhubPackageWithName: Decoder[ViashhubPackageWithName] = deriveConfiguredDecoderFullChecks
  implicit val decodeLocalPackageWithName: Decoder[LocalPackageWithName] = deriveConfiguredDecoderFullChecks

  // must come after the individual encode*/decode* vals above: each branch() call resolves them
  // implicitly, and object vals initialize in textual order.
  //
  // Package's own 4 direct subtypes, used for decoding (decodePackage never produces a
  // _WithName instance) and as half of the encode branches (a _WithName instance is also a
  // Package at runtime, so encodePackage must be able to handle it too).
  private val packageOwnBranches: List[DeriveConfiguredSumType.Branch[Package]] = List(
    branch[Package, GitPackage]("git"),
    branch[Package, GithubPackage]("github"),
    branch[Package, ViashhubPackage]("vsh"),
    branch[Package, LocalPackage]("local"),
  )
  private val packageWithNameAsPackageBranches: List[DeriveConfiguredSumType.Branch[Package]] = List(
    branch[Package, GitPackageWithName]("git"),
    branch[Package, GithubPackageWithName]("github"),
    branch[Package, ViashhubPackageWithName]("vsh"),
    branch[Package, LocalPackageWithName]("local"),
  )

  implicit val encodePackage: Encoder[Package] =
    DeriveConfiguredSumType.encoder(packageOwnBranches ++ packageWithNameAsPackageBranches)

  implicit val decodePackage: Decoder[Package] = DeriveConfiguredSumType.decoder[Package](
    "type",
    packageOwnBranches,
    whenInvalid = (typ, validTypes) => DeriveConfiguredDecoderWithValidationCheck.invalidSubTypeDecoder[LocalPackage](typ, validTypes).widen
  )

  private val packageWithNameBranches: List[DeriveConfiguredSumType.Branch[PackageWithName]] = List(
    branch[PackageWithName, GitPackageWithName]("git"),
    branch[PackageWithName, GithubPackageWithName]("github"),
    branch[PackageWithName, ViashhubPackageWithName]("vsh"),
    branch[PackageWithName, LocalPackageWithName]("local"),
  )

  implicit val encodePackageWithName: Encoder[PackageWithName] = DeriveConfiguredSumType.encoder(packageWithNameBranches)

  implicit val decodePackageWithName: Decoder[PackageWithName] = DeriveConfiguredSumType.decoder[PackageWithName](
    "type",
    packageWithNameBranches,
    whenInvalid = (typ, validTypes) => DeriveConfiguredDecoderWithValidationCheck.invalidSubTypeDecoder[LocalPackageWithName](typ, validTypes).widen
  )
}
