package io.viash.config_mods

import io.circe.Json
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import io.circe.syntax._

class ConfigModParserSuite extends AnyFunSuite {

  test("set command with only attributes") {
    val expected = ConfigMods(List(
      Assign(
        Path(List(Attribute("one"), Attribute("two"), Attribute("three"), Attribute("four"), Attribute("five"))),
        JsonValue(6.asJson)
      )
    ))
    val command = """.one.two.three.four.five := 6"""
    val result = ConfigModParser.block.parse(command)
    assert(result == expected)
  }

  test("set command with both attribute and filter") {
    val expected = ConfigMods(List(
      Assign(
        Path(List(Attribute("platforms"), Filter(Equals(Path(List(Attribute("type"))), JsonValue("native".asJson))), Attribute("id"))),
        JsonValue("test".asJson)
      )
    ))
    val command = """.platforms[.type == "native"].id := "test""""
    val result = ConfigModParser.block.parse(command)
    assert(result == expected)

    // funky whitespacing also works
    val commandWs = """    .  platforms   [   . type   ==    "native"    ]  .  id   :=    "test"    """
    val resultWs = ConfigModParser.block.parse(commandWs)
    assert(resultWs == expected)
  }

  test("set command with multiple filters") {
    val expected = ConfigMods(List(
      Assign(
        Path(List(
          Filter(Equals(JsonValue("native".asJson), Path(List(Attribute("type"))))),
          Filter(Equals(Path(List(Attribute("foo"))), JsonValue("bar".asJson)))
        )),
        JsonValue("test".asJson)
      )
    ))
    val command = """["native" == .type][.foo == "bar"] := "test""""
    val result = ConfigModParser.block.parse(command)
    assert(result == expected)
  }

  test("test json whole numbers") {
    val expected = ConfigMods(List(
      Assign(
        Path(List(Attribute("x"), Filter(Equals(Path(List(Attribute("y"))), JsonValue(4.asJson))))),
        JsonValue(5.asJson)
      )
    ))
    val command = """.x[.y == 4] := 5"""
    val result = ConfigModParser.block.parse(command)
    assert(result == expected)
  }

  test("test json real numbers") {
    val expected = ConfigMods(List(
      Assign(
        Path(List(Attribute("x"), Filter(Equals(Path(List(Attribute("y"))), JsonValue(0.5.asJson))))),
        JsonValue(4.2e3.asJson)
      )
    ))
    val command = """.x[.y == 0.5] := 4.2e3"""
    val result = ConfigModParser.block.parse(command)
    assert(result == expected)
  }

  test("test json booleans") {
    val expected = ConfigMods(List(
      Assign(
        Path(List(Attribute("x"), Filter(Equals(Path(List(Attribute("y"))), JsonValue(true.asJson))))),
        JsonValue(false.asJson)
      )
    ))
    val command = """.x[.y == true] := false"""
    val result = ConfigModParser.block.parse(command)
    assert(result == expected)
  }

  test("test condition booleans") {
    val expected = ConfigMods(List(
      Assign(
        Path(List(Attribute("x"), Filter(True), Filter(False))),
        JsonValue("x".asJson)
      )
    ))
    val command = """.x[true][false] := "x""""
    val result = ConfigModParser.block.parse(command)
    assert(result == expected)
  }

  test("test condition operators") {
    val expected = ConfigMods(List(
      Assign(
        Path(List(Attribute("x"), Filter(And(True, Or(False, False))))),
        JsonValue("x".asJson)
      )
    ))
    val command = """.x[true && (false || false)] := "x""""
    val result = ConfigModParser.block.parse(command)
    assert(result == expected)
  }

  test("append command") {
    val expected = ConfigMods(List(
      Append(
        Path(List(Attribute("platforms"), Filter(Equals(Path(List(Attribute("type"))), JsonValue("native".asJson))), Attribute("id"))),
        JsonValue("test".asJson)
      )
    ))
    val command = """.platforms[.type == "native"].id += "test""""
    val result = ConfigModParser.block.parse(command)
    assert(result == expected)
  }

  test("add command with complex json") {
    val expected = ConfigMods(List(
      Append(
        Path(List(Attribute("platforms"), Filter(Equals(Path(List(Attribute("type"))), JsonValue("native".asJson))), Attribute("setup"))),
        JsonValue(Json.fromFields(List(("type", "docker".asJson))))
      )
    ))
    val command = """.platforms[.type == "native"].setup += { type: "docker" }"""
    val result = ConfigModParser.block.parse(command)
    assert(result == expected)
  }

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
  
  test("test delete command") {
    val expected = ConfigMods(List(
      Delete(
        Path(List(Attribute("x")))
      )
    ))
    val command = """del(.x)"""
    val result = ConfigModParser.block.parse(command)
    assert(result == expected)
  }
}