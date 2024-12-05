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

import io.circe.{ ACursor, Decoder, CursorOp }
import io.circe.derivation.{Configuration, ConfiguredDecoder}
import scala.deriving.Mirror

import io.viash.helpers.Logging
import io.viash.helpers.*

object DeriveConfiguredDecoderWithDeprecationCheck extends Logging {

  // This method doesn't use any mirroring, so it can be called from multiple inlined validators without needing inlining.
  private def memberDeprecationCheck(
    name: String,
    history: List[CursorOp],
    deprecated: Option[(String, String, String)],
    removed: Option[(String, String, String)],
    hasInternalFunctionality: Boolean
  ): Unit = {
    lazy val historyString = history.collect{ case df: CursorOp.DownField => df.k }.reverse.mkString(".")

    lazy val fullHistoryName = 
      if (historyString.isEmpty) {
        s".$name"
      } else {
        s".$historyString.$name"
      }

    deprecated match {
      case Some(d) =>
        info(s"Warning: $fullHistoryName is deprecated: ${d._1} Deprecated since ${d._2}, planned removal ${d._3}.")
      case _ =>
    }
    removed match {
      case Some(r) => 
        info(s"Error: $fullHistoryName was removed: ${r._1} Initially deprecated ${r._2}, removed ${r._3}.")
      case _ =>
    }
    if (hasInternalFunctionality) {
      error(s"Error: $fullHistoryName is internal functionality.")
      throw new RuntimeException(s"Internal functionality used: $fullHistoryName")
    }
  }

  private inline def selfDeprecationCheck[A]()(using inline A: Mirror.Of[A]): Unit = {
    val name = typeOf[A]
    val deprecated = deprecatedOf[A].headOption
    val removed = removedOf[A].headOption

    deprecated match {
      case Some(d) =>
        info(s"Warning: $name is deprecated: ${d._1} Deprecated since ${d._2}, planned removal ${d._3}.")
      case _ =>
    }
    removed match {
      case Some(r) =>
        info(s"Error: $name was removed: ${r._1} Initially deprecated ${r._2}, removed ${r._3}.")
      case _ =>
    }
  }

  inline def checkDeprecation[A](cursor: ACursor)(using inline A: Mirror.Of[A]) : ACursor = {

    selfDeprecationCheck()

    val df = deprecatedFieldsOf[A].map(t => t._1 -> (t._2, t._3, t._4)).toMap
    val rf = removedFieldsOf[A].map(t => t._1 -> (t._2, t._3, t._4)).toMap
    val iff = internalFunctionalityFieldsOf[A]

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
        memberDeprecationCheck(key, cursor.history, df.get(key), rf.get(key), iff.contains(key))
      }
    }
    cursor // return unchanged json info
  }

// Use prepare to get raw json data to inspect used fields in the json but we're not performing any changes here
  inline def deriveConfiguredDecoderWithDeprecationCheck[A](using inline A: Mirror.Of[A], inline configuration: Configuration) = deriveConfiguredDecoder[A]
    .prepare( checkDeprecation[A] )
}
