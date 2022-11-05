package io.viash.config_mods

import io.circe.Json
import org.scalatest.FunSuite
import io.circe.syntax._

import io.circe.yaml.parser._

class PathTest extends FunSuite {
  // testing parsing
  // TODO

  // testing functionality
  val baseJson: Json = parse(
    """foo: bar
      |baz: 123
      |list_of_stuff: [4, 5, 6]
      |""".stripMargin).right.get
  
  test("test simple path") {
    val pth = ConfigModParser.parse(ConfigModParser.path, """.foo""").get
    val res3 = pth.get(baseJson)
    assert(res3 == Json.fromString("bar"))
  }
}