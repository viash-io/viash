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

import io.circe.Encoder
import io.circe.derivation.{Configuration, ConfiguredEncoder}
import scala.deriving.Mirror

// import io.circe.{Encoder, Json, HCursor}

// import scala.reflect.runtime.universe._
// import shapeless.Lazy
import io.viash.schemas.ParameterSchema
import io.viash.schemas.CollectedSchemas

object DeriveConfiguredEncoderStrict {

  // final def deriveConfiguredEncoderStrict[T](implicit encode: Lazy[ConfiguredAsObjectEncoder[T]], tag: TypeTag[T]) = deriveConfiguredEncoder[T]
  //   .mapJsonObject{ jsonObject =>
  //     val parameters = CollectedSchemas.getParameters[T]()
  //     jsonObject.filterKeys( k => 
  //       parameters
  //         .find(_.name == k) // find the correct parameter
  //         .map(!_.hasInternalFunctionality) // check if it has the 'internalFunctionality' annotation
  //         .getOrElse(true) // fallback, shouldn't really happen
  //     )
  //   }

  inline def deriveConfiguredEncoderStrict[A](using inline A: Mirror.Of[A], inline configuration: Configuration) = ConfiguredEncoder.derived[A]
}
