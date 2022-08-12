package io.viash.config_mods

import io.circe.Json
import org.scalatest.FunSuite
import io.circe.syntax._

import io.circe.yaml.parser._

class ConfigModsSuite extends FunSuite {
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
    val cmd1 = ConfigModParser.parseBlock(""".foo := 6""")
    val res1 = cmd1.apply(baseJson, false)
    assert(res1 == expected1)
  }
  
  test("test simple assign 2") {
    val expected2: Json = parse(
      """foo: [1, 2, 3]
        |baz: 123
        |list_of_stuff: [4, 5, 6]
        |""".stripMargin).right.get
    val cmd2 = ConfigModParser.parseBlock(""".bar := [ 1, 2, 3 ]""")
    val res2 = cmd2.apply(baseJson, false)
    assert(res2 == expected2)
  }
  
  test("test simple path") {
    val pth = ConfigModParser.parse(ConfigModParser.path, """.foo""").get
    val res3 = pth.get(baseJson)
    assert(res3 == Json.fromString("bar"))
  }
}