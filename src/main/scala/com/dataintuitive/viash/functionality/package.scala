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

package io.viash

import io.circe.generic.extras.semiauto.{deriveConfiguredDecoder, deriveConfiguredEncoder}
import io.circe.{Decoder, Encoder, Json}
import io.circe.ACursor

package object functionality {
  private var noticeFunTestDeprTests: Boolean = true
  private var noticeFunTestDeprEnabled: Boolean = true
  // import implicits

  import functionality.arguments._
  import functionality.resources._
  import functionality.Status._
  import io.viash.helpers.Circe._

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
          if (noticeFunTestDeprTests) {
            // todo: solve this in a cleaner way
            Console.err.println("Notice: functionality.tests is deprecated. Please use functionality.test_resources instead.")
            noticeFunTestDeprTests = false
          }
          fun2.add("test_resources", fun2.apply("tests").get).remove("tests")
        case (_, _) => fun2
      }

      // provide backwards compability for enabled -> status
      val fun4 = (fun3.contains("enabled"), fun3.contains("status")) match {
        case (true, true) =>
          Console.err.println("Error: functionality.enabled is deprecated. Please use functionality.status instead.")
          Console.err.println("Backwards compability is provided but not in combination with functionality.status")
          fun3
        case (true, false) =>
          if (noticeFunTestDeprEnabled) {
            Console.err.println("Notice: functionality.enabled is deprecated. Please use functionality.status instead.")
            noticeFunTestDeprEnabled = false
          }
          fun3.apply("enabled").get.asBoolean match {
            case Some(true) => fun3.add("status", Json.fromString("enabled")).remove("enabled")
            case Some(false) => fun3.add("status", Json.fromString("disabled")).remove("enabled")
            case None => fun3
          }
        case (_, _) => fun3
      }

      fun4
    })
  }

  // encoder and decoder for Author
  implicit val encodeAuthor: Encoder.AsObject[Author] = deriveConfiguredEncoder
  implicit val decodeAuthor: Decoder[Author] = deriveConfiguredDecoder
  
  // encoder and decoder for ArgumentGroup
  implicit val encodeArgumentGroup: Encoder.AsObject[ArgumentGroup] = deriveConfiguredEncoder
  implicit val decodeArgumentGroup: Decoder[ArgumentGroup] = deriveConfiguredDecoder

  // encoder and decoder for Status, make string lowercase before decoding
  implicit val encodeStatus: Encoder[Status] = Encoder.encodeEnumeration(Status)
  implicit val decodeStatus: Decoder[Status] = Decoder.decodeEnumeration(Status).prepare {
    _.withFocus(_.mapString(_.toLowerCase()))
  }

}
