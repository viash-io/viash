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

  def valueJson(`type`: String): Json = {
    Json.obj(
      typeOrRefJson(`type`)
    )
  }

  def valueJson(description: String, `type`: String): Json = {
    Json.obj(
      "description" -> Json.fromString(description),
      typeOrRefJson(`type`)
    )
  }

  def arrayJson(description: String, `type`: String): Json = {
    Json.obj(
      "description" -> Json.fromString(description),
      "type" -> Json.fromString("array"),
      "items" -> Json.obj(typeOrRefJson(`type`))
    )
  }

  def mapJson(description: String, `type`: String): Json = {
    Json.obj(
      "description" -> Json.fromString(description),
      "type" -> Json.fromString("object"),
      "additionalProperties" -> valueJson(description, `type`)
    )
  }

  def mapJson(`type`: String): Json = {
    Json.obj(
      "type" -> Json.fromString("object"),
      "additionalProperties" -> valueJson(`type`)
    )
  }

  def createSchema(info: List[ParameterSchema], fixedTypeString: Option[String] = None): Json = {
    val description = info.find(p => p.name == "__this__").get.description.get
    val properties = info.filter(p => !p.name.startsWith("__")).filter(p => !p.removed.isDefined)
    val propertiesJson = properties.map(p => {
      val pDescription = p.description.getOrElse("")
      val trimmedType = p.`type`.stripPrefix("Option of ")

      val mapRegex = "(List)?Map of String,(\\w*)".r

      trimmedType match {
        case s"List of $s" => 
          (p.name, arrayJson(pDescription, s))

        case s"OneOrMore of $s" =>
          (p.name, Json.obj("anyOf" -> Json.arr(
            valueJson(pDescription, s),
            arrayJson(pDescription, s)
          )))

        case "Option[Either[String,List of String]]" =>
          (p.name, Json.obj("anyOf" -> Json.arr(
            valueJson(pDescription, "String"),
            arrayJson(pDescription, "String")
          )))

        case s"Option[Either of $s,$t]" =>
          (p.name, Json.obj("anyOf" -> Json.arr(
            valueJson(pDescription, s),
            valueJson(pDescription, t)
          )))

        case "OneOrMore[Map of String,String]" =>
          (p.name, Json.obj("anyOf" -> Json.arr(
            mapJson(pDescription, "String"),
            Json.obj(
              "description" -> Json.fromString(pDescription),
              "type" -> Json.fromString("array"),
              "items" -> mapJson("String")
            )
          )))

        case "Option[Either[Map of String,String,String]]" =>
          (p.name, Json.obj("anyOf" -> Json.arr(
            mapJson(pDescription, "String"),
            valueJson(pDescription, "String")
          )))

        case "OneOrMore[Either[String,Map of String,String]]" =>
          (p.name, Json.obj("anyOf" -> Json.arr(
            Json.obj("anyOf" -> Json.arr(
              valueJson(pDescription, "String"),
              mapJson(pDescription, "String")
            )),
            Json.obj(
              "description" -> Json.fromString(pDescription),
              "type" -> Json.fromString("array"),
              "items" -> Json.obj("anyOf" -> Json.arr(
                valueJson(pDescription, "String"),
                mapJson(pDescription, "String")
            ))
            )
          )))

        case mapRegex(_, s) => 
          (p.name, mapJson(pDescription, s))

        case s if p.name == "type" && fixedTypeString.isDefined =>
          ("type", Json.obj(
            "description" -> Json.fromString(description), // not pDescription! We want to show the description of the main class
            "const" -> Json.fromString(fixedTypeString.get)
          ))
        
        case s =>
          (p.name, valueJson(pDescription, s))
      }

    })

    val required = properties.filter(p => !(p.`type`.startsWith("Option of ") || p.`type`.startsWith("Option[") || p.default.isDefined))
    val requiredJson = required.map(p => Json.fromString(p.name))

    // TODO figure out `required` (not "Option of" or has default value)
    Json.obj(
      "description" -> Json.fromString(description),
      "type" -> Json.fromString("object"),
      "properties" -> Json.obj(propertiesJson: _*),
      "required" -> Json.arr(requiredJson: _*),
      "additionalProperties" -> Json.False
    )
  }

  def createVariantSchemas(data: Map[String, List[ParameterSchema]], groupName: String, translationMap: Map[String, String]): Seq[(String, Json)] = {
    
    val group = groupName -> Json.obj(
      "anyOf" -> Json.arr(
        translationMap.map{ case (k, v) => Json.obj("$ref" -> Json.fromString(s"#definitions/$k")) }.toSeq: _*
      )
    )

    def firstLower(s: String): String = s.head.toLower.toString + s.tail

    val variants = translationMap.map {
      case (k, v) =>
        k -> createSchema(data.get(firstLower(k)).get, Some(v))
    }

    variants.toSeq :+ group
  }

  def createVariantSchemasAlt(data: Map[String, List[ParameterSchema]], groupName: String, translationMap: Map[String, String]): Seq[(String, Json)] = {
    
    val group = groupName -> Json.obj(
      "anyOf" -> Json.arr(
        translationMap.map{ case (k, v) => Json.obj("$ref" -> Json.fromString(s"#definitions/$k")) }.toSeq: _*
      )
    )

    val variants = translationMap.map {
      case (k, v) =>
        k -> createSchema(data.get(v).get, Some(v))
    }

    variants.toSeq :+ group
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
    val configData = data.config.get("config").get
    val configDescription = configData.find(p => p.name == "__this__").get.description.get


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
      createVariantSchemas(data.platforms, "Platforms", platformMap) ++
      Seq(
        "Info" -> createSchema(data.config.get("info").get),
        "Functionality" -> createSchema(data.functionality.get("functionality").get),
        "Author" -> createSchema(data.functionality.get("author").get),
        "ComputationalRequirements" -> createSchema(data.functionality.get("computationalRequirements").get)
      ) ++
      createVariantSchemas(data.requirements, "Requirements", requirementsMap) ++
      createVariantSchemasAlt(data.arguments, "Argument", argumentsMap) ++
      Seq("ArgumentGroup" -> Json.obj(
          "type" -> Json.fromString("object"),
          "properties" -> Json.obj(
            "name" -> valueJson("The name of the argument group.", "String"),
            "description" -> valueJson("A description of the argument group. Multiline descriptions are supported.", "String"),
            "arguments" -> arrayJson("List of the arguments names.", "Argument")
          ),
          "required" -> Json.arr(Json.fromString("name"), Json.fromString("arguments")),
          "additionalProperties" -> Json.False
      )) ++
      createVariantSchemas(data.resources, "Resource", resourceMap) ++
      Seq(
        "NextflowDirectives" -> createSchema(data.nextflowParameters.get("nextflowDirectives").get),
        "NextflowAuto" -> createSchema(data.nextflowParameters.get("nextflowAuto").get),
        "NextflowConfig" -> createSchema(data.nextflowParameters.get("nextflowConfig").get)
      ) ++
      Seq(
        "DockerSetupStrategy" -> createEnum(DockerSetupStrategy.map.keys.toSeq, "The Docker setup strategy to use when building a container.", Some("TODO add descriptions to different strategies")),
        "Direction" -> createEnum(Seq("input", "output"), "Makes this argument an `input` or an `output`, as in does the file/folder needs to be read or written. `input` by default.", None),
        "Status" -> createEnum(Seq("enabled", "disabled", "deprecated"), "Allows setting a component to active, deprecated or disabled.", None),
        "DockerResolveVolume" -> createEnum(Seq("manual", "automatic", "auto", "Manual", "Automatic", "Auto"), "Enables or disables automatic volume mapping. Enabled when set to `Automatic` or disabled when set to `Manual`. Default: `Automatic`", Some("TODO make fully case insensitive"))
      )

    val schema = Json.obj(
      "$schema" -> Json.fromString("https://json-schema.org/draft-07/schema#"),
      "description" -> Json.fromString("A schema for Viash config files"),
      "definitions" -> Json.obj(
        definitions: _*
      ),
      "properties" -> Json.obj(
        "functionality" -> valueJson(data.functionality.get("functionality").get.head.description.get, "Functionality"),
        "platforms" -> arrayJson("Definition of the platforms", "Platforms"),
        "info" -> valueJson("Definition of meta data", "Info")
      ),
      "required" -> Json.arr(Json.fromString("functionality")),
      "additionalProperties" -> Json.False,
    )

    schema
  }
}
