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

import scala.reflect.runtime.universe._
import io.circe.{Printer => JsonPrinter}
import io.circe.syntax.EncoderOps
import io.viash.helpers.Circe._

import io.circe.Encoder
import io.circe.generic.extras.semiauto.deriveConfiguredEncoder
import io.viash.functionality.Functionality
import io.viash.platforms._
import io.viash.platforms.requirements._
import io.viash.functionality.arguments._

final case class CollectedSchemas (
  functionality: List[ParameterSchema],
  platforms: Map[String, List[ParameterSchema]],
  requirements: Map[String, List[ParameterSchema]],
  arguments: Map[String, List[ParameterSchema]],
)

final case class ParameterSchema(
  name: String,
  `type`: String,
  description: Option[String],
  example: List[ExampleSchema],
  since: Option[String],
  deprecated: Option[DeprecatedOrRemovedSchema],
  removed: Option[DeprecatedOrRemovedSchema],
)

object ParameterSchema {
  // Aid processing `augmentString` strings
  private def unfinishedStringStripMargin(s: String, marginChar: Char = '|'): String = {
    s.replaceAll("\\\\n", "\n").stripMargin(marginChar)
  }

  private def mapTreeList(l: List[Tree], marginChar: Char = '|'): String = {
    l.map(i => i match {
      case Literal(Constant(value: String)) =>
        unfinishedStringStripMargin(value, marginChar)
      case _ =>
        "unmatched in mapTreeList: " + i.toString()
    }).mkString
  }

  // Traverse tree information and extract values or lists of values
  private def annotationToStrings(ann: Annotation):(String, List[String]) = {
    val name = ann.tree.tpe.toString()
    val values = ann.tree match {
      case Apply(c, args: List[Tree]) =>
        args.collect({
          case i: Tree =>
            i match {
              // Here 'Apply' contains lists
              // While 'Select' has a single element
              case Literal(Constant(value: String)) =>
                value
              // case Select(Select(a, b), stripMargin) =>
              //   unfinishedStringStripMargin(b)
              case Select(Apply(a, a2), b) if b.toString == "stripMargin" =>
                mapTreeList(a2)
              case Apply(Select(Apply(a, a2), b), stripMargin) if b.toString == "stripMargin" =>
                val stripper = stripMargin.head.toString.charAt(1)
                mapTreeList(a2, stripper)
              case _ =>
                "unmatched in annotationToStrings: " + i.toString()
            }
        })
    }
    (name, values)
  }

  def apply(name: String, `type`: String, annotations: List[Annotation]): ParameterSchema = {
    // name is e.g. "io.viash.functionality.Functionality.name", only keep "name"
    // name can also be "__this__"
    val name_ = name.split('.').last
    val annStrings = annotations.map(annotationToStrings(_))

    val description = annStrings.collectFirst({case (name, value) if name.endsWith("description") => value.head})
    val example = annStrings.collect({case (name, value) if name.endsWith("example") => value}).map(ExampleSchema(_))
    val since = annStrings.collectFirst({case (name, value) if name.endsWith("since") => value.head})
    val deprecated = annStrings.collectFirst({case (name, value) if name.endsWith("deprecated") => value}).map(DeprecatedOrRemovedSchema(_))
    val removed = annStrings.collectFirst({case (name, value) if name.endsWith("removed") => value}).map(DeprecatedOrRemovedSchema(_))
    ParameterSchema(name_, `type`, description, example, since, deprecated, removed)
  }
}

final case class DeprecatedOrRemovedSchema(
  message: String,
  since: String,
)

object DeprecatedOrRemovedSchema {
  def apply(l: List[String]): DeprecatedOrRemovedSchema = {
    DeprecatedOrRemovedSchema(l(0), l(1))
  }
}

final case class ExampleSchema(
  example: String,
  format: String,
)

object ExampleSchema {
  def apply(l: List[String]): ExampleSchema = {
    ExampleSchema(l(0), l(1))
  }
}

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
      annotationsOf[T].map({case (a, b, c) => ParameterSchema(a, b, c)})
  }

  def export() {
    val data = CollectedSchemas(
      functionality = getSchema[Functionality],
      platforms = Map(
        ("nativePlatform"        , getSchema[NativePlatform]),
        ("dockerPlatform"        , getSchema[DockerPlatform]),
        ("nextflowLegacyPlatform", getSchema[NextflowLegacyPlatform])
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
    val str = jsonPrinter.print(data.asJson)
    println(str)
  }
}
