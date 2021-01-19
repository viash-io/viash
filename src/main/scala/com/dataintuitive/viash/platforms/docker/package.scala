package com.dataintuitive.viash.platforms

import io.circe.{Decoder, Encoder, Json}
import io.circe.generic.extras.semiauto.{deriveConfiguredDecoder, deriveConfiguredEncoder}

package object docker {
  import com.dataintuitive.viash.helpers.Circe._

  // encoder and decoder for direction
  implicit val encodeResolveVolume: Encoder[DockerResolveVolume] = Encoder.instance {
    v => Json.fromString(v.toString)
  }
  implicit val decodeResolveVolume: Decoder[DockerResolveVolume] = Decoder.instance {
    cursor =>
      cursor.value.as[String].map(s =>
        s.toLowerCase() match {
          case "manual" => Manual
          case "auto" | "automatic" => Automatic
        }
      )
  }

  // encoder and decoder for direction
  implicit val encodeSetupStrategy: Encoder[DockerSetupStrategy] = Encoder.instance {
    dss => Json.fromString(dss.id.toLowerCase)
  }
  implicit val decodeSetupStrategy: Decoder[DockerSetupStrategy] = Decoder.instance {
    cursor =>
      cursor.value.as[String].map { s =>
        val id = s.toLowerCase.replaceAll("_", "")
        DockerSetupStrategy.map.applyOrElse(id,
          (key: String) => throw new Exception(s"Docker Setup Strategy '$key' not found.")
        )
      }
  }
}

