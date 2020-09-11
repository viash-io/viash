package com.dataintuitive.viash.platforms

import io.circe.{Decoder, Encoder, Json}
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.{deriveConfiguredDecoder, deriveConfiguredEncoder}
import cats.syntax.functor._ // for .widen

package object requirements {
  implicit val customConfig: Configuration = Configuration.default.withDefaults

  implicit val encodeRRequirements: Encoder.AsObject[RRequirements] = deriveConfiguredEncoder
  implicit val decodeRRequirements: Decoder[RRequirements] = deriveConfiguredDecoder

  implicit val encodePythonRequirements: Encoder.AsObject[PythonRequirements] = deriveConfiguredEncoder
  implicit val decodePythonRequirements: Decoder[PythonRequirements] = deriveConfiguredDecoder

  implicit val encodeAptRequirements: Encoder.AsObject[AptRequirements] = deriveConfiguredEncoder
  implicit val decodeAptRequirements: Decoder[AptRequirements] = deriveConfiguredDecoder

  implicit val encodeApkRequirements: Encoder.AsObject[ApkRequirements] = deriveConfiguredEncoder
  implicit val decodeApkRequirements: Decoder[ApkRequirements] = deriveConfiguredDecoder

  implicit val encodeDockerRequirements: Encoder.AsObject[DockerRequirements] = deriveConfiguredEncoder
  implicit val decodeDockerRequirements: Decoder[DockerRequirements] = deriveConfiguredDecoder

  implicit val encodeNextFlowRequirements: Encoder.AsObject[NextFlowRequirements] = deriveConfiguredEncoder
  implicit val decodeNextFlowRequirements: Decoder[NextFlowRequirements] = deriveConfiguredDecoder

  implicit def encodeRequirements[A <: Requirements]: Encoder[A] = Encoder.instance {
    reqs =>
      val typeJson = Json.obj("type" → Json.fromString(reqs.`type`))
      val objJson = reqs match {
        case s: ApkRequirements => encodeApkRequirements(s)
        case s: AptRequirements => encodeAptRequirements(s)
        case s: DockerRequirements => encodeDockerRequirements(s)
        case s: NextFlowRequirements => encodeNextFlowRequirements(s)
        case s: PythonRequirements => encodePythonRequirements(s)
        case s: RRequirements => encodeRRequirements(s)
      }
      objJson deepMerge typeJson
  }

  implicit def decodeRequirements: Decoder[Requirements] = Decoder.instance {
    cursor =>
      val decoder: Decoder[Requirements] =
        cursor.downField("type").as[String] match {
          case Right("apk") => decodeApkRequirements.widen
          case Right("apt") => decodeAptRequirements.widen
          case Right("docker") => decodeDockerRequirements.widen
          case Right("nextflow") => decodeNextFlowRequirements.widen
          case Right("python") => decodePythonRequirements.widen
          case Right("r") => decodeRRequirements.widen
          case Right(typ) => throw new RuntimeException("Type " + typ + " is not recognised.")
          case Left(exception) => throw exception
        }

      decoder(cursor)
  }
}