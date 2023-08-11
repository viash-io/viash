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
      |    a: 2
      |  - name: qux
      |    a: 3
      |""".stripMargin).toOption.get

  test("test delete single entry from a list #1") {
    val expected1: Json = parse(
      """foo:
        |  - name: baz
        |    a: 2
        |  - name: qux
        |    a: 3
        |""".stripMargin).toOption.get
    val cmd1 = ConfigModParser.block.parse("""del(.foo[.a == 1])""")
    val res1 = cmd1.apply(baseJson, false)
    assert(res1 == expected1)
  }
  
  test("test delete single entry from a list #2") {
    val expected1: Json = parse(
      """foo:
        |  - name: bar
        |    a: 1
        |  - name: qux
        |    a: 3
        |""".stripMargin).toOption.get
    val cmd1 = ConfigModParser.block.parse("""del(.foo[.a == 2])""")
    val res1 = cmd1.apply(baseJson, false)
    assert(res1 == expected1)
  }

  test("test delete single entry from a list #3") {
    val expected1: Json = parse(
      """foo:
        |  - name: bar
        |    a: 1
        |  - name: baz
        |    a: 2
        |""".stripMargin).toOption.get
    val cmd1 = ConfigModParser.block.parse("""del(.foo[.a == 3])""")
    val res1 = cmd1.apply(baseJson, false)
    assert(res1 == expected1)
  }

  test("test delete field from a single list entry #1") {
    val expected1: Json = parse(
      """foo:
        |  - name: bar
        |  - name: baz
        |    a: 2
        |  - name: qux
        |    a: 3
        |""".stripMargin).toOption.get
    val cmd1 = ConfigModParser.block.parse("""del(.foo[.a == 1].a)""")
    val res1 = cmd1.apply(baseJson, false)
    assert(res1 == expected1)
  }

  test("test delete field from a single list entry #2") {
    val expected1: Json = parse(
      """foo:
        |  - name: bar
        |    a: 1
        |  - name: baz
        |  - name: qux
        |    a: 3
        |""".stripMargin).toOption.get
    val cmd1 = ConfigModParser.block.parse("""del(.foo[.a == 2].a)""")
    val res1 = cmd1.apply(baseJson, false)
    assert(res1 == expected1)
  }

  test("test delete field from a single list entry #3") {
    val expected1: Json = parse(
      """foo:
        |  - name: bar
        |    a: 1
        |  - name: baz
        |    a: 2
        |  - name: qux
        |""".stripMargin).toOption.get
    val cmd1 = ConfigModParser.block.parse("""del(.foo[.a == 3].a)""")
    val res1 = cmd1.apply(baseJson, false)
    assert(res1 == expected1)
  }

  test("test delete multiple entries from a list #1") {
    val expected1: Json = parse(
      """foo:
        |  - name: bar
        |    a: 1
        |""".stripMargin).toOption.get
    val cmd1 = ConfigModParser.block.parse("""del(.foo[.a != 1])""")
    val res1 = cmd1.apply(baseJson, false)
    assert(res1 == expected1)
  }

  test("test delete multiple entries from a list #2") {
    val expected1: Json = parse(
      """foo:
        |  - name: baz
        |    a: 2
        |""".stripMargin).toOption.get
    val cmd1 = ConfigModParser.block.parse("""del(.foo[.a != 2])""")
    val res1 = cmd1.apply(baseJson, false)
    assert(res1 == expected1)
  }

  test("test delete multiple entries from a list #3") {
    val expected1: Json = parse(
      """foo:
        |  - name: qux
        |    a: 3
        |""".stripMargin).toOption.get
    val cmd1 = ConfigModParser.block.parse("""del(.foo[.a != 3])""")
    val res1 = cmd1.apply(baseJson, false)
    assert(res1 == expected1)
  }

  test("test delete field from multiple list entries #1") {
    val expected1: Json = parse(
      """foo:
        |  - name: bar
        |    a: 1
        |  - name: baz
        |  - name: qux
        |""".stripMargin).toOption.get
    val cmd1 = ConfigModParser.block.parse("""del(.foo[.a != 1].a)""")
    val res1 = cmd1.apply(baseJson, false)
    assert(res1 == expected1)
  }

  test("test delete field from multiple list entries #2") {
    val expected1: Json = parse(
      """foo:
        |  - name: bar
        |  - name: baz
        |    a: 2
        |  - name: qux
        |""".stripMargin).toOption.get
    val cmd1 = ConfigModParser.block.parse("""del(.foo[.a != 2].a)""")
    val res1 = cmd1.apply(baseJson, false)
    assert(res1 == expected1)
  }
  test("test delete field from multiple list entries #3") {
    val expected1: Json = parse(
      """foo:
        |  - name: bar
        |  - name: baz
        |  - name: qux
        |    a: 3
        |""".stripMargin).toOption.get
    val cmd1 = ConfigModParser.block.parse("""del(.foo[.a != 3].a)""")
    val res1 = cmd1.apply(baseJson, false)
    assert(res1 == expected1)
  }

  test("test delete all entries from a list") {
    val expected1: Json = parse(
      """foo: []
        |""".stripMargin).toOption.get
    val cmd1 = ConfigModParser.block.parse("""del(.foo[true])""")
    val res1 = cmd1.apply(baseJson, false)
    assert(res1 == expected1)
  }

  test("test delete field from all list entries") {
    val expected1: Json = parse(
      """foo:
        |  - name: bar
        |  - name: baz
        |  - name: qux
        |""".stripMargin).toOption.get
    val cmd1 = ConfigModParser.block.parse("""del(.foo[true].a)""")
    val res1 = cmd1.apply(baseJson, false)
    assert(res1 == expected1)
  }
  
}