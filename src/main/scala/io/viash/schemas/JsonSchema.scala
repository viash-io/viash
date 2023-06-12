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
    val properties = info.filter(p => !p.name.startsWith("__"))
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

    // TODO figure out `required` (not "Option of" or has default value)
    Json.obj(
      "description" -> Json.fromString(description),
      "type" -> Json.fromString("object"),
      "properties" -> Json.obj(propertiesJson: _*),
      "additionalProperties" -> Json.False
    )
  }

  def getJsonSchema: Json = {
    val configData = data.config.get("config").get
    val configDescription = configData.find(p => p.name == "__this__").get.description.get

    val schema = Json.obj(
      "$schema" -> Json.fromString("https://json-schema.org/draft-07/schema#"),
      "description" -> Json.fromString("A schema for Viash config files"),
      "definitions" -> Json.obj(
        "Functionality" -> createSchema(data.functionality.get("functionality").get),
        "NativePlatform" -> createSchema(data.platforms.get("nativePlatform").get, Some("native")),
        "DockerPlatform" -> createSchema(data.platforms.get("dockerPlatform").get, Some("docker")),
        "NextflowVdsl3Platform" -> createSchema(data.platforms.get("nextflowVdsl3Platform").get, Some("nextflow")),

        "Platforms" -> Json.obj(
          "anyOf" -> Json.arr(
            Json.obj("$ref" -> Json.fromString("#/definitions/NativePlatform")),
            Json.obj("$ref" -> Json.fromString("#/definitions/DockerPlatform")),
            Json.obj("$ref" -> Json.fromString("#/definitions/NextflowVdsl3Platform"))
          )
        ),

        "Info" -> createSchema(data.config.get("info").get),

        "Author" -> createSchema(data.functionality.get("author").get),
        "ComputationalRequirements" -> createSchema(data.functionality.get("computationalRequirements").get),

        "ApkRequirements" -> createSchema(data.requirements.get("apkRequirements").get, Some("apk")),
        "AptRequirements" -> createSchema(data.requirements.get("aptRequirements").get, Some("apt")),
        "DockerRequirements" -> createSchema(data.requirements.get("dockerRequirements").get, Some("docker")),
        "JavascriptRequirements" -> createSchema(data.requirements.get("javascriptRequirements").get, Some("javascript")),
        "PythonRequirements" -> createSchema(data.requirements.get("pythonRequirements").get, Some("python")),
        "RRequirements" -> createSchema(data.requirements.get("rRequirements").get, Some("r")),
        "RubyRequirements" -> createSchema(data.requirements.get("rubyRequirements").get, Some("ruby")),
        "YumRequirements" -> createSchema(data.requirements.get("yumRequirements").get, Some("yum")),
        "Requirements" -> Json.obj(
          "anyOf" -> Json.arr(
            Json.obj("$ref" -> Json.fromString("#/definitions/ApkRequirements")),
            Json.obj("$ref" -> Json.fromString("#/definitions/AptRequirements")),
            Json.obj("$ref" -> Json.fromString("#/definitions/DockerRequirements")),
            Json.obj("$ref" -> Json.fromString("#/definitions/JavascriptRequirements")),
            Json.obj("$ref" -> Json.fromString("#/definitions/PythonRequirements")),
            Json.obj("$ref" -> Json.fromString("#/definitions/RRequirements")),
            Json.obj("$ref" -> Json.fromString("#/definitions/RubyRequirements")),
            Json.obj("$ref" -> Json.fromString("#/definitions/YumRequirements")),            
          )
        ),

        "BooleanArgument" -> createSchema(data.arguments.get("boolean").get, Some("boolean")),
        "BooleanTrueArgument" -> createSchema(data.arguments.get("boolean_true").get, Some("boolean_true")),
        "BooleanFalseArgument" -> createSchema(data.arguments.get("boolean_false").get, Some("boolean_false")),
        "DoubleArgument" -> createSchema(data.arguments.get("double").get, Some("double")),
        "FileArgument" -> createSchema(data.arguments.get("file").get, Some("file")),
        "IntegerArgument" -> createSchema(data.arguments.get("integer").get, Some("integer")),
        "LongArgument" -> createSchema(data.arguments.get("long").get, Some("long")),
        "StringArgument" -> createSchema(data.arguments.get("string").get, Some("string")),
        "Argument" -> Json.obj(
          "anyOf" -> Json.arr(
            Json.obj("$ref" -> Json.fromString("#/definitions/BooleanArgument")),
            Json.obj("$ref" -> Json.fromString("#/definitions/BooleanTrueArgument")),
            Json.obj("$ref" -> Json.fromString("#/definitions/BooleanFalseArgument")),
            Json.obj("$ref" -> Json.fromString("#/definitions/DoubleArgument")),
            Json.obj("$ref" -> Json.fromString("#/definitions/FileArgument")),
            Json.obj("$ref" -> Json.fromString("#/definitions/IntegerArgument")),
            Json.obj("$ref" -> Json.fromString("#/definitions/LongArgument")),
            Json.obj("$ref" -> Json.fromString("#/definitions/StringArgument")),
          )
        ),
        "ArgumentGroup" -> Json.obj(
          "type" -> Json.fromString("object"),
          "properties" -> Json.obj(
            "name" -> valueJson("The name of the argument group.", "String"),
            "description" -> valueJson("A description of the argument group. Multiline descriptions are supported.", "String"),
            "arguments" -> arrayJson("List of the arguments names.", "Argument")
          ),
          "required" -> Json.arr(Json.fromString("name"), Json.fromString("arguments")),
          "additionalProperties" -> Json.False
        ),

        "BashScript" -> createSchema(data.resources.get("bashScript").get, Some("bash_script")),
        "CSharpScript" -> createSchema(data.resources.get("cSharpScript").get, Some("csharp_script")),
        "Executable" -> createSchema(data.resources.get("executable").get, Some("executable")),
        "JavaScriptScript" -> createSchema(data.resources.get("javaScriptScript").get, Some("javascript_script")),
        "NextflowScript" -> createSchema(data.resources.get("nextflowScript").get, Some("nextflow_script")),
        "PlainFile" -> createSchema(data.resources.get("plainFile").get, Some("file")),
        "PythonScript" -> createSchema(data.resources.get("pythonScript").get, Some("python_script")),
        "RScript" -> createSchema(data.resources.get("rScript").get, Some("r_script")),
        "ScalaScript" -> createSchema(data.resources.get("scalaScript").get, Some("scala_script")),
        "Resource" -> Json.obj(
          "anyOf" -> Json.arr(
            Json.obj("$ref" -> Json.fromString("#/definitions/BashScript")),
            Json.obj("$ref" -> Json.fromString("#/definitions/CSharpScript")),
            Json.obj("$ref" -> Json.fromString("#/definitions/Executable")),
            Json.obj("$ref" -> Json.fromString("#/definitions/JavaScriptScript")),
            Json.obj("$ref" -> Json.fromString("#/definitions/NextflowScript")),
            Json.obj("$ref" -> Json.fromString("#/definitions/PlainFile")),
            Json.obj("$ref" -> Json.fromString("#/definitions/PythonScript")),
            Json.obj("$ref" -> Json.fromString("#/definitions/RScript")),
            Json.obj("$ref" -> Json.fromString("#/definitions/ScalaScript")),
          )
        ),

        "NextflowDirectives" -> createSchema(data.nextflowParameters.get("nextflowDirectives").get),
        "NextflowAuto" -> createSchema(data.nextflowParameters.get("nextflowAuto").get),
        "NextflowConfig" -> createSchema(data.nextflowParameters.get("nextflowConfig").get),


        "DockerSetupStrategy" -> Json.obj(
          "$comment" -> Json.fromString("TODO add descriptions to different strategies"),
          "enum" -> Json.arr(
            DockerSetupStrategy.map.keys.toSeq.map(s => Json.fromString(s)): _*
          ),
          "description" -> Json.fromString("The Docker setup strategy to use when building a container.")
        ),

        "Direction" -> Json.obj(
          "enum" -> Json.arr(
            Seq("input", "output").map(s => Json.fromString(s)): _*
          ),
          "description" -> Json.fromString("Makes this argument an `input` or an `output`, as in does the file/folder needs to be read or written. `input` by default.")
        ),

        "Status" -> Json.obj(
          "enum" -> Json.arr(
            Seq("enabled", "disabled", "deprecated").map(s => Json.fromString(s)): _*
          ),
          "description" -> Json.fromString("Allows setting a component to active, deprecated or disabled.")
        ),

        "DockerResolveVolume" -> Json.obj(
          "$comment" -> Json.fromString("TODO make fully case insensitive"),
          "enum" -> Json.arr(
            Seq("manual", "automatic", "auto", "Manual", "Automatic", "Auto").map(s => Json.fromString(s)): _*
          ),
          "description" -> Json.fromString("Enables or disables automatic volume mapping. Enabled when set to `Automatic` or disabled when set to `Manual`. Default: `Automatic`")
        )

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
