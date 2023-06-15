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
      case "Double" =>
        "type" -> Json.fromString("number")
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

  def createSchema(info: List[ParameterSchema], fixedTypeString: Option[String] = None): Json = {

    def removeMarkup(text: String): String = {
      val markupRegex = raw"@\[(.*?)\]\(.*?\)".r
      val backtickRegex = "`(\"[^`\"]*?\")`".r
      val textWithoutMarkup = markupRegex.replaceAllIn(text, "$1")
      backtickRegex.replaceAllIn(textWithoutMarkup, "$1")
    }

    val description = removeMarkup(info.find(p => p.name == "__this__").get.description.get)
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
          if (s == "String" && p.name == "port" && fixedTypeString == Some("docker")) {
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

        case s if p.name == "type" && fixedTypeString.isDefined =>
          ("type", Json.obj(
            "description" -> Json.fromString(description), // not pDescription! We want to show the description of the main class
            "const" -> Json.fromString(fixedTypeString.get)
          ))
        
        case s =>
          (p.name, valueType(s, pDescription))
      }

    })

    val required = properties.filter(p => 
      !(
        p.`type`.startsWith("Option[") ||
        p.default.isDefined ||
        (p.name == "type" && fixedTypeString == Some("file")) // Custom exception, file resources are "kind of" default
      ))
    val requiredJson = required.map(p => Json.fromString(p.name))

    Json.obj(
      "description" -> Json.fromString(description),
      "type" -> Json.fromString("object"),
      "properties" -> Json.obj(propertiesJson: _*),
      "required" -> Json.arr(requiredJson: _*),
      "additionalProperties" -> Json.False
    )
  }

  def createVariantSchemas(data: Map[String, List[ParameterSchema]], groupName: String, translationMap: Map[String, String]): Seq[(String, Json)] = {
    val group = groupName -> eitherJson(
      translationMap.map{ case (k, v) => Json.obj("$ref" -> Json.fromString(s"#/definitions/$k")) }.toSeq: _*
    )

    def firstLower(s: String): String = s.head.toLower.toString + s.tail

    val variants = translationMap.map {
      case (k, v) =>
        k -> createSchema(data.get(firstLower(k)).get, Some(v))
    }

    variants.toSeq :+ group
  }

  def createVariantSchemasAlt(data: Map[String, List[ParameterSchema]], groupName: String, translationMap: Map[String, String]): Seq[(String, Json)] = {
    val group = groupName -> eitherJson(
      translationMap.map{ case (k, v) => Json.obj("$ref" -> Json.fromString(s"#/definitions/$k")) }.toSeq: _*
    )

    val variants = translationMap.map {
      case (k, v) =>
        k -> createSchema(data.get(v).get, Some(v))
    }

    variants.toSeq :+ group
  }

  def createGroupedSchemas(data: Map[String, List[ParameterSchema]]) : Seq[(String, Json)] = {
    data.toList.map{
      case (k, v) => k.capitalize -> createSchema(v, None)
    }
  }

  def createEnum(values: Seq[String], description: String, comment: Option[String]): Json = {
    comment match {
      case Some(s) =>
        Json.obj(
          "$comment" -> Json.fromString(s),
          "enum" -> Json.arr(
            values.map(s => Json.fromString(s)): _*
          ),
          "description" -> Json.fromString(description)
        )
      case None =>
        Json.obj(
          "enum" -> Json.arr(
            values.map(s => Json.fromString(s)): _*
          ),
          "description" -> Json.fromString(description)
        )
    }
  }

  def getJsonSchema: Json = {
    val platformMap = Map("NativePlatform" -> "native", "DockerPlatform" -> "docker", "NextflowVdsl3Platform" -> "nextflow")
    val requirementsMap = Map(
      "ApkRequirements" -> "apk",
      "AptRequirements" -> "apt",
      "DockerRequirements" -> "docker",
      "JavascriptRequirements" -> "javascript",
      "PythonRequirements" -> "python",
      "RRequirements" -> "r",
      "RubyRequirements" -> "ruby",
      "YumRequirements" -> "yum"
    )

    val argumentsMap = Map(
      "BooleanArgument" -> "boolean",
      "BooleanTrueArgument" -> "boolean_true",
      "BooleanFalseArgument" -> "boolean_false",
      "DoubleArgument" -> "double",
      "FileArgument" -> "file",
      "IntegerArgument" -> "integer",
      "LongArgument" -> "long",
      "StringArgument" -> "string",
    )

    val resourceMap = Map(
      "BashScript" -> "bash_script",
      "CSharpScript" -> "csharp_script",
      "Executable" -> "executable",
      "JavaScriptScript" -> "javascript_script",
      "NextflowScript" -> "nextflow_script",
      "PlainFile" -> "file",
      "PythonScript" -> "python_script",
      "RScript" -> "r_script",
      "ScalaScript" -> "scala_script"
    )

    val definitions =
      createGroupedSchemas(data.config) ++
      createGroupedSchemas(data.functionality) ++
      createVariantSchemas(data.platforms, "Platform", platformMap) ++
      createVariantSchemas(data.requirements, "Requirements", requirementsMap) ++
      createVariantSchemasAlt(data.arguments, "Argument", argumentsMap) ++
      createVariantSchemas(data.resources, "Resource", resourceMap) ++
      createGroupedSchemas(data.nextflowParameters) ++
      Seq(
        "DockerSetupStrategy" -> createEnum(DockerSetupStrategy.map.keys.toSeq, "The Docker setup strategy to use when building a container.", Some("TODO add descriptions to different strategies")),
        "Direction" -> createEnum(Seq("input", "output"), "Makes this argument an `input` or an `output`, as in does the file/folder needs to be read or written. `input` by default.", None),
        "Status" -> createEnum(Seq("enabled", "disabled", "deprecated"), "Allows setting a component to active, deprecated or disabled.", None),
        "DockerResolveVolume" -> createEnum(Seq("manual", "automatic", "auto", "Manual", "Automatic", "Auto"), "Enables or disables automatic volume mapping. Enabled when set to `Automatic` or disabled when set to `Manual`. Default: `Automatic`", Some("TODO make fully case insensitive"))
      )

    Json.obj(
      "$schema" -> Json.fromString("https://json-schema.org/draft-07/schema#"),
      "definitions" -> Json.obj(
        definitions: _*
      ),
      "oneOf" -> Json.arr(valueType("Config"))
    )
  }
}
