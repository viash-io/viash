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
import monocle.function.Cons

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

  private def getMembers[T: TypeTag]() = {
    (typeOf[T].members, typeOf[T].typeSymbol)
  }

  val schemaClassMap = Map(
    ("functionality", Map(
      (""                       , getMembers[Functionality]))),
    ("platforms", Map(
      ("nativePlatform"         , getMembers[NativePlatform]),
      ("dockerPlatform"         , getMembers[DockerPlatform]),
      ("nextflowVdsl3Platform"  , getMembers[NextflowVdsl3Platform]),
      ("nextflowLegacyPlatform" , getMembers[NextflowLegacyPlatform]),
    )),
    ("requirements", Map(
      ("apkRequirements"        , getMembers[ApkRequirements]),
      ("aptRequirements"        , getMembers[AptRequirements]),
      ("dockerRequirements"     , getMembers[DockerRequirements]),
      ("javascriptRequirements" , getMembers[JavaScriptRequirements]),
      ("pythonRequirements"     , getMembers[PythonRequirements]),
      ("rRequirements"          , getMembers[RRequirements]),
      ("rubyRequirements"       , getMembers[RubyRequirements]),
      ("yumRequirements"        , getMembers[YumRequirements]),
    )),
    ("arguments", Map(
      ("boolean"                , getMembers[BooleanArgument]),
      ("boolean_true"           , getMembers[BooleanTrueArgument]),
      ("boolean_false"          , getMembers[BooleanFalseArgument]),
      ("double"                 , getMembers[DoubleArgument]),
      ("file"                   , getMembers[FileArgument]),
      ("integer"                , getMembers[IntegerArgument]),
      ("long"                   , getMembers[LongArgument]),
      ("string"                 , getMembers[StringArgument]),
    ))
  )

  private def trimTypeName(s: String) = {
    // first: io.viash.helpers.Circe.OneOrMore[String] -> OneOrMore[String]
    // second: List[io.viash.platforms.requirements.Requirements] -> List[Requirements]
    s.replaceAll("^(\\w*\\.)*", "").replaceAll("""(\w*)\[[\w\.]*?([\w,]*)(\[_\])?\]""", "$1 of $2")
  }

  private def annotationsOf(members: MemberScope, typeSymbol: Symbol) = {
    val annMembers = members.map(x => (x.fullName, x.info.toString(), x.annotations)).filter(_._3.length > 0)
    val annThis = ("__this__", typeSymbol.name.toString(), typeSymbol.annotations)
    val allAnnotations = annThis :: annMembers.toList
    // filter out any information not from our own class and lazy evaluators (we'll use the standard one - otherwise double info and more complex)
    allAnnotations
      .filter(a => a._1.startsWith("io.viash") || a._1 == "__this__")
      .filter(!_._2.startsWith("=> "))
      .map({case (a, b, c) => (a, trimTypeName(b), c)})
  }

  private val getSchema_t = (t: (MemberScope, Symbol)) => t match {
    case (members: MemberScope, typeSymbol: Symbol) => {
      annotationsOf(members, typeSymbol).map({case (a, b, c) => ParameterSchema(a, b, c)})
    }
  }

  def getJson: Json = {
    val data = CollectedSchemas(
      functionality = getSchema_t(schemaClassMap.get("functionality").get("")),
      platforms = schemaClassMap.get("platforms").get.map{ case(k, v) => (k, getSchema_t(v))},
      requirements = schemaClassMap.get("requirements").get.map{ case(k, v) => (k, getSchema_t(v))},
      arguments = schemaClassMap.get("arguments").get.map{ case(k, v) => (k, getSchema_t(v))}
    )
    data.asJson
  }

  private def getNonAnnotated(members: MemberScope) = {
    members
      .filter(!_.isMethod) // only check values
      .filter(_.fullName.startsWith("io.viash")) // only check self-defined values, not inherited class members
      .filter(_.annotations.length == 0)
      
      .map(_.fullName)
  }

  // schemaClassMap.keys.foreach(key =>
  //   schemaClassMap.get(key).get.map{ 
  //     case(key2, v) => 
  //       val warnings = getNonAnnotated(v._1)
  //       warnings.foreach(w => Console.println(s"$key - $key2 - $w"))
  //     }
  // )
}
