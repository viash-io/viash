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

  private def getMembers[T: TypeTag](): (Map[String,List[(String, Symbol, String, Int)]], List[Symbol]) = {
    val name = typeOf[T].typeSymbol.fullName
    val memberNames = typeOf[T].members
      .filter(!_.isMethod)
      .map(_.fullName.split('.').last)
      .toSeq

    val baseClasses = typeOf[T].baseClasses
      .filter(_.fullName.startsWith("io.viash"))

    val allMembers = baseClasses
      .zipWithIndex
      .flatMap(x =>
        x._1.info.members
          .filter(_.fullName.startsWith("io.viash"))
          .filter(m => memberNames.contains(m.fullName.split('.').last))
          .filter(m => !m.info.toString.startsWith("=> ") || x._2 != 0) // Only regular members if base class, otherwise all members
          .map(y => (y.fullName, y, x._1.fullName, x._2)) // member name, member class, class name, inheritance index
      )
      .groupBy(k => k._1.split('.').last)
    
    (allMembers, baseClasses)
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

  private def annotationsOf(members: (Map[String,List[(String, Symbol, String, Int)]]), classes: List[Symbol]) = {
    val annMembers = members
      .map{ case (memberName, memberInfo) => { 
        val h = memberInfo.head
        val annotations = memberInfo.flatMap(_._2.annotations)
        (h._1, h._2.info.toString, annotations, h._3, h._4, Nil)
      } }
      .filter(_._3.length > 0)
    val annThis = ("__this__", classes.head.name.toString(), classes.head.annotations, "", 0, classes.map(_.fullName))
    val allAnnotations = annThis :: annMembers.toList
    allAnnotations
      .map({case (a, b, c, d, e, f) => (a, trimTypeName(b), f, c)})  // TODO this ignores where the annotation was defined, ie. top level class or super class
  }

  private val getSchema = (t: (Map[String,List[(String, Symbol, String, Int)]], List[Symbol])) => t match {
    case (members, classes) => {
      annotationsOf(members, classes).flatMap{ case (a, b, c, d) => ParameterSchema(a, b, c, d) }
    }
  }

  def getJson: Json = {
    val data = CollectedSchemas(
      functionality = getSchema(schemaClassMap.get("functionality").get("")),
      platforms = schemaClassMap.get("platforms").get.map{ case(k, v) => (k, getSchema(v))},
      requirements = schemaClassMap.get("requirements").get.map{ case(k, v) => (k, getSchema(v))},
      arguments = schemaClassMap.get("arguments").get.map{ case(k, v) => (k, getSchema(v))}
    )
    data.asJson
  }

  private def getNonAnnotated(members: Map[String,List[(String, Symbol, String, Int)]], classes: List[Symbol]) = {
    val issueMembers = members
      .toList
      .map{ case (k, v) => (k, v.map(_._2.annotations.length).sum) } // (name, # annotations)
      .filter(_._2 == 0)
      .map(_._1)

    val ownClassArr = if (classes.head.annotations.length == 0) Seq("__this__") else Nil
    issueMembers ++ ownClassArr
  }

  def getAllNonAnnotated = schemaClassMap.flatMap {
    case (key, v1) => v1.flatMap {
      case (key2, v2) => getNonAnnotated(v2._1, v2._2).map((key, key2, _))
    }
  }

}
