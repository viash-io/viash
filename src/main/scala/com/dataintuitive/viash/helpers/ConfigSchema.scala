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

final case class ConfigSchema (
  functionality: List[ParameterSchema]

)

final case class ParameterSchema(
  name: String,
  `type`: String,
  descripton: Option[String],
  example: List[String],
  since: Option[String],
  deprecated: Option[DeprecatedOrRemoved],
  removed: Option[DeprecatedOrRemoved],
)

final case class DeprecatedOrRemoved(
  message: String,
  since: String
)

object ConfigSchema {
  import scala.reflect.runtime.universe

  private val jsonPrinter = JsonPrinter.spaces2.copy(dropNullValues = true)

  implicit val encodeConfigSchema: Encoder.AsObject[ConfigSchema] = deriveConfiguredEncoder
  implicit val encodeParameterSchema: Encoder.AsObject[ParameterSchema] = deriveConfiguredEncoder
  implicit val encodeDeprecatedOrRemoved: Encoder.AsObject[DeprecatedOrRemoved] = deriveConfiguredEncoder

  def export() {
    
    def annotationsOf[T: universe.TypeTag](obj: T) = {
      // universe.typeOf[T].members.foldLeft(Nil: List[universe.type#Annotation]) {
      //     (xs, x) => x.annotations ::: xs
      // }
      universe.typeOf[T].members.map(x => (x.fullName, x.annotations)).filter(_._2.length > 0)
    }
    val fun = com.dataintuitive.viash.functionality.Functionality("foo")
    //annotationsOf(fun).foreach(t => println(s"${t._1} -> ${t._2}"))
    val annotations = annotationsOf(fun).filter(_._1.startsWith("com.dataintuitive.viash"))
    annotations.foreach(t => println(s"${t._1} -> ${t._2}"))
    val funSchema = annotations.map(a => ParameterSchema(a._1.toString(), "foo", None, a._2.map(_.toString()), None, None, None)).toList


    val data = ConfigSchema(funSchema)
    val str = jsonPrinter.print(funSchema.asJson)
    println(str)
  }
}
