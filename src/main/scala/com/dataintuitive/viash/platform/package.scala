package com.dataintuitive.viash

import io.circe.{ Decoder, Encoder, Json }
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import cats.syntax.functor._ // for .widen

package object platform {
  implicit val encodeVolume: Encoder[Volume] = deriveEncoder
  implicit val decodeVolume: Decoder[Volume] = deriveDecoder
  
  implicit val encodeREnvironment: Encoder[REnvironment] = deriveEncoder
  implicit val decodeREnvironment: Decoder[REnvironment] = deriveDecoder
  
  implicit val encodePythonEnvironment: Encoder[PythonEnvironment] = deriveEncoder
  implicit val decodePythonEnvironment: Decoder[PythonEnvironment] = deriveDecoder
  
  implicit val encodeDockerPlatform: Encoder[DockerPlatform] = deriveEncoder
  implicit val decodeDockerPlatform: Decoder[DockerPlatform] = deriveDecoder
  
  implicit val encodeNativePlatform: Encoder[NativePlatform] = deriveEncoder
  implicit val decodeNativePlatform: Decoder[NativePlatform] = deriveDecoder
  
  implicit def encodePlatform[A <: Platform]: Encoder[A] = Encoder.instance {
    platform => 
      val typeJson = Json.obj("type" → Json.fromString(platform.`type`))
      val objJson = platform match {
        case s: DockerPlatform => encodeDockerPlatform(s) 
        case s: NativePlatform => encodeNativePlatform(s)
      }
      objJson deepMerge typeJson
  }
  
  implicit def decodePlatform: Decoder[Platform] = Decoder.instance {
    cursor => 
      val decoder: Decoder[Platform] = 
        cursor.downField("type").as[String] match {
          case Right("docker") => decodeDockerPlatform.widen
          case Right("native") => decodeNativePlatform.widen
          case Right(typ) => throw new RuntimeException("Type " + typ + " is not recognised.")
          case Left(exception) => throw exception
        }
      
      decoder(cursor)
  }
}