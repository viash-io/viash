package com.dataintuitive.viash

import io.circe.{ Decoder, Encoder, Json }
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import com.dataintuitive.viash.targets.environments._

package object targets {
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
  
  
  import cats.syntax.functor._ // for .widen
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