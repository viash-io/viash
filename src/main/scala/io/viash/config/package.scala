/*
 * Copyright (C) 2020  Data Intuitive
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.viash

import io.circe.{Decoder, Encoder, Json}
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.{deriveConfiguredDecoder, deriveConfiguredEncoder}

package object config {
  import io.viash.helpers.circe.DeriveConfiguredDecoderFullChecks._

  implicit val customConfig: Configuration = Configuration.default.withDefaults

  // encoders and decoders for Config
  implicit val encodeConfig: Encoder.AsObject[Config] = deriveConfiguredEncoder
  implicit val decodeConfig: Decoder[Config] = deriveConfiguredDecoderFullChecks[Config].prepare{
    // map platforms to runners and engines
    _.withFocus{
      _.mapObject{ conf =>
        val runners = conf.apply("platforms").map {
          platforms => 
            platforms.mapArray(platformVector => {
              platformVector.map{platform =>
                val pObj = platform.asObject.get
                pObj.apply("type").get.asString.get match {
                  case "native" =>
                    Json.obj(
                      "type" -> Json.fromString("executable"),
                      "id" -> pObj.apply("id").getOrElse(Json.fromString("native")),
                    )
                  case "docker" =>
                    if (pObj.apply("chown") == Some(Json.False)) {
                      throw new RuntimeException("DockerPlatform.chown: false is not supported in backwards compatibility mode")
                    }
                    Json.obj(
                      "type" -> Json.fromString("executable"),
                      "id" -> pObj.apply("id").getOrElse(Json.fromString("docker")),
                      "port" -> pObj.apply("port").getOrElse(Json.Null),
                      "workdir" -> pObj.apply("workdir").getOrElse(Json.Null),
                      "docker_setup_strategy" -> pObj.apply("setup_strategy").getOrElse(Json.Null),
                      "docker_run_args" -> pObj.apply("run_args").getOrElse(Json.Null)            
                    )
                  case "nextflow" => 
                    Json.obj(
                      "type" -> Json.fromString("nextflow"),
                      "id" -> pObj.apply("id").getOrElse(Json.fromString("nextflow")),
                      "directives" -> pObj.apply("directives").getOrElse(Json.Null),
                      "auto" -> pObj.apply("auto").getOrElse(Json.Null),
                      "config" -> pObj.apply("config").getOrElse(Json.Null),
                      "debug" -> pObj.apply("debug").getOrElse(Json.Null),
                      "container" -> pObj.apply("container").getOrElse(Json.Null)
                    )
                  case s =>
                    throw new RuntimeException(s"Platform compability error. Type '$s' is not recognised. Valid types are 'docker', 'native', and 'nextflow'.\n$platform.")
                }
              }
            })
        }

        val engines = conf.apply("platforms").map {
          platforms => 
            platforms.mapArray(platformVector => {
              platformVector.flatMap{platform =>
                val pObj = platform.asObject.get
                pObj.apply("type").get.asString.get match {
                  case "native" =>
                    Some(Json.obj(
                      "type" -> Json.fromString("native"),
                      "id" -> pObj.apply("id").getOrElse(Json.fromString("native")),
                    ))
                  case "docker" =>
                    Some(Json.obj(
                      "type" -> Json.fromString("docker"),
                      "id" -> pObj.apply("id").getOrElse(Json.fromString("docker")),
                      "image" -> pObj.apply("image").getOrElse(Json.Null),
                      "organization" -> pObj.apply("organization").getOrElse(Json.Null),
                      "registry" -> pObj.apply("registry").getOrElse(Json.Null),
                      "tag" -> pObj.apply("tag").getOrElse(Json.Null),
                      "target_image" -> pObj.apply("target_image").getOrElse(Json.Null),
                      "target_organization" -> pObj.apply("target_organization").getOrElse(Json.Null),
                      "target_registry" -> pObj.apply("target_registry").getOrElse(Json.Null),
                      "target_tag" -> pObj.apply("target_tag").getOrElse(Json.Null),
                      "namespace_separator" -> pObj.apply("namespace_separator").getOrElse(Json.Null),
                      "target_image_source" -> pObj.apply("target_image_source").getOrElse(Json.Null),
                      "setup" -> pObj.apply("setup").getOrElse(Json.Null),
                      "test_setup" -> pObj.apply("test_setup").getOrElse(Json.Null),
                      "entrypoint" -> pObj.apply("entrypoint").getOrElse(Json.Null),
                      "cmd" -> pObj.apply("cmd").getOrElse(Json.Null)
                    ))
                  case "nextflow" =>
                    None
                  case s =>
                    throw new RuntimeException(s"Platform compability error. Type '$s' is not recognised. Valid types are 'docker', 'native', and 'nextflow'.\n$platform")
                }
              }
            })
        }

        // Add native engine if there would be no engines and a nextflow platform exists
        val nextflowPlatformExists = conf.apply("platforms").flatMap {
          platforms => 
            platforms.asArray.map(platformVector => {
              platformVector.exists{ platform =>
                val pObj = platform.asObject.get
                pObj.apply("type").get.asString.get match {
                  case "nextflow" => true
                  case _ => false
                }
              }
            })
        }.getOrElse(false)
        val noPlatformEngines = engines.map(_.asArray.get.isEmpty).getOrElse(true)
        val noEnginesInConfig = conf.apply("engines").map(_.asArray.get.isEmpty).getOrElse(true)
        val engines2 = 
          if (noEnginesInConfig && noPlatformEngines && nextflowPlatformExists) {
            Some(Json.arr(Json.obj("type" -> Json.fromString("native"))))
          } else {
            engines
          }

        // Create final config
        val conf1 = conf.remove("platforms")

        val conf2 = (conf1.apply("runners"), runners) match {
          case (Some(r), Some(r1)) => conf1.add("runners", Json.arr((r.asArray.get ++ r1.asArray.get):_*))
          case (None, Some(r1)) => conf1.add("runners", r1)
          case _ => conf1
        }

        val conf3 = (conf2.apply("engines"), engines2) match {
          case (Some(e), Some(e1)) => conf2.add("engines", Json.arr((e.asArray.get ++ e1.asArray.get):_*))
          case (None, Some(e1)) => conf2.add("engines", e1)
          case _ => conf2
        }

        conf3
      }
    }
  }

  implicit val encodeInfo: Encoder[Info] = deriveConfiguredEncoder
  implicit val decodeInfo: Decoder[Info] = deriveConfiguredDecoderFullChecks
}
