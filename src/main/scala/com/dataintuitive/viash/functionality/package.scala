package com.dataintuitive.viash

import java.io.File
import io.circe.{ Decoder, Encoder, HCursor, Json }
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

package object functionality {
  // encoders
  implicit val encodeFile: Encoder[File] = new Encoder[File] {
    final def apply(file: File): Json = 
      Json.fromString(file.getPath)
  }
  
  implicit def encodeParameter[A <: Parameter[_]]: Encoder[A] = new Encoder[A] {
    val encodeStringParameter: Encoder[StringParameter] = deriveEncoder[StringParameter]
    val encodeIntegerParameter: Encoder[IntegerParameter] = deriveEncoder[IntegerParameter]
    val encodeDoubleParameter: Encoder[DoubleParameter] = deriveEncoder[DoubleParameter]
    val encodeBooleanParameter: Encoder[BooleanParameter] = deriveEncoder[BooleanParameter]
    val encodeFileParameter: Encoder[FileParameter] = deriveEncoder[FileParameter]
    
    final def apply(par: A): Json = {
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
  }
  
  implicit val encodeResource: Encoder[Resource] = deriveEncoder[Resource]
  implicit val encodeFunctionality: Encoder[Functionality] = deriveEncoder[Functionality]
  
  // decoders
  implicit val decodeFile: Decoder[File] = new Decoder[File] {
    final def apply(c: HCursor): Decoder.Result[File] =
      for {
        path <- c.value.as[String]
      } yield {
        new File(path)
      }
  }
  implicit val decodeStringParameter: Decoder[StringParameter] = deriveDecoder[StringParameter]
  implicit val decodeIntegerParameter: Decoder[IntegerParameter] = deriveDecoder[IntegerParameter]
  implicit val decodeDoubleParameter: Decoder[DoubleParameter] = deriveDecoder[DoubleParameter]
  implicit val decodeBooleanParameter: Decoder[BooleanParameter] = deriveDecoder[BooleanParameter]
  implicit val decodeFileParameter: Decoder[FileParameter] = deriveDecoder[FileParameter]
  
  implicit def decodeParameter: Decoder[Parameter[_]] = new Decoder[Parameter[_]] {
    final def apply(c: HCursor): Decoder.Result[Parameter[_]] = {
      val res = c.downField("type").as[String] match {
        case Right("string") =>
          decodeStringParameter(c)
        case Right("integer") => 
          decodeIntegerParameter(c)
        case Right("double") => 
          decodeDoubleParameter(c)
        case Right("boolean") => 
          decodeBooleanParameter(c)
        case Right("file") => 
          decodeFileParameter(c)
        case Right(typ) =>
          throw new RuntimeException("Type " + typ + " is not recognised.")
        case Left(exception) =>
          throw exception
      }
      
      res.map(_.asInstanceOf[Parameter[_]])
    }
  }
  
  implicit val decodeResource: Decoder[Resource] = deriveDecoder[Resource]
  implicit val decodeFunctionality: Decoder[Functionality] = deriveDecoder[Functionality]
}