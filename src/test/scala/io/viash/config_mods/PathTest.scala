package io.viash.config_mods

import io.circe.Json
import org.scalatest.funsuite.AnyFunSuite
import io.circe.syntax._

import io.circe.yaml.parser.parse
import io.viash.helpers.Logger

class PathTest extends AnyFunSuite {
  Logger.UseColorOverride.value = Some(false)
  // testing parsing

  test("test path parsing") {
    val expected = Path(List(Attribute("foo")))
    val result = ConfigModParser.parse(ConfigModParser.path, """.foo""").get
    assert(result == expected)
  }

  test("test filter path parsing") {
    val expected = Path(List(Attribute("fltr"), Filter(Equals(Path(List(Attribute("a"))),JsonValue(Json.True))), Attribute("b")))
    val result = ConfigModParser.parse(ConfigModParser.path, """.fltr[.a == true].b""").get
    assert(result == expected)
  }

  // testing functionality
  val baseJson: Json = parse(
    """foo: bar
      |baz: 123
      |list_of_stuff: [4, 5, 6]
      |fltr:
      |  - a: true
      |    b: 1
      |  - a: true
      |    b: 2
      |  - a: false
      |    b: 3
      |""".stripMargin).toOption.get
  
  test("test simple path") {
    val pth = ConfigModParser.parse(ConfigModParser.path, """.foo""").get
    val res3 = pth.get(baseJson)
    assert(res3 == Json.fromString("bar"))
  }

  test("test filter path") {
    val cmd1 = ConfigModParser.parse(ConfigModParser.path, """.fltr[.a == true].b""").get
    val res1 = cmd1.get(baseJson)
    assert(res1 == Json.arr(Json.fromInt(1), Json.fromInt(2)))

    val cmd2 = ConfigModParser.parse(ConfigModParser.path, """.fltr[.a == false].b""").get
    val res2 = cmd2.get(baseJson)
    assert(res2 == Json.arr(Json.fromInt(3)))
  }
}