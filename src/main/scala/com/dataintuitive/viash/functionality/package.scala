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

          var newJsonObject = jsonObject

          if (jsonObject.contains("inputs")) {
            val inputJson = jsonObject.apply("inputs").get.mapArray(_.map(
              _.mapObject( obj =>	
                obj.contains("type") match {
                  case false => obj.add("type", Json.fromString("file"))
                  case true => obj
                }
              )
              .mapObject( obj =>	
                obj.contains("direction") match {
                  case false => obj.add("direction", Json.fromString("input"))
                  case true => obj
                }
              )
            ))
            newJsonObject = newJsonObject.add("inputs", inputJson)
          }
          
          if (jsonObject.contains("outputs")) {
            val outputJson = jsonObject.apply("outputs").get.mapArray(_.map(
              _.mapObject( obj =>	
                obj.contains("type") match {
                  case false => obj.add("type", Json.fromString("file"))
                  case true => obj
                }
              )
              .mapObject( obj =>	
                obj.contains("direction") match {
                  case false => obj.add("direction", Json.fromString("output"))
                  case true => obj
                }
              )
            ))
            newJsonObject = newJsonObject.add("outputs", outputJson)
          }
          
          newJsonObject
        })
      })
    }
  }

  // encoder and decoder for Author
  implicit val encodeAuthor: Encoder.AsObject[Author] = deriveConfiguredEncoder
  implicit val decodeAuthor: Decoder[Author] = deriveConfiguredDecoder
}
