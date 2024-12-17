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

import io.circe.{Decoder, Encoder, Json, HCursor, JsonObject}
import io.viash.exceptions.ConfigParserValidationException

import config.ArgumentGroup
import config.Author
import config.ComputationalRequirements
import config.Links
import config.References
import config.Status._
import config.arguments._
import io.circe.DecodingFailure
import io.circe.DecodingFailure.Reason.CustomReason
import io.circe.derivation.{ConfiguredEnumEncoder, ConfiguredEnumDecoder}

package object config {
  import io.viash.helpers.circe._
  import io.viash.helpers.circe.DeriveConfiguredDecoderWithDeprecationCheck.checkDeprecation
  import io.viash.helpers.circe.DeriveConfiguredDecoderWithValidationCheck.deriveConfiguredDecoderWithValidationCheck

  import io.viash.config.resources.{decodeResource, encodeResource}
  import io.viash.config.dependencies.{decodeDependency, encodeDependency}
  import io.viash.config.dependencies.{decodeRepositoryWithName, encodeRepositoryWithName}
  import io.viash.runners.{decodeRunner, encodeRunner}
  import io.viash.engines.{decodeEngine, encodeEngine}
  import io.viash.packageConfig.{decodePackageConfig, encodePackageConfig}
  import io.viash.platforms.decodePlatform
  import io.viash.functionality.decodeFunctionality

  // encoders and decoders for Config
  implicit val encodeConfig: Encoder.AsObject[Config] = deriveConfiguredEncoderStrict[Config]
  implicit val decodeConfig: Decoder[Config] = deriveConfiguredDecoderWithValidationCheck[Config].prepare{
    checkDeprecation[Config](_)
    // map platforms to runners and engines
    .withFocus{
      _.mapObject{ conf =>
        // Transform platforms to runners and engines
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
  .prepare {
    // merge arguments and argument_groups into argument_groups
    _.withFocus{ json =>
      json.asObject match {
        case None => json
        case Some(jo) => 
          val arguments = jo.apply("arguments")
          val argument_groups = jo.apply("argument_groups")

          val newJsonObject = (arguments, argument_groups) match {
            case (None, _) => jo
            case (Some(args), None) => 
              jo.add("argument_groups", Json.fromValues(List(
                Json.fromJsonObject(
                  JsonObject(
                    "name" -> Json.fromString("Arguments"),
                    "arguments" -> args
                  )
                )
              )))
            case (Some(args), Some(arg_groups)) =>
              // determine if we should prepend or append arguments to argument_groups
              val prepend = jo.keys.find(s => s == "argument_groups" || s == "arguments") == Some("arguments")
              def combinerSeq(a: Vector[Json], b: Seq[Json]) = if (prepend) b ++: a else a :++ b
              def combiner(a: Vector[Json], b: Json) = if (prepend) b +: a else a :+ b

              // get the argument group named 'Arguments' from arg_groups
              val argumentsGroup = arg_groups.asArray.flatMap(_.find(_.asObject.exists(_.apply("name").exists(_ == Json.fromString("Arguments")))))
              argumentsGroup match {
                case None =>
                  // no argument_group with name 'Argument' exists, so just add arguments as a new argument group
                  jo.add("argument_groups", Json.fromValues(
                    combiner(
                      arg_groups.asArray.get,
                      Json.fromJsonObject(
                        JsonObject(
                          "name" -> Json.fromString("Arguments"),
                          "arguments" -> args
                        )
                      )
                    )
                  ))
                case Some(ag) =>
                  // argument_group with name 'Argument' exists, so add arguments to this argument group
                  val newAg = ag.asObject.get.add("arguments",
                    Json.fromValues(
                      combinerSeq(
                        ag.asObject.get.apply("arguments").get.asArray.get,
                        args.asArray.get
                      )
                    )
                  )
                  jo.add("argument_groups", Json.fromValues(arg_groups.asArray.get.map{
                    case ag if ag == argumentsGroup.get => Json.fromJsonObject(newAg)
                    case ag => ag
                  }))
              }
          }
          Json.fromJsonObject(newJsonObject.remove("arguments"))
      }    
  }}
  .prepare {
    // Move functionality to config level, if functionality exists also move .info to .build_info
    _.withFocus{
      _.mapObject{ conf =>
        conf.contains("functionality") match {
          case true => 
            val functionality = conf.apply("functionality").get.asObject.get
            val buildInfo = conf.apply("info")
            val conf1 = buildInfo.map{ bi => 
                conf.remove("info").add("build_info", bi)
              }.getOrElse(conf)
            val conf2 = conf1.remove("functionality")
            val conf3 = conf2.deepMerge(functionality)
            conf3
          case false => conf
        }
      }
  }}
  .validate(
    // Validate platforms and functionality only. Will get stripped in the next prepare steps.
    (pred: HCursor) => {
      val platforms = pred.downField("platforms")
      if (platforms.succeeded) {
        platforms.values.get.foreach{ p =>
          val validate = decodePlatform(p.hcursor)
          validate.fold(_ => {
            throw new ConfigParserValidationException("Platform", p.toString())
            false
          }, _ => true)
        }
      }
      val functionality = pred.downField("functionality")
      if (functionality.succeeded) {
        val json = functionality.focus.get
        val validate = decodeFunctionality(json.hcursor)
        
        validate.fold(_ => {
          throw new ConfigParserValidationException("Functionality", json.toString())
          false
        }, _ => true)
      }
      true
    },
    "Could not convert json to Config."
  )

  implicit val encodeBuildInfo: Encoder.AsObject[BuildInfo] = deriveConfiguredEncoder
  implicit val decodeBuildInfo: Decoder[BuildInfo] = deriveConfiguredDecoderFullChecks

  // encoder and decoder for Author
  implicit val encodeAuthor: Encoder.AsObject[Author] = deriveConfiguredEncoder
  implicit val decodeAuthor: Decoder[Author] = deriveConfiguredDecoderFullChecks

  // encoder and decoder for Requirements
  implicit val encodeComputationalRequirements: Encoder.AsObject[ComputationalRequirements] = deriveConfiguredEncoder
  implicit val decodeComputationalRequirements: Decoder[ComputationalRequirements] = deriveConfiguredDecoderFullChecks
  
  // encoder and decoder for ArgumentGroup
  implicit val encodeArgumentGroup: Encoder.AsObject[ArgumentGroup] = deriveConfiguredEncoder
  implicit val decodeArgumentGroup: Decoder[ArgumentGroup] = deriveConfiguredDecoderFullChecks

  // encoder and decoder for Status, make string lowercase before decoding
  implicit val encodeStatus: Encoder[Status] = ConfiguredEnumEncoder.derive(_.toLowerCase())
  implicit val decodeStatus: Decoder[Status] = ConfiguredEnumDecoder.derive[Status](_.toLowerCase()).prepare {
    _.withFocus(_.mapString(_.toLowerCase()))
  }

  // encoder and decoder for ScopeEnum, make string lowercase before decoding
  implicit val encodeScopeEnum: Encoder[ScopeEnum] = ConfiguredEnumEncoder.derive(_.toLowerCase())
  implicit val decodeScopeEnum: Decoder[ScopeEnum] = ConfiguredEnumDecoder.derive[ScopeEnum](_.toLowerCase()).prepare {
    _.withFocus(_.mapString(_.toLowerCase()))
  }

  // encoder and decoder for Scope
  implicit val encodeScope: Encoder.AsObject[Scope] = deriveConfiguredEncoder
  implicit val decodeScope: Decoder[Scope] = deriveConfiguredDecoderFullChecks

  implicit val encodeLinks: Encoder.AsObject[Links] = deriveConfiguredEncoderStrict
  implicit val decodeLinks: Decoder[Links] = deriveConfiguredDecoderFullChecks

  implicit val encodeReferences: Encoder.AsObject[References] = deriveConfiguredEncoderStrict
  implicit val decodeReferences: Decoder[References] = deriveConfiguredDecoderFullChecks
}
