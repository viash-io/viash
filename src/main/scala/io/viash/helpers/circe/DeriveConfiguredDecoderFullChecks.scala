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

import io.circe.Decoder
import io.circe.derivation.Configuration
import scala.deriving.Mirror
import io.viash.helpers.typeOf

object DeriveConfiguredDecoderFullChecks {
  import io.viash.helpers.circe.DeriveConfiguredDecoderWithDeprecationCheck.checkDeprecation
  import io.viash.helpers.circe.DeriveConfiguredDecoderWithValidationCheck.validator

  inline def deriveConfiguredDecoderFullChecks[A](using inline A: Mirror.Of[A], inline configuration: Configuration): Decoder[A] = deriveConfiguredDecoder[A]
    .validate(
      validator[A],
      s"Could not convert json to ${typeOf[A]}."
    )
    .prepare( checkDeprecation[A] )
}
