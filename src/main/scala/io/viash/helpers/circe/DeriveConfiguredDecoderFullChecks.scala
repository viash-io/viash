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

import shapeless.Lazy
import scala.reflect.runtime.universe._

import io.circe.Decoder
import io.circe.generic.extras.decoding.ConfiguredDecoder
import io.circe.generic.extras.semiauto.deriveConfiguredDecoder

object DeriveConfiguredDecoderFullChecks {
  import io.viash.helpers.circe.DeriveConfiguredDecoderWithDeprecationCheck._
  import io.viash.helpers.circe.DeriveConfiguredDecoderWithValidationCheck._

  def deriveConfiguredDecoderFullChecks[A](implicit decode: Lazy[ConfiguredDecoder[A]], tag: TypeTag[A]): Decoder[A] = deriveConfiguredDecoder[A]
    .prepare( DeriveConfiguredDecoderWithDeprecationCheck.checkDeprecation[A] )
    .validate(
      validator[A],
      s"Could not convert json to ${typeOf[A].baseClasses.head.fullName}."
    )
}
