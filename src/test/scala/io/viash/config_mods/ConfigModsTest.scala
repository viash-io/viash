package io.viash.config_mods

import io.circe.Json
import org.scalatest.FunSuite
import io.circe.syntax._

class ConfigModsTest extends FunSuite {
  // testing parsers
  test("parsing multiple commands in one go") {
    val expected = ConfigMods(
      postparseCommands = List(
        Append(
          Path(List(Attribute("platforms"), Filter(Equals(Path(List(Attribute("type"))), JsonValue("native".asJson))), Attribute("setup"))),
          JsonValue(Json.fromFields(List("type" -> "docker".asJson, "image" -> Json.fromString("foo"))))
        ),
        Delete(
          Path(List(Attribute("x")))
        )
      ),
      preparseCommands = List(
        Prepend(
          Path(List(Attribute("platforms"), Filter(Equals(Path(List(Attribute("type"))), JsonValue("native".asJson))), Attribute("setup"))),
          JsonValue("foo".asJson)
        )
      )
    )

    val command = 
      """.platforms[.type == "native"].setup += { type: "docker", image: "foo" };
        |<preparse> .platforms[.type == "native"].setup +0= "foo";
        |del(.x)""".stripMargin
    val result = ConfigModParser.block.parse(command)
    assert(result == expected)
  }


  // TODO: re-enable this when https://github.com/viash-io/viash/issues/284 is solved
  /*
  test("parsing multiple commands with newlines and semicolons") {
    val expected = ConfigMods(
      postparseCommands = List(
        Append(
          Path(List(Attribute("platforms"), Filter(Equals(Path(List(Attribute("type"))), JsonValue("native".asJson))), Attribute("setup"))),
          JsonValue(Json.fromFields(List("type" -> "docker".asJson, "image" -> Json.fromString("foo"))))
        ),
        Delete(
          Path(List(Attribute("x")))
        )
      ),
      preparseCommands = List(
        Prepend(
          Path(List(Attribute("platforms"), Filter(Equals(Path(List(Attribute("type"))), JsonValue("native".asJson))), Attribute("setup"))),
          JsonValue("foo".asJson)
        )
      )
    )
    val command = """
      |.platforms[
      |  .type == "native"
      |].setup += { 
      |  type: "docker",
      |  image: "foo"
      |}
      |
      |<preparse> .platforms[.type == "native"].setup +0= "foo"; del(.x)
      |""".stripMargin
    val result = ConfigModParser.block.parse(command)
    assert(result == expected)
  }
  */

  // testing functionality
  // TODO
}