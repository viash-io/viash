package io.viash.schemas

import io.circe.Json

object JsonSchema {

  lazy val data = CollectedSchemas.data

  def typeOrRefJson(`type`: String): (String, Json) = {
    `type` match {
      case s @ (_: "String" | _: "Boolean") =>
        "type" -> Json.fromString(s.toLowerCase())
      case s =>
        "$ref" -> Json.fromString("#/definitions/" + s)
    }
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

  def createSchema(info: List[ParameterSchema]): Json = {
    val description = info.find(p => p.name == "__this__").get.description.get
    val properties = info.filter(p => !p.name.startsWith("__"))
    val propertiesJson = properties.map(p => {
      val pDescription = p.description.getOrElse("")
      val trimmedType = p.`type`.stripPrefix("Option of ")

      trimmedType match {
        case s"List of $s" => 
          (p.name, arrayJson(pDescription, s))

        case s"OneOrMore of $s" =>
          (p.name, Json.obj("anyOf" -> Json.arr(
            valueJson(pDescription, s),
            arrayJson(pDescription, s)
          )))

        case s =>
          (p.name, Json.obj(
            "description" -> Json.fromString(pDescription),
            typeOrRefJson(s)
          ))
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
        "NativePlatform" -> createSchema(data.platforms.get("nativePlatform").get),
        "DockerPlatform" -> createSchema(data.platforms.get("dockerPlatform").get),
        "NextflowVdsl3Platform" -> createSchema(data.platforms.get("nextflowVdsl3Platform").get)

      ),
      "properties" -> Json.obj(
        "functionality" -> valueJson(data.functionality.get("functionality").get.head.description.get, "Functionality"),
        "platforms" -> Json.obj(
          "anyOf" -> Json.arr(
            Json.fromString("#/definitions/NativePlatform"),
            Json.fromString("#/definitions/DockerPlatform"),
            Json.fromString("#/definitions/NextflowVdsl3Platform")
          )
        ),
        "info" -> valueJson("Definition of meta data", "Info")
      ),
      "required" -> Json.arr(Json.fromString("functionality")),
      "additionalProperties" -> Json.False,
    )

    schema
  }
}
