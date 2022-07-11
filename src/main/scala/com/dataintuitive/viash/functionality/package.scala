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
  private var noticeFunTestDepr: Boolean = true
  // import implicits

  import functionality.arguments._
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

      // provide backwards compability for tests -> test_resources
      val fun3 = (fun2.contains("tests"), fun2.contains("test_resources")) match {
        case (true, true) => 
          Console.err.println("Error: functionality.tests is deprecated. Please use functionality.test_resources instead.")
          Console.err.println("Backwards compability is provided but not in combination with functionality.test_resources.")
          fun2
        case (true, false) =>
          if (noticeFunTestDepr) {
            // todo: solve this in a cleaner way
            Console.err.println("Notice: functionality.tests is deprecated. Please use functionality.test_resources instead.")
            noticeFunTestDepr = false
          }
          fun2.add("test_resources", fun2.apply("tests").get).remove("tests")
        case (_, _) => fun2
      }

      fun3
    })
  }

  // encoder and decoder for Author
  implicit val encodeAuthor: Encoder.AsObject[Author] = deriveConfiguredEncoder
  implicit val decodeAuthor: Decoder[Author] = deriveConfiguredDecoder
  
  // encoder and decoder for ArgumentGroup
  implicit val encodeArgumentGroup: Encoder.AsObject[ArgumentGroup] = deriveConfiguredEncoder
  implicit val decodeArgumentGroup: Decoder[ArgumentGroup] = deriveConfiguredDecoder
}
