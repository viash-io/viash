package com.dataintuitive.viash

import io.circe.{ Decoder, Encoder, Json }
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import cats.syntax.functor._ // for .widen

package object functionality {
  // encoder and decoder for java.io.File
  implicit val encodeFile: Encoder[java.io.File] = Encoder.instance {
    file => Json.fromString(file.getPath)
  }
  implicit val decodeFile: Decoder[java.io.File] = Decoder.instance {
    cursor => cursor.value.as[String].map(new java.io.File(_))
  }
  
  // encoders and decoders for Object
  implicit val encodeStringObject: Encoder[StringObject] = deriveEncoder
  implicit val encodeIntegerObject: Encoder[IntegerObject] = deriveEncoder
  implicit val encodeDoubleObject: Encoder[DoubleObject] = deriveEncoder
  implicit val encodeBooleanObject: Encoder[BooleanObject] = deriveEncoder
  implicit val encodeFileObject: Encoder[FileObject] = deriveEncoder
    
  implicit def encodeDataObject[A <: DataObject[_]]: Encoder[A] = Encoder.instance {
    par => 
      val typeJson = Json.obj("type" → Json.fromString(par.`type`))
      val objJson = par match {
        case s: StringObject => encodeStringObject(s) 
        case s: IntegerObject => encodeIntegerObject(s)
        case s: DoubleObject => encodeDoubleObject(s)
        case s: BooleanObject => encodeBooleanObject(s)
        case s: FileObject => encodeFileObject(s)
      }
      objJson deepMerge typeJson
  }
  
  implicit val decodeStringObject: Decoder[StringObject] = deriveDecoder
  implicit val decodeIntegerObject: Decoder[IntegerObject] = deriveDecoder
  implicit val decodeDoubleObject: Decoder[DoubleObject] = deriveDecoder
  implicit val decodeBooleanObject: Decoder[BooleanObject] = deriveDecoder
  implicit val decodeFileObject: Decoder[FileObject] = deriveDecoder
  
  implicit def decodeDataObject: Decoder[DataObject[_]] = Decoder.instance {
    cursor => 
      val decoder: Decoder[DataObject[_]] = 
        cursor.downField("type").as[String] match {
          case Right("string") => decodeStringObject.widen
          case Right("integer") => decodeIntegerObject.widen
          case Right("double") => decodeDoubleObject.widen
          case Right("boolean") => decodeBooleanObject.widen
          case Right("file") => decodeFileObject.widen
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