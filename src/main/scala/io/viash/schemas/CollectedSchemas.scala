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

import io.viash.functionality.Functionality
import io.viash.platforms._
import io.viash.platforms.requirements._
import io.viash.functionality.arguments._
import io.circe.Json
import monocle.function.Cons
import io.viash.config.Config
import io.viash.config.Info
import io.viash.functionality.resources._
import io.viash.project.ViashProject

final case class CollectedSchemas (
  config: List[ParameterSchema],
  functionality: List[ParameterSchema],
  platforms: Map[String, List[ParameterSchema]],
  requirements: Map[String, List[ParameterSchema]],
  arguments: Map[String, List[ParameterSchema]],
  resources: Map[String, List[ParameterSchema]],
  project: List[ParameterSchema],
)


object CollectedSchemas {

  implicit class RichSymbol(s: Symbol) {
    def shortName = s.fullName.split('.').last
  }

  case class MemberInfo (
    symbol: Symbol,
    inConstructor: Boolean,
    className: String,
    inheritanceIndex: Int
  ) {
    def fullName = symbol.fullName
    def shortName = symbol.shortName
  }

  private val jsonPrinter = JsonPrinter.spaces2.copy(dropNullValues = true)

  import io.viash.helpers.circe._

  private implicit val encodeConfigSchema: Encoder.AsObject[CollectedSchemas] = deriveConfiguredEncoder
  private implicit val encodeParameterSchema: Encoder.AsObject[ParameterSchema] = deriveConfiguredEncoder
  private implicit val encodeDeprecatedOrRemoved: Encoder.AsObject[DeprecatedOrRemovedSchema] = deriveConfiguredEncoder
  private implicit val encodeExample: Encoder.AsObject[ExampleSchema] = deriveConfiguredEncoder

  private def getMembers[T: TypeTag](): (Map[String,List[MemberInfo]], List[Symbol]) = {

    val name = typeOf[T].typeSymbol.fullName
    val memberNames = typeOf[T].members
      .filter(!_.isMethod)
      .map(_.shortName)
      .toSeq

    val constructorMembers = typeOf[T].members.filter(_.isConstructor).head.asMethod.paramLists.head.map(_.shortName)

    val baseClasses = typeOf[T].baseClasses
      .filter(_.fullName.startsWith("io.viash"))

    val allMembers = baseClasses
      .zipWithIndex
      .flatMap{ case (baseClass, index) =>
        baseClass.info.members
          .filter(_.fullName.startsWith("io.viash"))
          .filter(m => memberNames.contains(m.shortName))
          .filter(m => !m.info.getClass.toString.endsWith("NullaryMethodType") || index != 0) // Only regular members if base class, otherwise all members
          .map(y => MemberInfo(y, (constructorMembers.contains(y.shortName)), baseClass.fullName, index))
        }
      .groupBy(k => k.shortName)
    
    (allMembers, baseClasses)
  }

  val schemaClassMap = Map(
    "config" -> Map(
      ""                       -> getMembers[Config](),
    ),
    "functionality" -> Map(
      ""                       -> getMembers[Functionality]()
    ),
    "platforms" -> Map(
      "nativePlatform"         -> getMembers[NativePlatform](),
      "dockerPlatform"         -> getMembers[DockerPlatform](),
      "nextflowVdsl3Platform"  -> getMembers[NextflowVdsl3Platform](),
      "nextflowLegacyPlatform" -> getMembers[NextflowLegacyPlatform](),
    ),
    "requirements" -> Map(
      "apkRequirements"        -> getMembers[ApkRequirements](),
      "aptRequirements"        -> getMembers[AptRequirements](),
      "dockerRequirements"     -> getMembers[DockerRequirements](),
      "javascriptRequirements" -> getMembers[JavaScriptRequirements](),
      "pythonRequirements"     -> getMembers[PythonRequirements](),
      "rRequirements"          -> getMembers[RRequirements](),
      "rubyRequirements"       -> getMembers[RubyRequirements](),
      "yumRequirements"        -> getMembers[YumRequirements](),
    ),
    "arguments" -> Map(
      "argument"               -> getMembers[Argument[_]](),
      "boolean"                -> getMembers[BooleanArgument](),
      "boolean_true"           -> getMembers[BooleanTrueArgument](),
      "boolean_false"          -> getMembers[BooleanFalseArgument](),
      "double"                 -> getMembers[DoubleArgument](),
      "file"                   -> getMembers[FileArgument](),
      "integer"                -> getMembers[IntegerArgument](),
      "long"                   -> getMembers[LongArgument](),
      "string"                 -> getMembers[StringArgument](),
    ),
    "resources" -> Map(
      "resource"               -> getMembers[Resource](),
      "bashScript"             -> getMembers[BashScript](),
      "cSharpScript"           -> getMembers[CSharpScript](),
      "executable"             -> getMembers[Executable](),
      "javaScriptScript"       -> getMembers[JavaScriptScript](),
      "nextflowScript"         -> getMembers[NextflowScript](),
      "plainFile"              -> getMembers[PlainFile](),
      "pythonScript"           -> getMembers[PythonScript](),
      "rScript"                -> getMembers[RScript](),
      "scalaScript"            -> getMembers[ScalaScript](),
    ),
    "project" -> Map(
      ""                       -> getMembers[ViashProject](),
    )
  )

  private def trimTypeName(s: String) = {
    // first: io.viash.helpers.data_structures.OneOrMore[String] -> OneOrMore[String]
    // second: List[io.viash.platforms.requirements.Requirements] -> List[Requirements]
    s.replaceAll("^(\\w*\\.)*", "").replaceAll("""(\w*)\[[\w\.]*?([\w,]*)(\[_\])?\]""", "$1 of $2")
  }

  private def annotationsOf(members: (Map[String,List[MemberInfo]]), classes: List[Symbol]) = {
    val annMembers = members
      .map{ case (memberName, memberInfo) => { 
        val h = memberInfo.head
        val annotations = memberInfo.flatMap(_.symbol.annotations)
        (h.fullName, h.symbol.info.toString, annotations, h.className, h.inheritanceIndex, Nil)
      } }
      .filter(_._3.length > 0)
    val annThis = ("__this__", classes.head.name.toString(), classes.head.annotations, "", 0, classes.map(_.fullName))
    val allAnnotations = annThis :: annMembers.toList
    allAnnotations
      .map({case (a, b, c, d, e, f) => (a, trimTypeName(b), f, c)})  // TODO this ignores where the annotation was defined, ie. top level class or super class
  }

  private val getSchema = (t: (Map[String,List[MemberInfo]], List[Symbol])) => t match {
    case (members, classes) => {
      annotationsOf(members, classes).flatMap{ case (a, b, c, d) => ParameterSchema(a, b, c, d) }
    }
  }

  // Main call for documentation output
  private lazy val data = CollectedSchemas(
      config = getSchema(schemaClassMap.get("config").get("")),
      functionality = getSchema(schemaClassMap.get("functionality").get("")),
      platforms = schemaClassMap.get("platforms").get.map{ case(k, v) => (k, getSchema(v))},
      requirements = schemaClassMap.get("requirements").get.map{ case(k, v) => (k, getSchema(v))},
      arguments = schemaClassMap.get("arguments").get.map{ case(k, v) => (k, getSchema(v))},
      resources = schemaClassMap.get("resources").get.map{ case(k, v) => (k, getSchema(v))},
      project = getSchema(schemaClassMap.get("project").get("")),
    )

  def getJson: Json = {
    data.asJson
  }

  private def getNonAnnotated(members: Map[String,List[MemberInfo]], classes: List[Symbol]) = {
    val issueMembers = members
      .toList
      .filter{ case(k, v) => v.map(m => m.inConstructor).contains(true) } // Only check values that are in a constructor. Annotation may occur on private vals but that is not a requirement.
      .map{ case (k, v) => (k, v.map(_.symbol.annotations.length).sum) } // (name, # annotations)
      .filter(_._2 == 0)
      .map(_._1)

    val ownClassArr = if (classes.head.annotations.length == 0) Seq("__this__") else Nil
    issueMembers ++ ownClassArr
  }

  // Main call for checking whether all arguments are annotated
  def getAllNonAnnotated = schemaClassMap.flatMap {
    case (key, v1) => v1.flatMap {
      case (key2, v2) => getNonAnnotated(v2._1, v2._2).map((key, key2, _))
    }
  }

  def getAllDeprecations = {
    val arr =
      data.config.map(c => ("config " + c.name, c.deprecated)) ++
      data.functionality.map(f => ("functionality " + f.name, f.deprecated)) ++
      data.platforms.flatMap{ case (key, v) => v.map(v2 => ("platforms " + key + " " + v2.name, v2.deprecated)) } ++ 
      data.requirements.flatMap{ case (key, v) => v.map(v2 => ("requirements " + key + " " + v2.name, v2.deprecated)) } ++
      data.arguments.flatMap{ case (key, v) => v.map(v2 => ("arguments " + key + " " + v2.name, v2.deprecated)) }
    
    arr.filter(t => t._2.isDefined).map(t => (t._1, t._2.get))
  }

}
