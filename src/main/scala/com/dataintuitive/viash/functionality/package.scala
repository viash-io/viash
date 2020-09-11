package com.dataintuitive.viash

import io.circe.{ Decoder, Encoder, Json }
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

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
    cursor => cursor.value.as[String].map(s =>
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
  implicit val encodeFunctionality: Encoder[Functionality] = deriveEncoder
  implicit val decodeFunctionality: Decoder[Functionality] = deriveDecoder
}
