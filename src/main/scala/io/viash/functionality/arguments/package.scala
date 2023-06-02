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
import cats.syntax.functor._ // for .widen
import io.viash.helpers.circe.DeriveConfiguredDecoderWithValidationCheck._
import io.viash.config.ConfigParserSubTypeException

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
        case Right("+inf" | "+infinity" | "positiveinfinity" | "positiveinf") => Right(Double.PositiveInfinity)
        case Right("-inf" | "-infinity" | "negativeinfinity" | "negativeinf") => Right(Double.NegativeInfinity)
        case Right("nan") => Right(Double.NaN)
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
  implicit val encodeStringArgument: Encoder.AsObject[StringArgument] = deriveConfiguredEncoder
  implicit val encodeIntegerArgument: Encoder.AsObject[IntegerArgument] = deriveConfiguredEncoder
  implicit val encodeLongArgument: Encoder.AsObject[LongArgument] = deriveConfiguredEncoder
  implicit val encodeDoubleArgument: Encoder.AsObject[DoubleArgument] = deriveConfiguredEncoder
  implicit val encodeBooleanArgumentR: Encoder.AsObject[BooleanArgument] = deriveConfiguredEncoder
  implicit val encodeBooleanArgumentT: Encoder.AsObject[BooleanTrueArgument] = deriveConfiguredEncoder
  implicit val encodeBooleanArgumentF: Encoder.AsObject[BooleanFalseArgument] = deriveConfiguredEncoder
  implicit val encodeFileArgument: Encoder.AsObject[FileArgument] = deriveConfiguredEncoder

  implicit def encodeArgument[A <: Argument[_]]: Encoder[A] = Encoder.instance {
    par =>
      val typeJson = Json.obj("type" -> Json.fromString(par.`type`))
      val objJson = par match {
        case s: StringArgument => encodeStringArgument(s)
        case s: IntegerArgument => encodeIntegerArgument(s)
        case s: LongArgument => encodeLongArgument(s)
        case s: DoubleArgument => encodeDoubleArgument(s)
        case s: BooleanArgument => encodeBooleanArgumentR(s)
        case s: BooleanTrueArgument => encodeBooleanArgumentT(s)
        case s: BooleanFalseArgument => encodeBooleanArgumentF(s)
        case s: FileArgument => encodeFileArgument(s)
      }
      objJson deepMerge typeJson
  }

  implicit val decodeStringArgument: Decoder[StringArgument] = deriveConfiguredDecoderWithValidationCheck
  implicit val decodeIntegerArgument: Decoder[IntegerArgument] = deriveConfiguredDecoderWithValidationCheck
  implicit val decodeLongArgument: Decoder[LongArgument] = deriveConfiguredDecoderWithValidationCheck
  implicit val decodeDoubleArgument: Decoder[DoubleArgument] = deriveConfiguredDecoderWithValidationCheck
  implicit val decodeBooleanArgumentR: Decoder[BooleanArgument] = deriveConfiguredDecoderWithValidationCheck
  implicit val decodeBooleanArgumentT: Decoder[BooleanTrueArgument] = deriveConfiguredDecoderWithValidationCheck
  implicit val decodeBooleanArgumentF: Decoder[BooleanFalseArgument] = deriveConfiguredDecoderWithValidationCheck
  implicit val decodeFileArgument: Decoder[FileArgument] = deriveConfiguredDecoderWithValidationCheck

  implicit def decodeDataArgument: Decoder[Argument[_]] = Decoder.instance {
    cursor =>
      val decoder: Decoder[Argument[_]] =
        cursor.downField("type").as[String] match {
          case Right("string") => decodeStringArgument.widen
          case Right("integer") => decodeIntegerArgument.widen
          case Right("long") => decodeLongArgument.widen
          case Right("double") => decodeDoubleArgument.widen
          case Right("boolean") => decodeBooleanArgumentR.widen
          case Right("boolean_true") => decodeBooleanArgumentT.widen
          case Right("boolean_false") => decodeBooleanArgumentF.widen
          case Right("file") => decodeFileArgument.widen
          // case Right(typ) => throw new ConfigParserSubTypeException("Type " + typ + " is not recognised. Valid types are string, integer, long, double, boolean, boolean_true, boolean_false and file.")
          case Right(typ) => invalidSubTypeDecoder[StringArgument](typ, "string, integer, long, double, boolean, boolean_true, boolean_false and file").widen
          case Left(exception) => throw exception
        }

      decoder(cursor)
  }
}
