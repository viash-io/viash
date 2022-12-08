package io.viash.config_mods

import io.circe.Json
import org.scalatest.FunSuite
import io.circe.syntax._

class PrependSuite extends FunSuite {
  // testing parsing
  test("prepend command") {
    val expected = ConfigMods(List(
      Prepend(
        Path(List(Attribute("platforms"), Filter(Equals(Path(List(Attribute("type"))), JsonValue("native".asJson))), Attribute("setup"))),
        JsonValue("foo".asJson)
      )
    ))
    val command = """.platforms[.type == "native"].setup +0= "foo""""
    val result = ConfigModParser.block.parse(command)
    assert(result == expected)
  }

  test("preparse prepend command") {
    val expected = ConfigMods(preparseCommands = List(
      Prepend(
        Path(List(Attribute("platforms"), Filter(Equals(Path(List(Attribute("type"))), JsonValue("native".asJson))), Attribute("setup"))),
        JsonValue("foo".asJson)
      )
    ))
    val command = """<preparse> .platforms[.type == "native"].setup +0= "foo""""
    val result = ConfigModParser.block.parse(command)
    assert(result == expected)
  }

  // testing functionality
  // TODO
}