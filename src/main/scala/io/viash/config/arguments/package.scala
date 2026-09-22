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
import cats.syntax.functor._ // for .widen
import io.viash.helpers.circe.DeriveConfiguredDecoderWithValidationCheck.invalidSubTypeDecoder
import io.viash.exceptions.ConfigParserSubTypeException
import io.viash.helpers.circe.DeriveConfiguredSumType
import io.viash.helpers.circe.DeriveConfiguredSumType.branch

package object arguments {

  import io.viash.helpers.circe._

  implicit val encodeDouble: Encoder[Double] = Encoder.instance {
      value => 
        if (value.isPosInfinity) {
          Json.fromString("+Infinity")
        } else {
          Json.fromDoubleOrString(value)
        }
    }
  implicit val decodeDouble: Decoder[Double] = 
    io.circe.Decoder.decodeDouble or
    Decoder.instance {
      cursor => cursor.value.as[String].map(_.toLowerCase()) match {
        case Right(".inf" | "+.inf" | "+inf" | "+infinity" | "positiveinfinity" | "positiveinf") => Right(Double.PositiveInfinity)
        case Right("-.inf" | "-inf" | "-infinity" | "negativeinfinity" | "negativeinf") => Right(Double.NegativeInfinity)
        case Right(".nan" | "nan") => Right(Double.NaN)
        case a => a.map(_.toDouble)
      }
    }

  // encoder and decoder for direction
  implicit val encodeDirection: Encoder[Direction] = Encoder.instance {
    dir => Json.fromString(dir.toString.toLowerCase())
  }
  implicit val decodeDirection: Decoder[Direction] = Decoder.instance {
    cursor =>
      cursor.value.as[String].map(s =>
        s.toLowerCase() match {
          case "input" => Input
          case "output" => Output
        }
      )
  }

  // encoders and decoders for Argument
  implicit val encodeStringArgument: Encoder.AsObject[StringArgument] = deriveConfiguredEncoderStrict[StringArgument]
  implicit val encodeIntegerArgument: Encoder.AsObject[IntegerArgument] = deriveConfiguredEncoderStrict[IntegerArgument]
  implicit val encodeLongArgument: Encoder.AsObject[LongArgument] = deriveConfiguredEncoderStrict[LongArgument]
  implicit val encodeDoubleArgument: Encoder.AsObject[DoubleArgument] = deriveConfiguredEncoderStrict[DoubleArgument]
  implicit val encodeBooleanArgumentR: Encoder.AsObject[BooleanArgument] = deriveConfiguredEncoderStrict[BooleanArgument]
  implicit val encodeBooleanArgumentT: Encoder.AsObject[BooleanTrueArgument] = deriveConfiguredEncoderStrict[BooleanTrueArgument]
  implicit val encodeBooleanArgumentF: Encoder.AsObject[BooleanFalseArgument] = deriveConfiguredEncoderStrict[BooleanFalseArgument]
  implicit val encodeFileArgument: Encoder.AsObject[FileArgument] = deriveConfiguredEncoderStrict[FileArgument]

  implicit val decodeStringArgument: Decoder[StringArgument] = deriveConfiguredDecoderFullChecks
  implicit val decodeIntegerArgument: Decoder[IntegerArgument] = deriveConfiguredDecoderFullChecks
  implicit val decodeLongArgument: Decoder[LongArgument] = deriveConfiguredDecoderFullChecks
  implicit val decodeDoubleArgument: Decoder[DoubleArgument] = deriveConfiguredDecoderFullChecks
  implicit val decodeBooleanArgumentR: Decoder[BooleanArgument] = deriveConfiguredDecoderFullChecks
  implicit val decodeBooleanArgumentT: Decoder[BooleanTrueArgument] = deriveConfiguredDecoderFullChecks
  implicit val decodeBooleanArgumentF: Decoder[BooleanFalseArgument] = deriveConfiguredDecoderFullChecks
  implicit val decodeFileArgument: Decoder[FileArgument] = deriveConfiguredDecoderFullChecks

  // must come after the individual encode*/decode* vals above: each branch() call resolves them
  // implicitly, and package object vals initialize in textual order
  private val argumentBranches: List[DeriveConfiguredSumType.Branch[Argument[_]]] = List(
    branch[Argument[_], StringArgument]("string"),
    branch[Argument[_], IntegerArgument]("integer"),
    branch[Argument[_], LongArgument]("long"),
    branch[Argument[_], DoubleArgument]("double"),
    branch[Argument[_], BooleanArgument]("boolean"),
    branch[Argument[_], BooleanTrueArgument]("boolean_true"),
    branch[Argument[_], BooleanFalseArgument]("boolean_false"),
    branch[Argument[_], FileArgument]("file"),
  )

  private val argumentEncoder: Encoder.AsObject[Argument[_]] = DeriveConfiguredSumType.encoder(argumentBranches)
  // Argument is parameterized (Argument[String], Argument[Boolean], ...), so callers need an
  // Encoder for the concrete parameterized type; a fixed Encoder[Argument[_]] doesn't satisfy that
  // via implicit search the way it would for a non-parameterized hierarchy like Resource.
  implicit def encodeArgument[A <: Argument[_]]: Encoder[A] = argumentEncoder.asInstanceOf[Encoder[A]]

  implicit val decodeDataArgument: Decoder[Argument[_]] = DeriveConfiguredSumType.decoder[Argument[_]](
    "type",
    argumentBranches,
    whenInvalid = (typ, validTypes) => invalidSubTypeDecoder[StringArgument](typ, validTypes).widen
  )
}
