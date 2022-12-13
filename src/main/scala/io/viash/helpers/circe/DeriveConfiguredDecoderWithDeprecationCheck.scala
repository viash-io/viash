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

package io.viash.helpers.circe

import io.circe.generic.extras.semiauto.deriveConfiguredDecoder
import io.circe.{ Decoder, CursorOp }
import io.circe.generic.extras.decoding.ConfiguredDecoder

import scala.reflect.runtime.universe._
import shapeless.Lazy

import io.viash.schemas.ParameterSchema
import io.circe.ACursor

object DeriveConfiguredDecoderWithDeprecationCheck {

  private def memberDeprecationCheck(name: String, history: List[CursorOp], T: Type) {
    val m = T.member(TermName(name))
    val schema = ParameterSchema(name, "", List.empty, m.annotations)
    val deprecated = schema.flatMap(_.deprecated)
    if (deprecated.isDefined) {
      val d = deprecated.get
      val historyString = history.collect{ case df: CursorOp.DownField => df.k }.reverse.mkString(".")

      Console.err.println(s"Warning: .$historyString.$name is deprecated: ${d.message} Deprecated since ${d.since}")
    }
  }

  // 
  def checkDeprecation[A](a: ACursor)(implicit tag: TypeTag[A]) : ACursor = {
    // check each defined 'key' value
    a.keys match {
      case Some(s) => s.foreach(memberDeprecationCheck(_, a.history, typeOf[A]))
      case _ =>
    }
    a // return unchanged json info
  }

  // Use prepare to get raw json data to inspect used fields in the json but we're not performing any changes here
  def deriveConfiguredDecoderWithDeprecationCheck[A](implicit decode: Lazy[ConfiguredDecoder[A]], tag: TypeTag[A]): Decoder[A] = deriveConfiguredDecoder[A]
    .prepare( checkDeprecation[A] )
}
