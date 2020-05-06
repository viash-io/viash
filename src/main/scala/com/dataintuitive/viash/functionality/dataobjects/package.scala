package com.dataintuitive.viash.functionality

import io.circe.{ Decoder, Encoder, Json }
import io.circe.generic.extras.Configuration
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.generic.extras.semiauto.{deriveConfiguredDecoder, deriveConfiguredEncoder}
import cats.syntax.functor._ // for .widen

package object dataobjects {
  implicit val customConfig: Configuration = Configuration.default.withDefaults

  // encoder and decoder for java.io.File
  implicit val encodeFile: Encoder[java.io.File] = Encoder.instance {
    file => Json.fromString(file.getPath)
  }
  implicit val decodeFile: Decoder[java.io.File] = Decoder.instance {
    cursor => cursor.value.as[String].map(new java.io.File(_))
  }

  // encoder and decoder for direction
  implicit val encodeDirection: Encoder[Direction] = Encoder.instance {
    dir => Json.fromString(dir.toString)
  }
  implicit val decodeDirection: Decoder[Direction] = Decoder.instance {
    cursor => cursor.value.as[String].map(s =>
      s.toLowerCase() match {
        case "input" => Input
        case "output" => Output
        case "log" => Log
      }
    )
  }

  // encoders and decoders for Object
  implicit val encodeStringObject: Encoder.AsObject[StringObject] = deriveConfiguredEncoder
  implicit val encodeIntegerObject: Encoder.AsObject[IntegerObject] = deriveConfiguredEncoder
  implicit val encodeDoubleObject: Encoder.AsObject[DoubleObject] = deriveConfiguredEncoder
  implicit val encodeBooleanObject: Encoder.AsObject[BooleanObject] = deriveConfiguredEncoder
  implicit val encodeFileObject: Encoder.AsObject[FileObject] = deriveConfiguredEncoder

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

  implicit val decodeStringObject: Decoder[StringObject] = deriveConfiguredDecoder
  implicit val decodeIntegerObject: Decoder[IntegerObject] = deriveConfiguredDecoder
  implicit val decodeDoubleObject: Decoder[DoubleObject] = deriveConfiguredDecoder
  implicit val decodeBooleanObject: Decoder[BooleanObject] = deriveConfiguredDecoder
  implicit val decodeFileObject: Decoder[FileObject] = deriveConfiguredDecoder

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
}
