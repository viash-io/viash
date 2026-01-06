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

  // encoders and decoders for Config
  implicit val encodeConfig: Encoder.AsObject[Config] = deriveConfiguredEncoderStrict[Config]
  implicit val decodeConfig: Decoder[Config] = deriveConfiguredDecoderWithValidationCheck[Config].prepare{
    checkDeprecation[Config](_)
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
