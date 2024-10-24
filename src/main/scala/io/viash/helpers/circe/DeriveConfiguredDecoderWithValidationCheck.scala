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

package io.viash.helpers.circe

import io.circe.{ Decoder, CursorOp, HCursor, DecodingFailure }
import io.circe.derivation.{Configuration, ConfiguredDecoder}
import scala.deriving.Mirror

import io.viash.exceptions.ConfigParserSubTypeException
import io.viash.exceptions.ConfigParserValidationException
import io.viash.helpers.{typeOf, fieldsOf}

object DeriveConfiguredDecoderWithValidationCheck {

  private def validator_static_error(pred: HCursor, validFields: List[String], typeOf: String)(error: DecodingFailure): Boolean = {
    val usedFields = pred.value.asObject.map(_.keys.toSeq)
    val invalidFields = usedFields.map(_.diff(validFields))

    val fieldsHint = invalidFields match {
      case Some(a) if a.length > 1 => Some(s"Unexpected fields: ${a.mkString(", ")}")
      case Some(a) if a.length == 1 => Some(s"Unexpected field: ${a.head}")
      case _ => None
    }

    val historyString = error.history.collect{ case df: CursorOp.DownField => df.k }.reverse.mkString(".")

    val hint = (fieldsHint, historyString, error.message) match {
      case (Some(a), h, _) if h != "" => Some(s".$h -> $a")
      case (Some(a), _, _) => Some(a)
      case (None, h, m) if h != "" => Some(s".$h -> $m")
      case _ => None
    }

    throw new ConfigParserValidationException(typeOf, pred.value.toString(), hint)
  }

  // Validate the json can correctly converted to the required type by actually converting it.
  // Throw an exception when the conversion fails.
  inline def validator[A](pred: HCursor)(using inline A: Mirror.Of[A], inline configuration: Configuration): Boolean = {     
    val d = deriveConfiguredDecoder[A]
    // val v = d(pred)
    // TODO not entirely sure why this is needed instead of just doing `val v = d(pred)`
    // goes wrong when decoding empty PackageConfig
    val v = pred match {
      case pred if pred.value.isNull => Right(null.asInstanceOf[A])
      case _ => d(pred)
    }

    v.fold(
      validator_static_error(pred, fieldsOf[A], typeOf[A]),
      _ => true)
  }

  // Attempts to convert the json to the desired class. Throw an exception if the conversion fails.
  inline def deriveConfiguredDecoderWithValidationCheck[A](using inline A: Mirror.Of[A], inline configuration: Configuration) = deriveConfiguredDecoder[A]
    .validate(
      validator[A],
      s"Could not convert json to ${typeOf[A]}."
    )

  // Dummy decoder to generate exceptions when an invalid type is specified
  // We need a valid class type to be specified
  inline def invalidSubTypeDecoder[A](tpe: String, validTypes: List[String])(using inline A: Mirror.Of[A], inline configuration: Configuration): Decoder[A] = deriveConfiguredDecoder[A]
    .validate(
      pred => {
        throw new ConfigParserSubTypeException(tpe, validTypes, pred.value.toString())
        false
      },
      s"Type '$tpe 'is not recognised. Valid types are ${validTypes.mkString("'", "', '", ",")}."
    )

}
