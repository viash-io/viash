package io.viash.config_mods

import io.circe.Json
import org.scalatest.FunSuite
import io.circe.syntax._

class ConfigModsTest extends FunSuite {
  // testing parsers
  test("parsing multiple commands in one go") {
    val expected = ConfigMods(List(
      Append(
        Path(List(Attribute("platforms"), Filter(Equals(Path(List(Attribute("type"))), JsonValue("native".asJson))), Attribute("setup"))),
        JsonValue(Json.fromFields(List("type" -> "docker".asJson, "image" -> Json.fromString("foo"))))
      ),
      Prepend(
        Path(List(Attribute("platforms"), Filter(Equals(Path(List(Attribute("type"))), JsonValue("native".asJson))), Attribute("setup"))),
        JsonValue("foo".asJson)
      ),
      Delete(
        Path(List(Attribute("x")))
      )
    ))
    val command = """
      .platforms[
        .type == "native"
      ].setup += { 
        type: "docker"
        image: "foo"
      }

      <preparse> .platforms[.type == "native"].setup +0= "foo"; del(.x)
    """.stripMargin
    val result = ConfigModParser.block.parse(command)
    assert(result == expected)
  }

  // testing functionality
  // TODO
}