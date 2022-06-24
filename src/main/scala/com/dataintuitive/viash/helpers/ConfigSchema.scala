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

final case class ConfigSchema (
  functionality: List[ParameterSchema]

)

final case class ParameterSchema(
  name: String,
  `type`: String,
  descripton: Option[String],
  example: List[Example],
  since: Option[String],
  deprecated: Option[DeprecatedOrRemoved],
  removed: Option[DeprecatedOrRemoved],
)

final case class DeprecatedOrRemoved(
  message: String,
  since: String,
)

final case class Example(
  example: String,
  format: String,
)

object ConfigSchema {
  import scala.reflect.runtime.universe._

  private val jsonPrinter = JsonPrinter.spaces2.copy(dropNullValues = true)

  implicit val encodeConfigSchema: Encoder.AsObject[ConfigSchema] = deriveConfiguredEncoder
  implicit val encodeParameterSchema: Encoder.AsObject[ParameterSchema] = deriveConfiguredEncoder
  implicit val encodeDeprecatedOrRemoved: Encoder.AsObject[DeprecatedOrRemoved] = deriveConfiguredEncoder
  implicit val encodeExample: Encoder.AsObject[Example] = deriveConfiguredEncoder

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
      typeOf[T].members.map(x => (x.fullName, x.info.toString(), x.annotations)).filter(_._3.length > 0)
    }

    val fun = com.dataintuitive.viash.functionality.Functionality("foo")
    // filter out any information not from our own class and lazy evaluators (we'll use the standard one - otherwise double info and more complex)
    val annotations = annotationsOf(fun).filter(_._1.startsWith("com.dataintuitive.viash")).filter(!_._2.startsWith("=> "))
    val funSchema = annotations.map(a => {
      val name = a._1.replaceFirst("com.dataintuitive.viash.functionality.Functionality.", "")
      val `type` = a._2
      val annStrings = a._3.map(annotationToStrings(_))

      val description = annStrings.collectFirst({case (name, value) if name.endsWith("description") => value.head})
      // TODO handle example.format correctly
      val example = annStrings.collect({case (name, value) if name.endsWith("example") => value}).map(l => Example(l(0), l(1)))
      val since = annStrings.collectFirst({case (name, value) if name.endsWith("since") => value.head})
      val deprecated = annStrings.collectFirst({case (name, value) if name.endsWith("deprecated") => value}).map(l => DeprecatedOrRemoved(l(0), l(1)))
      val removed = annStrings.collectFirst({case (name, value) if name.endsWith("removed") => value}).map(l => DeprecatedOrRemoved(l(0), l(1)))
      ParameterSchema(name, `type`, description, example, since, deprecated, removed)
    }).toList


    val data = ConfigSchema(funSchema)
    val str = jsonPrinter.print(data.asJson)
    println(str)
  }
}
