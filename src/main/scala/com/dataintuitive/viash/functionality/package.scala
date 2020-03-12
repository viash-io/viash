package com.dataintuitive.viash

import java.io.File
import io.circe.{ Decoder, Encoder, HCursor, Json }
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.generic.auto._
import io.circe.syntax._
import cats.syntax.functor._ // for .widen

package object functionality {
  // encoder and decoder for java.io.File
  implicit val encodeFile: Encoder[File] = Encoder.instance {
    file =>
      Json.fromString(file.getPath)
  }
  implicit val decodeFile: Decoder[File] = Decoder.instance {
    cursor =>
      cursor.value.as[String].map(new File(_))
  }
  
  // encoders and decoders for Parameter
  implicit val encodeStringParameter: Encoder[StringParameter] = deriveEncoder
  implicit val encodeIntegerParameter: Encoder[IntegerParameter] = deriveEncoder
  implicit val encodeDoubleParameter: Encoder[DoubleParameter] = deriveEncoder
  implicit val encodeBooleanParameter: Encoder[BooleanParameter] = deriveEncoder
  implicit val encodeFileParameter: Encoder[FileParameter] = deriveEncoder
    
  implicit def encodeParameter[A <: Parameter[_]]: Encoder[A] = Encoder.instance {
    par => 
      val typeJson = Json.obj("type" → Json.fromString(par.`type`))
      val objJson = par match {
        case s: StringParameter => encodeStringParameter(s) 
        case s: IntegerParameter => encodeIntegerParameter(s)
        case s: DoubleParameter => encodeDoubleParameter(s)
        case s: BooleanParameter => encodeBooleanParameter(s)
        case s: FileParameter => encodeFileParameter(s)
      }
      objJson deepMerge typeJson
  }
  
  implicit val decodeStringParameter: Decoder[StringParameter] = deriveDecoder
  implicit val decodeIntegerParameter: Decoder[IntegerParameter] = deriveDecoder
  implicit val decodeDoubleParameter: Decoder[DoubleParameter] = deriveDecoder
  implicit val decodeBooleanParameter: Decoder[BooleanParameter] = deriveDecoder
  implicit val decodeFileParameter: Decoder[FileParameter] = deriveDecoder
  
  implicit def decodeParameter: Decoder[Parameter[_]] = Decoder.instance {
    cursor => 
      val decoder: Decoder[Parameter[_]] = 
        cursor.downField("type").as[String] match {
          case Right("string") => decodeStringParameter.widen
          case Right("integer") => decodeIntegerParameter.widen
          case Right("double") => decodeDoubleParameter.widen
          case Right("boolean") => decodeBooleanParameter.widen
          case Right("file") => decodeFileParameter.widen
          case Right(typ) => throw new RuntimeException("Type " + typ + " is not recognised.")
          case Left(exception) => throw exception
        }
      
      decoder(cursor)
  }
  
  // encoder and decoder for Resource  
  implicit val encodeResource: Encoder[Resource] = deriveEncoder
  implicit val decodeResource: Decoder[Resource] = deriveDecoder
  
  // encoder and decoder for Functionality
  implicit val encodeFunctionality: Encoder[Functionality] = deriveEncoder
  implicit val decodeFunctionality: Decoder[Functionality] = deriveDecoder
}