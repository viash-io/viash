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

import io.circe.{Decoder, Encoder, Json}
import io.circe.generic.extras.Configuration
import java.net.URI

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
        val newJs = json.mapObject(_.mapValues(_.dropEmptyRecursively()).filter(!_._2.isNull))
        if (newJs.asObject.get.isEmpty) {
          Json.Null
        } else {
          newJs
        }
      } else if (json.isArray) {
        val newJs = json.mapArray(_.map(_.dropEmptyRecursively()).filter(!_.isNull))
        if (json.asArray.get.isEmpty) {
          Json.Null
        } else {
          newJs
        }
      } else {
        json
      }
    }

    /**
     * Perform a deep merge of this JSON value with another JSON value.
     *
     * Objects are merged by key, values from the argument JSON take
     * precedence over values from this JSON. Nested objects are
     * recursed.
     *
     * Null, Boolean, String and Number are treated as values,
     * and values from the argument JSON completely replace values
     * from this JSON.
     *
     * `mergeMode` controls the behavior when merging two arrays within JSON.
     * The Default mode treats Array as value, similar to Null, Boolean,
     * String or Number above. The Index mode will replace the elements in
     * this JSON array with the elements in the argument JSON at corresponding
     * position. The Concat mode will concatenate the elements in this JSON array
     * and the argument JSON array.
     * 
     * Implementation borrowed from 
     * https://github.com/circe/circe/pull/1275/files#diff-29d5b464593242150a323b723877ccad4b5be2c0eedb6772ccdcfb7a3d5059fbR173.
     * Function was renamed to avoid future naming conflicts.
     */
    def customDeepMerge(that: Json, mergeMode: MergeMode = MergeMode.Concat): Json =
      (json.asObject, that.asObject) match {
        case (Some(lhs), Some(rhs)) =>
          Json.fromJsonObject(
            lhs.toList.foldLeft(rhs) {
              case (acc, (key, value)) =>
                rhs(key).fold(acc.add(key, value)) { r =>
                  acc.add(key, value.customDeepMerge(r, mergeMode))
                }
            }
          )
        case _ =>
          mergeMode match {
            case MergeMode.Default =>
              that
            case _ =>
              (json.asArray, that.asArray) match {
                case (Some(lhs), Some(rhs)) =>
                  mergeMode match {
                    case MergeMode.Concat =>
                      Json.fromValues(lhs ++ rhs)
                    case MergeMode.Index if rhs.size < lhs.size =>
                      Json.fromValues(rhs ++ lhs.slice(rhs.size, lhs.size))
                    case MergeMode.Index if rhs.size >= lhs.size => 
                      that
                    case _ => that
                  }
                case _ => that
              }
          }
      }

      /**
       * Recursively resolve inheritance within Json objects.
       * 
       * If an object has a field named "__inherits__", that file will be read
       * and be deep-merged with the object itself.
       */
      def inherit(uri: URI, mergeMode: MergeMode = MergeMode.Concat): Json = {
        json match {
          case x if x.isObject =>
            val obj1 = x.asObject.get
            val obj2 = obj1.apply("__inherits__") match {
              case Some(y) if y.isString || y.isArray =>
                // remove inherits field from obj1
                val obj1rem = Json.fromJsonObject(obj1.remove("__inherits__"))

                // resolve uri
                val uriStrs = 
                  if (y.isString) {
                    List(y.asString.get)
                  } else {
                    // TODO: add decent error message instead of simply .get
                    y.asArray.get.map(_.asString.get).toList
                  }

                // recurse through new json as well
                val newJsons = uriStrs.map(uriStr => {
                  val newURI = uri.resolve(uriStr)

                  // read as string
                  val str = IO.read(newURI)

                  // parse as yaml
                  // TODO: add decent error message instead of simply .get
                  val newJson1 = io.circe.yaml.parser.parse(str).right.get

                  // recurse through new json as well
                  val newJson2 = newJson1.inherit(newURI, mergeMode = mergeMode)

                  newJson2
                })

                // merge with orig object
                val jsMerged = 
                  newJsons.foldLeft(obj1rem) { (oldJs, newJs) => 
                    oldJs.customDeepMerge(newJs, mergeMode = mergeMode)
                  }

                // return combined object
                jsMerged.asObject.get
                
              case None => obj1
            }
            val obj3 = obj2.mapValues(x => x.inherit(uri, mergeMode = mergeMode))
            Json.fromJsonObject(obj3)
          case x if x.isArray => 
            val arr1 = x.asArray.get
            val arr2 = arr1.map(y => y.inherit(uri, mergeMode = mergeMode))
            Json.fromValues(arr2)
          case _ => json
        }
      }
    }
  
  sealed trait MergeMode
  object MergeMode {
    case object Default extends MergeMode
    case object Index extends MergeMode
    case object Concat extends MergeMode
  }

  implicit val decodeStringLike: Decoder[String] =
    Decoder.decodeString or
      Decoder.decodeInt.map(_.toString) or
      Decoder.decodeFloat.map(_.toString) or
      Decoder.decodeBoolean.map(_.toString)
}