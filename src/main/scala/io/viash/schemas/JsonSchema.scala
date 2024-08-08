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
import io.viash.runners.executable.DockerSetupStrategy

object JsonSchema {

  case class SchemaConfig(
    strict: Boolean = false,
    minimal: Boolean = false
  )

  lazy val data = CollectedSchemas.fullData.map(_.filter(!_.hasInternalFunctionality))

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

  def valueType(`type`: String, description: Option[String] = None)(implicit config: SchemaConfig): Json = {
    val descr = config.minimal match {
      case true => None
      case false => description
    }
    Json.obj(
      descr.map(s => Seq("description" -> Json.fromString(s))).getOrElse(Nil) ++
      Seq(typeOrRefJson(`type`)): _*
    )
  }

  def arrayType(`type`: String, description: Option[String] = None)(implicit config: SchemaConfig): Json = {
    arrayJson(valueType(`type`), description)
  }

  def mapType(`type`: String, description: Option[String] = None)(implicit config: SchemaConfig): Json = {
    mapJson(valueType(`type`), description)
  }

  def oneOrMoreType(`type`: String, description: Option[String] = None)(implicit config: SchemaConfig): Json = {
    oneOrMoreJson(valueType(`type`, description))
  }

  def arrayJson(json: Json, description: Option[String] = None)(implicit config: SchemaConfig): Json = {
    val descr = config.minimal match {
      case true => None
      case false => description
    }
    Json.obj(
      descr.map(s => Seq("description" -> Json.fromString(s))).getOrElse(Nil) ++
      Seq(
        "type" -> Json.fromString("array"),
        "items" -> json
      ): _*
    )
  }

  def mapJson(json: Json, description: Option[String] = None)(implicit config: SchemaConfig) = {
    val descr = config.minimal match {
      case true => None
      case false => description
    }      
    Json.obj(
      descr.map(s => Seq("description" -> Json.fromString(s))).getOrElse(Nil) ++
      Seq(
        "type" -> Json.fromString("object"),
        "additionalProperties" -> json
      ): _*
    )
  }

  def oneOrMoreJson(json: Json)(implicit config: SchemaConfig): Json = {
    eitherJson(
      json,
      arrayJson(json)
    )
  }
  def eitherJson(jsons: Json*)(implicit config: SchemaConfig, mustIncludeAll: Boolean = false): Json = {
    if (config.strict && !mustIncludeAll) {
      jsons.last
    } else {
      Json.obj("oneOf" -> Json.arr(jsons: _*))
    }
  }

  def getThisParameter(data: List[ParameterSchema]): ParameterSchema = 
    data.find(_.name == "__this__").get

  def createSchema(info: List[ParameterSchema])(implicit config: SchemaConfig): (String, Json) = {
    ("", Json.Null)

    // def removeMarkup(text: String): String = {
    //   val markupRegex = raw"@\[(.*?)\]\(.*?\)".r
    //   val backtickRegex = "`(\"[^`\"]*?\")`".r
    //   val textWithoutMarkup = markupRegex.replaceAllIn(text, "$1")
    //   backtickRegex.replaceAllIn(textWithoutMarkup, "$1")
    // }

    // val thisParameter = getThisParameter(info)
    // val description = removeMarkup(thisParameter.description.get)
    // val subclass = thisParameter.subclass.map(l => l.head)
    // val properties = 
    //   info
    //   .filter(p => !p.name.startsWith("__")) // remove __this__
    //   .filter(p => !p.removed.isDefined && (!config.strict || !p.deprecated.isDefined)) // always remove 'removed' arguments and if need be create a strict schema by removing deprecated
    //   .filter(p => !config.strict || !(p.name == "arguments" && (thisParameter.`type` == "Functionality" || thisParameter.`type` == "Config"))) // exception: remove 'arguments' in 'Functionality' for strict schema
    // val propertiesJson = properties.map(p => {
    //   val pDescription = p.description.map(s => removeMarkup(s))
    //   val trimmedType = p.`type` match {
    //     case s if s.startsWith("Option[") => s.stripPrefix("Option[").stripSuffix("]")
    //     case s => s
    //   }

    //   val mapRegex = "(List)?Map\\[String,(\\w*)\\]".r

    //   implicit val useAllInEither = thisParameter.`type` == "NextflowDirectives" || thisParameter.`type` == "NextflowAuto"

    //   trimmedType match {
    //     case s"List[$s]" => 
    //       (p.name, arrayType(s, pDescription))

    //     case "Either[String,List[String]]" =>
    //       (p.name, eitherJson(
    //         valueType("String", pDescription),
    //         arrayType("String", pDescription)
    //       ))

    //     case "Either[Map[String,String],String]" =>
    //       (p.name, eitherJson(
    //         mapType("String", pDescription),
    //         valueType("String", pDescription)
    //       ))

    //     case s"Either[$s,$t]" =>
    //       (p.name, eitherJson(
    //         valueType(s, pDescription),
    //         valueType(t, pDescription)
    //       ))

    //     case "OneOrMore[Map[String,String]]" =>
    //       (p.name, oneOrMoreJson(
    //         mapType("String", pDescription)
    //       ))

    //     case "OneOrMore[Either[String,Map[String,String]]]" =>
    //       (p.name, oneOrMoreJson(
    //         eitherJson(
    //           valueType("String", pDescription),
    //           mapType("String", pDescription)
    //         )
    //       ))

    //     case s"OneOrMore[$s]" =>
    //       if (s == "String" && p.name == "port" && subclass == Some("executable")) {
    //         // Custom exception
    //         // This is the port field for a excutable runner.
    //         // We want to allow a Strings or Ints.
    //         (p.name, eitherJson(
    //           valueType("Int", pDescription),
    //           valueType("String", pDescription),
    //           arrayType("Int", pDescription),
    //           arrayType("String", pDescription)
    //         ))
    //       } else {
    //         (p.name, oneOrMoreType(s, pDescription))
    //       }

    //     case mapRegex(_, s) => 
    //       (p.name, mapType(s, pDescription))

    //     case s if p.name == "type" && subclass.isDefined =>
    //       if (config.minimal) {
    //         ("type", Json.obj(
    //           "const" -> Json.fromString(subclass.get)
    //         ))
    //       } else {
    //         ("type", Json.obj(
    //           "description" -> Json.fromString(description), // not pDescription! We want to show the description of the main class
    //           "const" -> Json.fromString(subclass.get)
    //         ))
    //       }
        
    //     case s =>
    //       (p.name, valueType(s, pDescription))
    //   }

    // })

    // val required = properties.filter(p => 
    //   !(
    //     p.`type`.startsWith("Option[") ||
    //     p.default.isDefined ||
    //     p.hasUndocumented ||
    //     (p.name == "type" && thisParameter.`type` == "PlainFile" && !config.strict) // Custom exception, file resources are "kind of" default
    //   ) ||
    //   // Strict schema is what will be outputted by Viash, so fields with a default value other than 'None' will always have a value -> add it in the strict schema as required.
    //   (!p.`type`.startsWith("Option[") && p.default != Some("Empty") && config.strict && !p.hasUndocumented))
    // val requiredJson = required.map(p => Json.fromString(p.name))

    // val k = thisParameter.`type`
    // val descr = config.minimal match {
    //   case true => None
    //   case false => Some(description)
    // }
    // val v = Json.obj(
    //   descr.map(s => Seq("description" -> Json.fromString(s))).getOrElse(Nil) ++
    //   Seq("type" -> Json.fromString("object"),
    //   "properties" -> Json.obj(propertiesJson: _*),
    //   "required" -> Json.arr(requiredJson: _*),
    //   "additionalProperties" -> Json.False): _*
    // )
    // k -> v
  }

  def createSuperClassSchema(info: List[ParameterSchema])(implicit config: SchemaConfig): (String, Json) = {
    val thisParameter = getThisParameter(info)
    val k = thisParameter.`type`
    implicit val mustIncludeAll = true
    val v = eitherJson(
      thisParameter.subclass.get.map(s => Json.obj("$ref" -> Json.fromString(s"#/definitions/$s"))): _*
    )
    k -> v
  }

  def createSchemas(data: List[List[ParameterSchema]])(implicit config: SchemaConfig) : Seq[(String, Json)] = {
    data.flatMap{
      case v if getThisParameter(v).`type` == "EnvironmentVariables" => None
      case v if getThisParameter(v).removed.isDefined || (getThisParameter(v).deprecated.isDefined && config.strict) => None
      case v if getThisParameter(v).subclass.map(_.length).getOrElse(0) > 1 => Some(createSuperClassSchema(v))
      case v => Some(createSchema(v))
    }
  }

  def createEnum(values: Seq[String], description: Option[String], comment: Option[String])(implicit config: SchemaConfig): Json = {
    val descr = config.minimal match {
      case true => None
      case false => description
    }
    val comm = config.minimal match {
      case true => None
      case false => comment
    }
    Json.obj(
      Seq("enum" -> Json.arr(values.map(s => Json.fromString(s)): _*)) ++
      comm.map(s => Seq("$comment" -> Json.fromString(s))).getOrElse(Nil) ++
      descr.map(s => Seq("description" -> Json.fromString(s))).getOrElse(Nil): _*
    )
  }

  def getJsonSchema(strict: Boolean, minimal: Boolean): Json = {
    implicit val configSchema = SchemaConfig(strict, minimal)
    
    val enumDefinitions = if (strict) {
      Seq(
        "DockerSetupStrategy" -> createEnum(DockerSetupStrategy.objs.map(obj => obj.id).toSeq, Some("The Docker setup strategy to use when building a container."), Some("TODO add descriptions to different strategies")),
        "Direction" -> createEnum(Seq("input", "output"), Some("Makes this argument an `input` or an `output`, as in does the file/folder needs to be read or written. `input` by default."), None),
        "Status" -> createEnum(Seq("enabled", "disabled", "deprecated"), Some("Allows setting a component to active, deprecated or disabled."), None),
        "DoubleStrings" -> createEnum(Seq("+infinity", "-infinity", "nan"), None, None)
      )
    } else {
      Seq(
        "DockerSetupStrategy" -> createEnum(DockerSetupStrategy.map.keys.toSeq, Some("The Docker setup strategy to use when building a container."), Some("TODO add descriptions to different strategies")),
        "Direction" -> createEnum(Seq("input", "output"), Some("Makes this argument an `input` or an `output`, as in does the file/folder needs to be read or written. `input` by default."), None),
        "Status" -> createEnum(Seq("enabled", "disabled", "deprecated"), Some("Allows setting a component to active, deprecated or disabled."), None),
        "DockerResolveVolume" -> createEnum(Seq("manual", "automatic", "auto", "Manual", "Automatic", "Auto"), Some("Enables or disables automatic volume mapping. Enabled when set to `Automatic` or disabled when set to `Manual`. Default: `Automatic`"), Some("TODO make fully case insensitive")),
        "DoubleStrings" -> createEnum(Seq("+.inf", "+inf", "+infinity", "positiveinfinity", "positiveinf", "-.inf", "-inf", "-infinity", "negativeinfinity", "negativeinf", ".nan", "nan"), None, None)
      )
    }

    val definitions =
      createSchemas(data) ++
      enumDefinitions ++
      {
        implicit val mustIncludeAll = true
        Seq("DoubleWithInf" -> eitherJson(valueType("Double_"), valueType("DoubleStrings")))
      }

    Json.obj(
      "$schema" -> Json.fromString("https://json-schema.org/draft-07/schema#"),
      "definitions" -> Json.obj(
        definitions: _*
      ),
      "oneOf" -> Json.arr(valueType("Config"))
    )
  }
}
