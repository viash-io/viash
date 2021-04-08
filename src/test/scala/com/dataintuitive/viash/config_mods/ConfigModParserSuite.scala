package com.dataintuitive.viash.config_mods

import org.scalatest.FunSuite
import io.circe.syntax._

class ConfigModParserSuite extends FunSuite {

  test("set command with only attributes") {
    val expected = ConfigMods(List(
      ConfigMod(
        Path(List(Attribute("one"), Attribute("two"), Attribute("three"), Attribute("four"), Attribute("five"))),
        Modify(6.asJson)
      )
    ))
    val command = """.one.two.three.four.five := 6"""
    val result = ConfigModParser.parseBlock(command)
    assert(result == expected)
  }

  test("set command with both attribute and filter") {
    val expected = ConfigMods(List(
      ConfigMod(
        Path(List(Attribute("platforms"), Filter(Equals(Path(List(Attribute("type"))), JsonValue("native".asJson))), Attribute("id"))),
        Modify("test".asJson)
      )
    ))
    val command = """.platforms[.type == "native"].id := "test""""
    val result = ConfigModParser.parseBlock(command)
    assert(result == expected)

    // funky whitespacing also works
    val commandWs = """    .  platforms   [   . type   ==    "native"    ]  .  id   :=    "test"    """
    val resultWs = ConfigModParser.parseBlock(commandWs)
    assert(resultWs == expected)
  }

  test("set command with multiple filters") {
    val expected = ConfigMods(List(
      ConfigMod(
        Path(List(
          Filter(Equals(JsonValue("native".asJson), Path(List(Attribute("type"))))),
          Filter(Equals(Path(List(Attribute("foo"))), JsonValue("bar".asJson)))
        )),
        Modify("test".asJson)
      )
    ))
    val command = """["native" == .type][.foo == "bar"] := "test""""
    val result = ConfigModParser.parseBlock(command)
    assert(result == expected)
  }

  test("test json whole numbers") {
    val expected = ConfigMods(List(
      ConfigMod(
        Path(List(Attribute("x"), Filter(Equals(Path(List(Attribute("y"))), JsonValue(4.asJson))))),
        Modify(5.asJson)
      )
    ))
    val command = """.x[.y == 4] := 5"""
    val result = ConfigModParser.parseBlock(command)
    assert(result == expected)
  }

  test("test json real numbers") {
    val expected = ConfigMods(List(
      ConfigMod(
        Path(List(Attribute("x"), Filter(Equals(Path(List(Attribute("y"))), JsonValue(0.5.asJson))))),
        Modify(4.2e3.asJson)
      )
    ))
    val command = """.x[.y == 0.5] := 4.2e3"""
    val result = ConfigModParser.parseBlock(command)
    assert(result == expected)
  }

  test("test json booleans") {
    val expected = ConfigMods(List(
      ConfigMod(
        Path(List(Attribute("x"), Filter(Equals(Path(List(Attribute("y"))), JsonValue(true.asJson))))),
        Modify(false.asJson)
      )
    ))
    val command = """.x[.y == true] := false"""
    val result = ConfigModParser.parseBlock(command)
    assert(result == expected)
  }

  test("test condition booleans") {
    val expected = ConfigMods(List(
      ConfigMod(
        Path(List(Attribute("x"), Filter(True), Filter(False))),
        Modify("x".asJson)
      )
    ))
    val command = """.x[true][false] := "x""""
    val result = ConfigModParser.parseBlock(command)
    assert(result == expected)
  }

  test("test condition operators") {
    val expected = ConfigMods(List(
      ConfigMod(
        Path(List(Attribute("x"), Filter(And(True, Or(False, False))))),
        Modify("x".asJson)
      )
    ))
    val command = """.x[true && (false || false)] := "x""""
    val result = ConfigModParser.parseBlock(command)
    assert(result == expected)
  }
  // TODO: extend tests for more conditions
}