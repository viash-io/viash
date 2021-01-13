package com.dataintuitive.viash

import io.circe.{Decoder, Encoder, Json}
import io.circe.generic.extras.semiauto.{deriveConfiguredDecoder, deriveConfiguredEncoder}
import cats.syntax.functor._ // for .widen

package object platforms {
  import com.dataintuitive.viash.helpers.Circe._

  implicit val encodeDockerPlatform: Encoder.AsObject[DockerPlatform] = deriveConfiguredEncoder
  implicit val decodeDockerPlatform: Decoder[DockerPlatform] = deriveConfiguredDecoder

  implicit val encodeNextFlowPlatform: Encoder.AsObject[NextFlowPlatform] = deriveConfiguredEncoder
  implicit val decodeNextFlowPlatform: Decoder[NextFlowPlatform] = deriveConfiguredDecoder

  implicit val encodeNativePlatform: Encoder.AsObject[NativePlatform] = deriveConfiguredEncoder
  implicit val decodeNativePlatform: Decoder[NativePlatform] = deriveConfiguredDecoder

  implicit def encodePlatform[A <: Platform]: Encoder[A] = Encoder.instance {
    platform =>
      val typeJson = Json.obj("type" → Json.fromString(platform.`type`))
      val objJson = platform match {
        case s: DockerPlatform => encodeDockerPlatform(s)
        case s: NextFlowPlatform => encodeNextFlowPlatform(s)
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
          case Right("nextflow") => decodeNextFlowPlatform.widen
          case Right(typ) => throw new RuntimeException("Type " + typ + " is not recognised.")
          case Left(exception) => throw exception
        }

      decoder(cursor)
  }
}
