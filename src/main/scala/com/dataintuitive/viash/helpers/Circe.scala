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

import io.circe.{Decoder, Encoder, Json}
import io.circe.generic.extras.Configuration

object Circe {
  implicit val customConfig: Configuration =
    Configuration.default.withDefaults.withStrictDecoding

  // encoder and decoder for Either
  implicit def encodeEither[A,B](implicit ea: Encoder[A], eb: Encoder[B]): Encoder[Either[A,B]] = {
    case Left(a) => ea(a)
    case Right(b) => eb(b)
  }

  implicit def decodeEither[A,B](implicit a: Decoder[A], b: Decoder[B]): Decoder[Either[A,B]] = {
    val l: Decoder[Either[A,B]] = a.map(Left.apply)
    val r: Decoder[Either[A,B]] = b.map(Right.apply)
    l or r
  }

  // oneormore helper type
  abstract class OneOrMore[+A] {
    def toList: List[A]
    override def equals(that: Any): Boolean = {
      that match {
        case that: OneOrMore[_] => {
          this.toList.equals(that.toList)
        }
        case _ => false
      }
    }
  }
  case class One[A](element: A) extends OneOrMore[A] {
    def toList = List(element)
  }
  case class More[A](list: List[A]) extends OneOrMore[A] {
    def toList = list
  }

  implicit def oneOrMoreToList[A](oom: OneOrMore[A]): List[A] = {
    if (oom == null) {
      Nil
    } else {
      oom.toList
    }
  }
  implicit def listToOneOrMore[A](li: List[A]): OneOrMore[A] = More(li)

  
  implicit def encodeOneOrMore[A](implicit enc: Encoder[List[A]]): Encoder[OneOrMore[A]] = { 
    oom: OneOrMore[A] => if (oom == null) enc(Nil) else enc(oom.toList)
  }

  implicit def decodeOneOrMore[A](implicit da: Decoder[A], dl: Decoder[List[A]]): Decoder[OneOrMore[A]] = {
    val l: Decoder[OneOrMore[A]] = da.map(One.apply)
    val r: Decoder[OneOrMore[A]] = dl.map(More.apply)
    l or r
  }
  
  implicit class RichJson(json: Json) {
    def withDefault(field: String, value: Json): Json = {
      json.mapObject{ obj =>	
        if (!obj.contains(field)) {
          obj.add(field, value)
        } else {
          obj
        }
      }
    }

    def dropEmptyRecursively(): Json = {
      if (json.isObject) {
        val newJs = json.mapObject{
          _.mapValues(_.dropEmptyRecursively())
            .filter(!_._2.isNull)
        }
        if (newJs.asObject.get.isEmpty) {
          Json.Null
        } else {
          newJs
        }
      } else if (json.isArray) {
        if (json.asArray.get.isEmpty) {
          Json.Null
        } else {
          json
        }
      } else {
        json
      }
    }
  }

}