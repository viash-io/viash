package com.dataintuitive.viash

import io.circe.{ Decoder, Encoder, Json }
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import cats.syntax.functor._ // for .widen

package object targets {
  implicit val encodeVolume: Encoder[Volume] = deriveEncoder
  implicit val decodeVolume: Decoder[Volume] = deriveDecoder
  
  implicit val encodeREnvironment: Encoder[REnvironment] = deriveEncoder
  implicit val decodeREnvironment: Decoder[REnvironment] = deriveDecoder
  
  implicit val encodePythonEnvironment: Encoder[PythonEnvironment] = deriveEncoder
  implicit val decodePythonEnvironment: Decoder[PythonEnvironment] = deriveDecoder
  
  implicit val encodeDockerTarget: Encoder[DockerTarget] = deriveEncoder
  implicit val decodeDockerTarget: Decoder[DockerTarget] = deriveDecoder
  
  implicit val encodeNativeTarget: Encoder[NativeTarget] = deriveEncoder
  implicit val decodeNativeTarget: Decoder[NativeTarget] = deriveDecoder
  
  implicit def encodeTarget[A <: Target]: Encoder[A] = Encoder.instance {
    Target => 
      val typeJson = Json.obj("type" → Json.fromString(Target.`type`))
      val objJson = Target match {
        case s: DockerTarget => encodeDockerTarget(s) 
        case s: NativeTarget => encodeNativeTarget(s)
      }
      objJson deepMerge typeJson
  }
  
  implicit def decodeTarget: Decoder[Target] = Decoder.instance {
    cursor => 
      val decoder: Decoder[Target] = 
        cursor.downField("type").as[String] match {
          case Right("docker") => decodeDockerTarget.widen
          case Right("native") => decodeNativeTarget.widen
          case Right(typ) => throw new RuntimeException("Type " + typ + " is not recognised.")
          case Left(exception) => throw exception
        }
      
      decoder(cursor)
  }
}