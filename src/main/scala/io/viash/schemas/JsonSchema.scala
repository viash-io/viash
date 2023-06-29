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

package io.viash.schemas

import io.circe.Json
import io.viash.platforms.docker.DockerSetupStrategy

object JsonSchema {

  lazy val data = CollectedSchemas.data

  def typeOrRefJson(`type`: String): (String, Json) = {
    `type` match {
      case "Boolean" =>
        "type" -> Json.fromString("boolean")
      case "Int" | "Long" =>
        "type" -> Json.fromString("integer")
      // Basic double value, still needed to compose the either (double or infinity strings)
      case "Double_" =>
        "type" -> Json.fromString("number")
      // Custom exception. Used to add infinity or nan values to the double type
      case "Double" =>
        "$ref" -> Json.fromString("#/definitions/DoubleWithInf")
      case "String" | "Path" =>
        "type" -> Json.fromString("string")
      case "Json" =>
        "type" -> Json.fromString("object")
      case s =>
        "$ref" -> Json.fromString("#/definitions/" + s)
    }
  }

  def valueType(`type`: String, description: Option[String] = None): Json = {
    Json.obj(
      description.map(s => Seq("description" -> Json.fromString(s))).getOrElse(Nil) ++
      Seq(typeOrRefJson(`type`)): _*
    )
  }

  def arrayType(`type`: String, description: Option[String] = None): Json = {
    arrayJson(valueType(`type`), description)
  }

  def mapType(`type`: String, description: Option[String] = None): Json = {
    mapJson(valueType(`type`), description)
  }

  def oneOrMoreType(`type`: String, description: Option[String] = None): Json = {
    oneOrMoreJson(valueType(`type`, description))
  }

  def arrayJson(json: Json, description: Option[String] = None): Json = {
    Json.obj(
      description.map(s => Seq("description" -> Json.fromString(s))).getOrElse(Nil) ++
      Seq(
        "type" -> Json.fromString("array"),
        "items" -> json
      ): _*
    )
  }

  def mapJson(json: Json, description: Option[String] = None) = {
    Json.obj(
      description.map(s => Seq("description" -> Json.fromString(s))).getOrElse(Nil) ++
      Seq(
        "type" -> Json.fromString("object"),
        "additionalProperties" -> json
      ): _*
    )
  }

  def oneOrMoreJson(json: Json): Json = {
    eitherJson(
      json,
      arrayJson(json)
    )
  }
  def eitherJson(jsons: Json*): Json = {
    Json.obj("oneOf" -> Json.arr(jsons: _*))
  }

  def createSchema(info: List[ParameterSchema]): (String, Json) = {

    def removeMarkup(text: String): String = {
      val markupRegex = raw"@\[(.*?)\]\(.*?\)".r
      val backtickRegex = "`(\"[^`\"]*?\")`".r
      val textWithoutMarkup = markupRegex.replaceAllIn(text, "$1")
      backtickRegex.replaceAllIn(textWithoutMarkup, "$1")
    }

    val thisParameter = info.find(p => p.name == "__this__").get
    val description = removeMarkup(thisParameter.description.get)
    val subclass = thisParameter.subclass.map(l => l.head)
    val properties = info.filter(p => !p.name.startsWith("__")).filter(p => !p.removed.isDefined)
    val propertiesJson = properties.map(p => {
      val pDescription = p.description.map(s => removeMarkup(s))
      val trimmedType = p.`type` match {
        case s if s.startsWith("Option[") => s.stripPrefix("Option[").stripSuffix("]")
        case s => s
      }

      val mapRegex = "(List)?Map\\[String,(\\w*)\\]".r

      trimmedType match {
        case s"List[$s]" => 
          (p.name, arrayType(s, pDescription))

        case "Either[String,List[String]]" =>
          (p.name, eitherJson(
            valueType("String", pDescription),
            arrayType("String", pDescription)
          ))

        case "Either[Map[String,String],String]" =>
          (p.name, eitherJson(
            mapType("String", pDescription),
            valueType("String", pDescription)
          ))

        case s"Either[$s,$t]" =>
          (p.name, eitherJson(
            valueType(s, pDescription),
            valueType(t, pDescription)
          ))

        case "OneOrMore[Map[String,String]]" =>
          (p.name, oneOrMoreJson(
            mapType("String", pDescription)
          ))

        case "OneOrMore[Either[String,Map[String,String]]]" =>
          (p.name, oneOrMoreJson(
            eitherJson(
              valueType("String", pDescription),
              mapType("String", pDescription)
            )
          ))

        case s"OneOrMore[$s]" =>
          if (s == "String" && p.name == "port" && subclass == Some("docker")) {
            // Custom exception
            // This is the port field for a docker platform.
            // We want to allow a Strings or Ints.
            (p.name, eitherJson(
              valueType("String", pDescription),
              valueType("Int", pDescription),
              arrayType("String", pDescription),
              arrayType("Int", pDescription)
            ))
          } else {
            (p.name, oneOrMoreType(s, pDescription))
          }

        case mapRegex(_, s) => 
          (p.name, mapType(s, pDescription))

        case s if p.name == "type" && subclass.isDefined =>
          ("type", Json.obj(
            "description" -> Json.fromString(description), // not pDescription! We want to show the description of the main class
            "const" -> Json.fromString(subclass.get)
          ))
        
        case s =>
          (p.name, valueType(s, pDescription))
      }

    })

    val required = properties.filter(p => 
      !(
        p.`type`.startsWith("Option[") ||
        p.default.isDefined ||
        (p.name == "type" && subclass == Some("file")) // Custom exception, file resources are "kind of" default
      ))
    val requiredJson = required.map(p => Json.fromString(p.name))

    val k = thisParameter.`type`
    val v = Json.obj(
      "description" -> Json.fromString(description),
      "type" -> Json.fromString("object"),
      "properties" -> Json.obj(propertiesJson: _*),
      "required" -> Json.arr(requiredJson: _*),
      "additionalProperties" -> Json.False
    )
    k -> v
  }

  def createSuperClassSchema(info: List[ParameterSchema]): (String, Json) = {
    val thisParameter = info.find(p => p.name == "__this__").get
    val k = thisParameter.`type`
    val v = eitherJson(
      thisParameter.subclass.get.map(s => Json.obj("$ref" -> Json.fromString(s"#/definitions/$s"))): _*
    )
    k -> v
  }

  def createSchemas(data: List[List[ParameterSchema]]) : Seq[(String, Json)] = {
    val withoutRemoved = data.toList.filter(v => v.find(p => p.name == "__this__").get.removed.isEmpty)
    withoutRemoved.map{
      case v if (v.find(p => p.name == "__this__").get.subclass.map(l => l.length).getOrElse(0) > 1) => createSuperClassSchema(v)
      case v => createSchema(v)
    }
  }

  def createEnum(values: Seq[String], description: Option[String], comment: Option[String]): Json = {
    Json.obj(
      Seq("enum" -> Json.arr(values.map(s => Json.fromString(s)): _*)) ++
      comment.map(s => Seq("$comment" -> Json.fromString(s))).getOrElse(Nil) ++
      description.map(s => Seq("description" -> Json.fromString(s))).getOrElse(Nil): _*
    )
  }

  def getJsonSchema: Json = {
    val definitions =
      createSchemas(data) ++
      Seq(
        "DockerSetupStrategy" -> createEnum(DockerSetupStrategy.map.keys.toSeq, Some("The Docker setup strategy to use when building a container."), Some("TODO add descriptions to different strategies")),
        "Direction" -> createEnum(Seq("input", "output"), Some("Makes this argument an `input` or an `output`, as in does the file/folder needs to be read or written. `input` by default."), None),
        "Status" -> createEnum(Seq("enabled", "disabled", "deprecated"), Some("Allows setting a component to active, deprecated or disabled."), None),
        "DockerResolveVolume" -> createEnum(Seq("manual", "automatic", "auto", "Manual", "Automatic", "Auto"), Some("Enables or disables automatic volume mapping. Enabled when set to `Automatic` or disabled when set to `Manual`. Default: `Automatic`"), Some("TODO make fully case insensitive")),
        "DoubleStrings" -> createEnum(Seq("+.inf", "+inf", "+infinity", "positiveinfinity", "positiveinf", "-.inf", "-inf", "-infinity", "negativeinfinity", "negativeinf", ".nan", "nan"), None, None)
      ) ++
      Seq("DoubleWithInf" -> eitherJson(valueType("Double_"), valueType("DoubleStrings")))

    Json.obj(
      "$schema" -> Json.fromString("https://json-schema.org/draft-07/schema#"),
      "definitions" -> Json.obj(
        definitions: _*
      ),
      "oneOf" -> Json.arr(valueType("Config"))
    )
  }
}
