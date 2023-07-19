package io.viash.config_mods

import io.circe.Json
import org.scalatest.funsuite.AnyFunSuite
import io.circe.syntax._

import io.circe.yaml.parser.parse
import io.viash.helpers.Logger

class PathTest extends AnyFunSuite {
  Logger.UseColorOverride.value = Some(false)
  // testing parsing
  // TODO

  // testing functionality
  val baseJson: Json = parse(
    """foo: bar
      |baz: 123
      |list_of_stuff: [4, 5, 6]
      |""".stripMargin).toOption.get
  
  test("test simple path") {
    val pth = ConfigModParser.parse(ConfigModParser.path, """.foo""").get
    val res3 = pth.get(baseJson)
    assert(res3 == Json.fromString("bar"))
  }
}