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

import java.net.URI

import io.circe.Json
import io.circe.JsonObject
import io.circe.generic.extras.Configuration
import io.circe.{Json, Printer => JsonPrinter}
import io.circe.yaml.{Printer => YamlPrinter}

import io.viash.helpers.IO
import io.viash.exceptions._

class RichJson(json: Json) {
  /**
    * If the Json is an object and does not have a certain field, add
    * a default value.
    *
    * @param field The field to check
    * @param value The value to add if the field is not present
    * @return A modified Json
    */
  def withDefault(field: String, value: Json): Json = {
    json.mapObject{ obj =>	
      if (!obj.contains(field)) {
        obj.add(field, value)
      } else {
        obj
      }
    }
  }

  /**
    * Removes all empty lists / objects from the Json
    *
    * @return A modified Json
    */
  def dropEmptyRecursively: Json = dropEmptyRecursivelyExcept(Nil)

  /**
    * Removes all empty lines / objects from the Json unless the key is found in the exclusions list
    *
    * @param exclusions The list of key names for which the values should remain untouched
    * @return A modified Json
    */
  def dropEmptyRecursivelyExcept(exclusions: Seq[String], nestedKey: String = "", key: String = ""): Json = {
    if (json.isObject) {
      val jo = json.asObject.get
        .map{
          case (k, v) if exclusions.contains(k) || exclusions.contains(s"$nestedKey.$k") => (k, v)
          case (k, v) => (k, v.dropEmptyRecursivelyExcept(exclusions, s"$nestedKey.$k", k))
        }
        .filter{ case (k, v) => !v.isNull || exclusions.contains(k) || exclusions.contains(s"$nestedKey.$k") }
      if (jo.nonEmpty) 
        Json.fromJsonObject(jo)
      else
        Json.Null
    } else if (json.isArray) {
      val ja = json.asArray.get
        .map(_.dropEmptyRecursivelyExcept(exclusions, s"$nestedKey", key))
        .filter(!_.isNull || exclusions.contains(key) || exclusions.contains(nestedKey))
      if (ja.nonEmpty) 
        Json.fromValues(ja)
      else
        Json.Null
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
   * Implementation borrowed from 
   * https://github.com/circe/circe/pull/1275/files#diff-29d5b464593242150a323b723877ccad4b5be2c0eedb6772ccdcfb7a3d5059fbR173.
   * Function was renamed to avoid future naming conflicts.
   */
  def concatDeepMerge(that: Json): Json = {
    if (json.isObject && that.isObject) {
      // if this and that are objects, recursively merge
      val lhs = json.asObject.get
      val rhs = that.asObject.get
      Json.fromJsonObject(
        lhs.toList.foldLeft(rhs) {
          case (acc, (key, value)) =>
            rhs(key).fold(acc.add(key, value)) { r =>
              acc.add(key, value.concatDeepMerge(r))
            }
        }
      )
    } else if (json.isArray && that.isArray) {
      // if this and that are both arrays, append
      val lhs = json.asArray.get
      val rhs = that.asArray.get
      Json.fromValues(lhs ++ rhs)
    } else {
      // else override this with that
      that
    }
  }

  private val mergeTag = "__merge__"

  /**
   * Recursively resolve inheritance within Json objects.
   * 
   * If an object has a field named "__merge__", that file will be read
   * and be deep-merged with the object itself.
   */
  def inherit(uri: URI, packageDir: Option[URI], stripInherits: Boolean = true): Json = {
    json match {
      case x if x.isObject =>
        val obj1 = x.asObject.get
        
        // throw removal error
        assert(
          !obj1.contains("__includes__"), 
          "__includes__ has been deprecated and removed. Please use __merge__ instead."
        )

        val obj2 = obj1.apply(mergeTag) match {
          case Some(y) if y.isString || y.isArray =>
            // get includes
            val inheritStrs0 = 
              if (y.isString) {
                List(y.asString.get)
              } else {
                // TODO: add decent error message instead of simply .get
                if (y.asArray.get.filter(!_.isString).nonEmpty) {
                  throw new ConfigParserMergeException(uri.toString, "invalid merge tag type. Must be a String or Array of Strings", y.toString())
                }
                y.asArray.get.map(_.asString.get).toList
              }
            
            // add self as last element if it is not already explicitly defined
            val inheritStrs1 =
              if (inheritStrs0.contains(".")) {
                inheritStrs0
              } else {
                inheritStrs0 ::: List(".")
              }

            // fetch uris
            val uris = inheritStrs1.map{str =>
              if (str == ".") {
                None
              } else if (str.startsWith("/")) {
                Some(IO.resolvePackagePath(str, packageDir))
              } else {
                Some(uri.resolve(str))
              }
            }
            
            // remove inherits field from obj1 if so desired, else replace with absolute paths
            val self = Json.fromJsonObject(
              if (stripInherits) {
                obj1.remove(mergeTag)
              } else {
                // replace __merge__ with absolute path using deepMerge
                val uriJsons = uris.map{
                  case None => Json.fromString(".")
                  case Some(uri) => Json.fromString(uri.toString)
                }
                val newInherits = Json.fromValues(uriJsons)
                val newObj = JsonObject(mergeTag -> newInherits)
                obj1 deepMerge newObj
              }
            )

            // generate list of jsons to merge
            // don't forget to resolve inheritance recursively
            val jsonsToMerge = uris.map{
              case None => self
              case Some(newURI) =>
                // read as string
                val str = IO.read(newURI)

                // parse as yaml
                val newJson1 = Convert.textToJson(str, newURI.toString)

                // recurse through new json as well
                val newJson2 = newJson1.inherit(newURI, packageDir = packageDir, stripInherits = stripInherits)

                newJson2
            }

            // merge jsons
            val jsMerged = jsonsToMerge.reduce{_ concatDeepMerge _}

            // return combined object
            jsMerged.asObject.get
          
          case Some(j) =>
            throw new ConfigParserMergeException(uri.toString, "invalid merge tag type. Must be a String or Array of Strings", j.toString())
          case None => obj1
        }
        val obj3 = obj2.mapValues(x => x.inherit(uri, packageDir = packageDir, stripInherits = stripInherits))
        Json.fromJsonObject(obj3)
      case x if x.isArray => 
        val arr1 = x.asArray.get
        val arr2 = arr1.map(y => y.inherit(uri, packageDir = packageDir, stripInherits = stripInherits))
        Json.fromValues(arr2)
      case _ => json
    }
  }

  def stripInherits: Json = {
    json
      .mapObject{
        _.mapValues(_.stripInherits).filter(_._1 != mergeTag)
      }
      .mapArray(
        _.map(_.stripInherits)
      )
  }

  private val yamlPrinter = YamlPrinter(
    preserveOrder = true,
    dropNullKeys = true,
    mappingStyle = YamlPrinter.FlowStyle.Block,
    splitLines = true,
    stringStyle = YamlPrinter.StringStyle.DoubleQuoted
  )
  private val jsonPrinter = JsonPrinter.spaces2.copy(dropNullValues = true)

  /**
    * Convert to a pretty String.
    *
    * @param format Must be either 'yaml' or 'json'
    * @return The YAML or JSON String representation.
    */
  def toFormattedString(format: String): String = {
    format match {
      case "yaml" => yamlPrinter.pretty(json)
      case "json" => jsonPrinter.print(json)
      case _ => throw new IllegalArgumentException("'format' must be either 'json' or 'yaml'.")
    }
  }
}