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

package com.dataintuitive.viash.helpers

import io.circe.{Printer => JsonPrinter}
import io.circe.syntax.EncoderOps
import com.dataintuitive.viash.helpers.Circe._

import io.circe.Encoder
import io.circe.generic.extras.semiauto.deriveConfiguredEncoder
import com.dataintuitive.viash.functionality.Functionality

final case class CollectedSchemas (
  // functionality
  functionality: List[ParameterSchema],
  // platforms
  nativePlatform: List[ParameterSchema],
  dockerPlatform: List[ParameterSchema],
  nextflowLegacyPlatform: List[ParameterSchema],
  // platform requirements
  apkRequirements: List[ParameterSchema],
  aptRequirements: List[ParameterSchema],
  dockerRequirements: List[ParameterSchema],
  javascriptRequirements: List[ParameterSchema],
  pythonRequirements: List[ParameterSchema],
  rRequirements: List[ParameterSchema],
  rubyRequirements: List[ParameterSchema],
  yumRequirements: List[ParameterSchema],
)

final case class ParameterSchema(
  name: String,
  `type`: String,
  descripton: Option[String],
  example: List[ExampleSchema],
  since: Option[String],
  deprecated: Option[DeprecatedOrRemovedSchema],
  removed: Option[DeprecatedOrRemovedSchema],
)

final case class DeprecatedOrRemovedSchema(
  message: String,
  since: String,
)

final case class ExampleSchema(
  example: String,
  format: String,
)

object CollectedSchemas {
  import scala.reflect.runtime.universe._

  private val jsonPrinter = JsonPrinter.spaces2.copy(dropNullValues = true)

  implicit val encodeConfigSchema: Encoder.AsObject[CollectedSchemas] = deriveConfiguredEncoder
  implicit val encodeParameterSchema: Encoder.AsObject[ParameterSchema] = deriveConfiguredEncoder
  implicit val encodeDeprecatedOrRemoved: Encoder.AsObject[DeprecatedOrRemovedSchema] = deriveConfiguredEncoder
  implicit val encodeExample: Encoder.AsObject[ExampleSchema] = deriveConfiguredEncoder

  def export() {

    def unfinishedStringStripMargin(s: Any, marginChar: Char = '|'): String = 
      s.toString().replaceAll("\\\\n", "\n").stripMargin(marginChar).replaceAll("\n", "\\\\n")

    def annotationToStrings(ann: Annotation):(String, List[String]) = {
      val name = ann.tree.tpe.toString()
      val values = ann.tree match {
        case Apply(c, args: List[Tree]) =>
          args.collect({
            case i: Tree =>
              i match {
                // Here 'Apply' contains lists
                // While 'Select' has a single element
                case Literal(Constant(value)) =>
                  value.toString()
                case Select(Select(a, b), stripMargin) =>
                  unfinishedStringStripMargin(b)
                case Select(Apply(a, b), stripMargin) =>
                  b.map(unfinishedStringStripMargin(_)).mkString
                case Apply(Select(Apply(a, a2), b), stripMargin) =>
                  val stripper = stripMargin.head.toString.charAt(1)
                  a2.map(unfinishedStringStripMargin(_, stripper)).mkString
                case _ =>
                  i.toString()
              }
          })
      }

      (name, values)
    }
    
    def annotationsOf[T: TypeTag](obj: T) = {
      val annMembers = typeOf[T].members.map(x => (x.fullName, x.info.toString(), x.annotations)).filter(_._3.length > 0)
      val annThis = ("__this__", typeOf[T].toString(), typeOf[T].typeSymbol.annotations)
      val allAnnotations = annThis :: annMembers.toList
      // filter out any information not from our own class and lazy evaluators (we'll use the standard one - otherwise double info and more complex)
      allAnnotations.filter(a => a._1.startsWith("com.dataintuitive.viash") || a._1 == "__this__").filter(!_._2.startsWith("=> "))
    }

    def annotationsToSchema(annotations: Iterable[(String, String, List[Annotation])]) = {
      annotations.map(a => {
        val name = a._1.split('.').last // format is e.g. "com.dataintuitive.viash.functionality.Functionality.name", only keep "name"
        val `type` = a._2
        val annStrings = a._3.map(annotationToStrings(_))

        val description = annStrings.collectFirst({case (name, value) if name.endsWith("description") => value.head})
        val example = annStrings.collect({case (name, value) if name.endsWith("example") => value}).map(l => ExampleSchema(l(0), l(1)))
        val since = annStrings.collectFirst({case (name, value) if name.endsWith("since") => value.head})
        val deprecated = annStrings.collectFirst({case (name, value) if name.endsWith("deprecated") => value}).map(l => DeprecatedOrRemovedSchema(l(0), l(1)))
        val removed = annStrings.collectFirst({case (name, value) if name.endsWith("removed") => value}).map(l => DeprecatedOrRemovedSchema(l(0), l(1)))
        ParameterSchema(name, `type`, description, example, since, deprecated, removed)
      }).toList
    }

    val fun = com.dataintuitive.viash.functionality.Functionality("foo")
    val funSchema = annotationsToSchema( annotationsOf(fun) )

    val nativePlat = com.dataintuitive.viash.platforms.NativePlatform()
    val nativeSchema = annotationsToSchema( annotationsOf(nativePlat) )

    val dockerPlat = com.dataintuitive.viash.platforms.DockerPlatform("", "", None)
    val dockerSchema = annotationsToSchema( annotationsOf(dockerPlat) )

    val nextflowLegacyPlat = com.dataintuitive.viash.platforms.NextflowLegacyPlatform("", None)
    val nextflowLegacyPlatSchema = annotationsToSchema( annotationsOf(nextflowLegacyPlat) )

    val apkReq = com.dataintuitive.viash.platforms.requirements.ApkRequirements()
    val apkReqSchema = annotationsToSchema( annotationsOf(apkReq) )

    val aptReq = com.dataintuitive.viash.platforms.requirements.AptRequirements()
    val aptReqSchema = annotationsToSchema( annotationsOf(aptReq) )

    val dockerReq = com.dataintuitive.viash.platforms.requirements.DockerRequirements()
    val dockerReqSchema = annotationsToSchema( annotationsOf(dockerReq) )

    val javascriptReq = com.dataintuitive.viash.platforms.requirements.JavaScriptRequirements()
    val javascriptReqSchema = annotationsToSchema( annotationsOf(javascriptReq) )

    val pythonReq = com.dataintuitive.viash.platforms.requirements.PythonRequirements()
    val pythonReqSchema = annotationsToSchema( annotationsOf(pythonReq) )

    val rReq = com.dataintuitive.viash.platforms.requirements.RRequirements()
    val rReqSchema = annotationsToSchema( annotationsOf(rReq) )

    val rubyReq = com.dataintuitive.viash.platforms.requirements.RubyRequirements()
    val rubyReqSchema = annotationsToSchema( annotationsOf(rubyReq) )

    val yumReq = com.dataintuitive.viash.platforms.requirements.YumRequirements()
    val yumReqSchema = annotationsToSchema( annotationsOf(yumReq) )

    val data = CollectedSchemas(
      functionality = funSchema,
      nativePlatform = nativeSchema,
      dockerPlatform = dockerSchema,
      nextflowLegacyPlatform = nextflowLegacyPlatSchema,
      apkRequirements = apkReqSchema,
      aptRequirements = aptReqSchema,
      dockerRequirements = dockerReqSchema,
      javascriptRequirements = javascriptReqSchema,
      pythonRequirements = pythonReqSchema,
      rRequirements = rReqSchema,
      rubyRequirements = rubyReqSchema,
      yumRequirements = yumReqSchema,
    )
    val str = jsonPrinter.print(data.asJson)
    println(str)
  }
}
