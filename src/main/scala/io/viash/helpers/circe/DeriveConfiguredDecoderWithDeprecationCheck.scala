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

import io.viash.helpers.Logging

object DeriveConfiguredDecoderWithDeprecationCheck extends Logging {

  private def memberDeprecationCheck(name: String, history: List[CursorOp], T: Type): Unit = {
    val m = T.member(TermName(name))
    val schema = ParameterSchema(name, "", List.empty, m.annotations)
    val deprecated = schema.flatMap(_.deprecated)
    val removed = schema.flatMap(_.removed)
    if (deprecated.isDefined) {
      val d = deprecated.get
      val historyString = history.collect{ case df: CursorOp.DownField => df.k }.reverse.mkString(".")
      info(s"Warning: .$historyString.$name is deprecated: ${d.message} Deprecated since ${d.deprecation}, planned removal ${d.removal}.")
    }
    if (removed.isDefined) {
      val r = removed.get
      val historyString = history.collect{ case df: CursorOp.DownField => df.k }.reverse.mkString(".")
      info(s"Error: .$historyString.$name was removed: ${r.message} Initially deprecated ${r.deprecation}, removed ${r.removal}.")
    }
  }

  private def selfDeprecationCheck(T: Type): Unit = {
    val baseClass = T.baseClasses.head
    val name = baseClass.fullName.split('.').last
    val schema = ParameterSchema("", "", List.empty, baseClass.annotations)
    val deprecated = schema.flatMap(_.deprecated)
    val removed = schema.flatMap(_.removed)
    if (deprecated.isDefined) {
      val d = deprecated.get
      info(s"Warning: $name is deprecated: ${d.message} Deprecated since ${d.deprecation}, planned removal ${d.removal}.")
    }
    if (removed.isDefined) {
      val r = removed.get
      info(s"Error: $name was removed: ${r.message} Initially deprecated ${r.deprecation}, removed ${r.removal}.")
    }
  }

  // 
  def checkDeprecation[A](cursor: ACursor)(implicit tag: TypeTag[A]) : ACursor = {
    selfDeprecationCheck(typeOf[A])
    // check each defined 'key' value
    for (key <- cursor.keys.getOrElse(Nil)) {
      val isEmpty = 
        cursor.downField(key).focus.get match {
          case value if value.isNull => true
          case value if value.isArray => value.asArray.get.isEmpty
          case value if value.isObject => value.asObject.get.isEmpty
          case _ => false
        }
      if (!isEmpty) {
        memberDeprecationCheck(key, cursor.history, typeOf[A])
      }
    }
    cursor // return unchanged json info
  }

  // Use prepare to get raw json data to inspect used fields in the json but we're not performing any changes here
  def deriveConfiguredDecoderWithDeprecationCheck[A](implicit decode: Lazy[ConfiguredDecoder[A]], tag: TypeTag[A]): Decoder[A] = deriveConfiguredDecoder[A]
    .prepare( checkDeprecation[A] )
}
