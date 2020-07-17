package com.dataintuitive.viash

//import io.circe.{ Decoder, Encoder, Json }
//import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

import io.circe.{ Decoder, Encoder, Json }
import io.circe.generic.extras.Configuration
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.generic.extras.semiauto.{deriveConfiguredDecoder, deriveConfiguredEncoder}
import cats.syntax.functor._ // for .widen

package object targets {
  implicit val customConfig: Configuration = Configuration.default.withDefaults

    // encoder and decoder for direction
  implicit val encodeResolveVolume: Encoder[ResolveVolume] = Encoder.instance {
    v => Json.fromString(v.toString)
  }
  implicit val decodeResolveVolume: Decoder[ResolveVolume] = Decoder.instance {
    cursor => cursor.value.as[String].map(s =>
      s.toLowerCase() match {
        case "manual" => Manual
        case "auto" | "automatic" => Automatic
      }
    )
  }

  implicit val encodeDockerTarget: Encoder.AsObject[DockerTarget] = deriveConfiguredEncoder
  implicit val decodeDockerTarget: Decoder[DockerTarget] = deriveConfiguredDecoder

  implicit val encodeNextFlowTarget: Encoder[NextFlowTarget] = deriveEncoder
  implicit val decodeNextFlowTarget: Decoder[NextFlowTarget] = deriveDecoder

  implicit val encodeNativeTarget: Encoder[NativeTarget] = deriveEncoder
  implicit val decodeNativeTarget: Decoder[NativeTarget] = deriveDecoder

  implicit def encodeTarget[A <: Target]: Encoder[A] = Encoder.instance {
    Target =>
      val typeJson = Json.obj("type" → Json.fromString(Target.`type`))
      val objJson = Target match {
        case s: DockerTarget => encodeDockerTarget(s)
        case s: NextFlowTarget => encodeNextFlowTarget(s)
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
          case Right("nextflow") => decodeNextFlowTarget.widen
          case Right(typ) => throw new RuntimeException("Type " + typ + " is not recognised.")
          case Left(exception) => throw exception
        }

      decoder(cursor)
  }
}
