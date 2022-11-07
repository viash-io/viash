package io.viash.config_mods

import io.circe.Json
import org.scalatest.FunSuite
import io.circe.syntax._

import io.circe.yaml.parser.parse

class AppendTest extends FunSuite {
  // testing parsing
  test("parse append command") {
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

  test("parse add command with complex json") {
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

  // testing functionality
  val baseJson: Json = parse(
    """foo: bar
      |baz: 123
      |list_of_stuff: [4, 5, 6]
      |""".stripMargin).right.get
  
  test("test simple assign") {
    val expected1: Json = parse(
      """foo: 6
        |baz: 123
        |list_of_stuff: [4, 5, 6]
        |""".stripMargin).right.get
    val cmd1 = ConfigModParser.block.parse(""".foo := 6""")
    val res1 = cmd1.apply(baseJson, false)
    assert(res1 == expected1)
  }
  
  test("test simple assign 2") {
    val expected2: Json = parse(
      """foo: [1, 2, 3]
        |baz: 123
        |list_of_stuff: [4, 5, 6]
        |""".stripMargin).right.get
    val cmd2 = ConfigModParser.block.parse(""".foo := [ 1, 2, 3 ]""")
    val res2 = cmd2.apply(baseJson, false)
    assert(res2 == expected2)
  }
  
  test("test simple assign 3") {
    val expected2: Json = parse(
      """foo: bar
        |bar: [1, 2, 3]
        |baz: 123
        |list_of_stuff: [4, 5, 6]
        |""".stripMargin).right.get
    val cmd2 = ConfigModParser.block.parse(""".bar := [ 1, 2, 3 ]""")
    val res2 = cmd2.apply(baseJson, false)
    assert(res2 == expected2)
  }
}