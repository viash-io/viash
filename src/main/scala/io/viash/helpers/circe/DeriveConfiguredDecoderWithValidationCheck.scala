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

import io.circe.generic.extras.semiauto.deriveConfiguredDecoder
import io.circe.{ Decoder, CursorOp }
import io.circe.generic.extras.decoding.ConfiguredDecoder

import scala.reflect.runtime.universe._
import shapeless.Lazy

import io.viash.schemas.ParameterSchema
import io.circe.ACursor
import io.viash.config.ConfigParserSubTypeException
import io.viash.config.ConfigParserValidationException

object DeriveConfiguredDecoderWithValidationCheck {

  // final def validate(pred: HCursor => Boolean, message: => String): Decoder[A] = validate
  def deriveConfiguredDecoderWithValidationCheck[A](implicit decode: Lazy[ConfiguredDecoder[A]], tag: TypeTag[A]): Decoder[A] = deriveConfiguredDecoder[A]
    .validate(
      pred => {
        val d = deriveConfiguredDecoder[A]
        val v = d(pred)

        v.fold(_ => {
          throw new ConfigParserValidationException(typeOf[A].baseClasses.head.fullName, pred.value.toString())
          false
        }, _ => true)
      },
      s"Could not convert json to ${typeOf[A].baseClasses.head.fullName.split(".").last}."
    )

  def invalidSubTypeDecoder[A](tpe: String, validTypes: String)(implicit decode: Lazy[ConfiguredDecoder[A]], tag: TypeTag[A]): Decoder[A] = deriveConfiguredDecoder[A]
    .validate(
      pred => {
        // Console.println(s"Pred Invalid SubType $tpe. ${pred.value}.")
        throw new ConfigParserSubTypeException(tpe, validTypes, pred.value.toString())
        false
      },
      s"Type $tpe is not recognised. Valid types are $validTypes."
    )

}
