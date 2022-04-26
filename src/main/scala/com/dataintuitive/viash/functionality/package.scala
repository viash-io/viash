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

  // encoder and decoder for functiontype
  implicit val encodeFunctionType: Encoder[FunctionType] = Encoder.instance {
    ft => Json.fromString(ft.toString.toLowerCase())
  }
  implicit val decodeFunctionType: Decoder[FunctionType] = Decoder.instance {
    cursor =>
      cursor.value.as[String].map(s =>
        s.toLowerCase() match {
          case "asis" => AsIs
          case "transform" => Convert
          case "convert" => Convert
          case "todir" => ToDir
          case "join" => Join
        }
      )
  }


  // encoder and decoder for Functionality
  implicit val encodeFunctionality: Encoder.AsObject[Functionality] = deriveConfiguredEncoder
  // implicit val decodeFunctionality: Decoder[Functionality] = deriveConfiguredDecoder
  implicit val decodeFunctionality: Decoder[Functionality] = deriveConfiguredDecoder[Functionality].prepare { (aCursor: ACursor) =>
    {
      aCursor.withFocus(json => {
        json.mapObject(jsonObject => {
          val inputs = jsonObject.apply("inputs")
          val outputs = jsonObject.apply("outputs")
          val arguments = jsonObject.apply("arguments")
          
          if (inputs.isDefined) {
            println("inputs:" + inputs)
          }
          if (outputs.isDefined) {
            println("outputs:" + outputs)
          }
          
          jsonObject
          // if (jsonObject.contains("public")) {
          //   jsonObject
          // } else {
          //   jsonObject.add("public", Json.fromBoolean(false))
          // }
        })
      })
    }
  }

  // encoder and decoder for Author
  implicit val encodeAuthor: Encoder.AsObject[Author] = deriveConfiguredEncoder
  implicit val decodeAuthor: Decoder[Author] = deriveConfiguredDecoder
}
