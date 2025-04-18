package io.viash.config_mods

import io.circe.Json
import org.scalatest.funsuite.AnyFunSuite
import io.circe.syntax._
import io.viash.helpers.Logger

class ConfigModsTest extends AnyFunSuite {
  Logger.UseColorOverride.value = Some(false)
  // testing parsers
  test("parsing multiple commands in one go") {
    val expected = ConfigMods(
      postparseCommands = List(
        Append(
          Path(List(Attribute("engines"), Filter(Equals(Path(List(Attribute("type"))), JsonValue("native".asJson))), Attribute("setup"))),
          JsonValue(Json.fromFields(List("type" -> "docker".asJson, "image" -> Json.fromString("foo"))))
        ),
        Delete(
          Path(List(Attribute("x")))
        )
      ),
      preparseCommands = List(
        Prepend(
          Path(List(Attribute("engines"), Filter(Equals(Path(List(Attribute("type"))), JsonValue("native".asJson))), Attribute("setup"))),
          JsonValue("foo".asJson)
        )
      )
    )

    val command = 
      """.engines[.type == "native"].setup += { type: "docker", image: "foo" };
        |<preparse> .engines[.type == "native"].setup +0= "foo";
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
          Path(List(Attribute("engines"), Filter(Equals(Path(List(Attribute("type"))), JsonValue("native".asJson))), Attribute("setup"))),
          JsonValue(Json.fromFields(List("type" -> "docker".asJson, "image" -> Json.fromString("foo"))))
        ),
        Delete(
          Path(List(Attribute("x")))
        )
      ),
      preparseCommands = List(
        Prepend(
          Path(List(Attribute("engines"), Filter(Equals(Path(List(Attribute("type"))), JsonValue("native".asJson))), Attribute("setup"))),
          JsonValue("foo".asJson)
        )
      )
    )
    val command = """
      |.engines[
      |  .type == "native"
      |].setup += { 
      |  type: "docker",
      |  image: "foo"
      |}
      |
      |<preparse> .engines[.type == "native"].setup +0= "foo"; del(.x)
      |""".stripMargin
    val result = ConfigModParser.block.parse(command)
    assert(result == expected)
  }
  */

  // testing functionality
  // TODO
}