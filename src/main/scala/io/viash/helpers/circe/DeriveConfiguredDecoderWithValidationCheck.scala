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

object DeriveConfiguredDecoderWithValidationCheck {

  // final def validate(pred: HCursor => Boolean, message: => String): Decoder[A] = validate
  def deriveConfiguredDecoderWithValidationCheck[A](implicit decode: Lazy[ConfiguredDecoder[A]], tag: TypeTag[A]): Decoder[A] = deriveConfiguredDecoder[A]
    .validate(
      pred => {
        val d = deriveConfiguredDecoder[A]
        val v = d(pred)
        
        val res = v.fold(_ => false, _ => true)
        Console.println(s"Testing parsing of ${pred.value} -> $res")
        res
      },
      s"Big booboo for ${typeOf[A].baseClasses.head.fullName}"
    )

  def invalidSubTypeDecoder[A](tpe: String, validTypes: String)(implicit decode: Lazy[ConfiguredDecoder[A]], tag: TypeTag[A]): Decoder[A] = deriveConfiguredDecoder[A]
    .validate(_ => false, s"Type $tpe is not recognised. Valid types are $validTypes.")

}
