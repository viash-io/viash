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

package io.viash.schemas

import scala.reflect.runtime.universe._
import io.circe.{Encoder, Printer => JsonPrinter}
import io.circe.syntax.EncoderOps
import io.circe.generic.extras.semiauto.deriveConfiguredEncoder
import io.viash.helpers.Circe._

import io.viash.functionality.Functionality
import io.viash.platforms._
import io.viash.platforms.requirements._
import io.viash.functionality.arguments._
import io.circe.Json

final case class CollectedSchemas (
  functionality: List[ParameterSchema],
  platforms: Map[String, List[ParameterSchema]],
  requirements: Map[String, List[ParameterSchema]],
  arguments: Map[String, List[ParameterSchema]],
)

object CollectedSchemas {
  private val jsonPrinter = JsonPrinter.spaces2.copy(dropNullValues = true)

  private implicit val encodeConfigSchema: Encoder.AsObject[CollectedSchemas] = deriveConfiguredEncoder
  private implicit val encodeParameterSchema: Encoder.AsObject[ParameterSchema] = deriveConfiguredEncoder
  private implicit val encodeDeprecatedOrRemoved: Encoder.AsObject[DeprecatedOrRemovedSchema] = deriveConfiguredEncoder
  private implicit val encodeExample: Encoder.AsObject[ExampleSchema] = deriveConfiguredEncoder

  private def trimTypeName(s: String) = {
    // first: io.viash.helpers.Circe.OneOrMore[String] -> OneOrMore[String]
    // second: List[io.viash.platforms.requirements.Requirements] -> List[Requirements]
    s.replaceAll("^(\\w*\\.)*", "").replaceAll("""(\w*)\[[\w\.]*?([\w,]*)(\[_\])?\]""", "$1 of $2")
  }

  private def annotationsOf[T: TypeTag]() = {
    val annMembers = typeOf[T].members.map(x => (x.fullName, x.info.toString(), x.annotations)).filter(_._3.length > 0)
    val annThis = ("__this__", typeOf[T].typeSymbol.name.toString(), typeOf[T].typeSymbol.annotations)
    val allAnnotations = annThis :: annMembers.toList
    // filter out any information not from our own class and lazy evaluators (we'll use the standard one - otherwise double info and more complex)
    allAnnotations
      .filter(a => a._1.startsWith("io.viash") || a._1 == "__this__")
      .filter(!_._2.startsWith("=> "))
      .map({case (a, b, c) => (a, trimTypeName(b), c)})
  }

  private def getSchema[T: TypeTag] = {
      annotationsOf[T]().map({case (a, b, c) => ParameterSchema(a, b, c)})
  }

  def getJson: Json = {
    val data = CollectedSchemas(
      functionality = getSchema[Functionality],
      platforms = Map(
        ("nativePlatform"        , getSchema[NativePlatform]),
        ("dockerPlatform"        , getSchema[DockerPlatform]),
        ("nextflowVdsl3Platform" , getSchema[NextflowVdsl3Platform]),
        ("nextflowLegacyPlatform", getSchema[NextflowLegacyPlatform]),
      ),
      requirements = Map(
        ("apkRequirements"        , getSchema[ApkRequirements]),
        ("aptRequirements"        , getSchema[AptRequirements]),
        ("dockerRequirements"     , getSchema[DockerRequirements]),
        ("javascriptRequirements" , getSchema[JavaScriptRequirements]),
        ("pythonRequirements"     , getSchema[PythonRequirements]),
        ("rRequirements"          , getSchema[RRequirements]),
        ("rubyRequirements"       , getSchema[RubyRequirements]),
        ("yumRequirements"        , getSchema[YumRequirements]),
      ),
      arguments = Map(
        ("boolean"                , getSchema[BooleanArgument]),
        ("boolean_true"           , getSchema[BooleanTrueArgument]),
        ("boolean_false"          , getSchema[BooleanFalseArgument]),
        ("double"                 , getSchema[DoubleArgument]),
        ("file"                   , getSchema[FileArgument]),
        ("integer"                , getSchema[IntegerArgument]),
        ("string"                 , getSchema[StringArgument]),
      ),
    )
    data.asJson
  }
}
