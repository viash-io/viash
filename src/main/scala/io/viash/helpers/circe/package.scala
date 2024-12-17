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

package io.viash.helpers

import io.circe._
import io.circe.derivation.{Configuration, ConfiguredDecoder, ConfiguredEncoder}
import java.net.URI
import data_structures.OneOrMore
import java.nio.file.Paths
import scala.deriving.Mirror
import scala.language.implicitConversions


package object circe {
  implicit val customConfig: Configuration =
    Configuration.default.withDefaults.withStrictDecoding

  inline def deriveConfiguredDecoder[A](using inline A: Mirror.Of[A], inline configuration: Configuration) = ConfiguredDecoder.derived[A]
  inline def deriveConfiguredDecoderFullChecks[A](using inline A: Mirror.Of[A], inline configuration: Configuration): Decoder[A] = DeriveConfiguredDecoderFullChecks.deriveConfiguredDecoderFullChecks
  inline def deriveConfiguredEncoder[A](using inline A: Mirror.Of[A], inline configuration: Configuration) = ConfiguredEncoder.derived[A]
  inline def deriveConfiguredEncoderStrict[A](using inline A: Mirror.Of[A], inline configuration: Configuration) = DeriveConfiguredEncoderStrict.deriveConfiguredEncoderStrict

  // encoder and decoder for Either
  implicit def encodeEither[A,B](implicit ea: Encoder[A], eb: Encoder[B]): Encoder[Either[A,B]] = {
    _.fold(ea(_), eb(_))
  }

  implicit def decodeEither[A,B](implicit a: Decoder[A], b: Decoder[B]): Decoder[Either[A,B]] = {
    a either b
  }

  // encoder and decoder for OneOrMore
  implicit def encodeOneOrMore[A](implicit enc: Encoder[List[A]]): Encoder[OneOrMore[A]] = { 
    (oom: OneOrMore[A]) => if (oom == null) enc(Nil) else enc(oom.toList)
  }

  implicit def decodeOneOrMore[A](implicit da: Decoder[A], dl: Decoder[List[A]]): Decoder[OneOrMore[A]] = {
    val l: Decoder[OneOrMore[A]] = da.map(OneOrMore(_))
    val r: Decoder[OneOrMore[A]] = dl.map(OneOrMore(_: _*))
    l or r
  }

  // encoder and decoder for java.io.File
  implicit val encodeFile: Encoder[java.io.File] = Encoder.instance {
    file => Json.fromString(file.getPath)
  }
  implicit val decodeFile: Decoder[java.io.File] = Decoder.instance {
    cursor => cursor.value.as[String].map(new java.io.File(_))
  }

  // encoder and decoder for java.nio.file.Path
  implicit val encodePath: Encoder[java.nio.file.Path] = Encoder.instance {
    file => Json.fromString(file.toString)
  }
  implicit val decodePath: Decoder[java.nio.file.Path] = Decoder.instance {
    cursor => cursor.value.as[String].map(Paths.get(_))
  }

  // allow any type of base type to be interpreted as a string
  implicit val decodeStringLike: Decoder[String] =
    Decoder.decodeString or
      Decoder.decodeInt.map(_.toString) or
      Decoder.decodeBigInt.map(_.toString) or
      Decoder.decodeBigDecimal.map(_.toString) or
      Decoder.decodeBoolean.map(_.toString)
  
  // auto convert a json to a RichJson
  implicit def enrichJson(json: Json): RichJson = new RichJson(json)
  implicit def enrichJsonObject(jo: JsonObject): RichJsonObject = new RichJsonObject(jo)

  // auto convert a JMap to Json
  def JMap(fields: (String, Json)*): Json = {
    Json.fromJsonObject(
      JsonObject(fields: _*)
    )
  }

  def Yaml(str: String): Json = {
    io.circe.yaml.parser.parse(str).fold(throw _, a => a)
  }
}