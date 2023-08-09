package io.viash.config_mods

import io.circe.Json
import org.scalatest.funsuite.AnyFunSuite
import io.circe.syntax._
import io.viash.helpers.Logger

import io.circe.yaml.parser.parse

class DeleteTest extends AnyFunSuite {
  Logger.UseColorOverride.value = Some(false)
  // testing parsing
  test("parsing delete command") {
    val expected = ConfigMods(List(
      Delete(
        Path(List(Attribute("x")))
      )
    ))
    val command = """del(.x)"""
    val result = ConfigModParser.block.parse(command)
    assert(result == expected)
  }

  // testing functionality
  val baseJson: Json = parse(
    """foo:
      |  - name: bar
      |    a: 1
      |  - name: baz
      |    a: 1
      |  - name: qux
      |    a: 2
      |""".stripMargin).toOption.get
  
  test("test delete single entry from a list") {
    val expected1: Json = parse(
      """foo:
        |  - name: bar
        |    a: 1
        |  - name: baz
        |    a: 1
        |""".stripMargin).toOption.get
    val cmd1 = ConfigModParser.block.parse("""del(.foo[.a == 2])""")
    val res1 = cmd1.apply(baseJson, false)
    assert(res1 == expected1)
  }

  test("test delete multiple entries from a list") {
    val expected1: Json = parse(
      """foo:
        |  - name: qux
        |    a: 2
        |""".stripMargin).toOption.get
    val cmd1 = ConfigModParser.block.parse("""del(.foo[.a == 1])""")
    val res1 = cmd1.apply(baseJson, false)
    assert(res1 == expected1)
  }
  
}