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

package com.dataintuitive.viash

import io.circe.generic.extras.semiauto.{deriveConfiguredDecoder, deriveConfiguredEncoder}
import io.circe.{Decoder, Encoder, Json}
import io.circe.ACursor

package object functionality {
  // import implicits

  import functionality.dataobjects._
  import functionality.resources._
  import com.dataintuitive.viash.helpers.Circe._

  // encoder and decoder for Functionality
  implicit val encodeFunctionality: Encoder.AsObject[Functionality] = deriveConfiguredEncoder

  // add file & direction defaults for inputs & outputs
  implicit val decodeFunctionality: Decoder[Functionality] = deriveConfiguredDecoder[Functionality].prepare {
    _.withFocus(_.mapObject{ fun0 =>
      
      val fun1 = fun0.apply("inputs") match {
        case Some(inputs) => 
          val newInputs = inputs.mapArray(_.map{ js =>
            js.withDefault("type", Json.fromString("file"))
              .withDefault("direction", Json.fromString("input"))
          })
          fun0.add("inputs", newInputs)
        case None => fun0
      }

      val fun2 = fun1.apply("outputs") match {
        case Some(outputs) => 
          val newOutputs = outputs.mapArray(_.map{ js =>
            js.withDefault("type", Json.fromString("file"))
              .withDefault("direction", Json.fromString("output"))
          })
          fun1.add("outputs", newOutputs)
        case None => fun1
      }

      fun2
    })
  }

  // encoder and decoder for Author
  implicit val encodeAuthor: Encoder.AsObject[Author] = deriveConfiguredEncoder
  implicit val decodeAuthor: Decoder[Author] = deriveConfiguredDecoder
}
