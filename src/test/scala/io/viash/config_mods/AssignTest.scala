package io.viash.config_mods

import io.circe.Json
import org.scalatest.funsuite.AnyFunSuite
import io.circe.syntax._

import io.circe.yaml.parser.parse
import io.viash.helpers.Logger

class AssignTest extends AnyFunSuite {
  Logger.UseColorOverride.value = Some(false)
  // testing parsing
  test("parse assign command with only attributes") {
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

  test("parse assign command with both attribute and filter") {
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

  test("parse assign command with multiple filters") {
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

  test("parse assign with json whole numbers") {
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

  test("parse assign with json real numbers") {
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

  test("parse assign with json booleans") {
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

  test("parse assign with condition booleans") {
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

  test("parse assign with condition operators") {
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

  test("test assign single entry from a list #1") {
    val expected1: Json = parse(
      """foo:
        |  - name: quux
        |    a: 5
        |  - name: baz
        |    a: 2
        |  - name: qux
        |    a: 3
        |""".stripMargin).toOption.get
    val cmd1 = ConfigModParser.block.parse(""".foo[.a == 1] := { name: "quux", a: 5 }""")
    val res1 = cmd1.apply(baseJson, false)
    assert(res1 == expected1)
  }
  
  test("test assign single entry from a list #2") {
    val expected1: Json = parse(
      """foo:
        |  - name: bar
        |    a: 1
        |  - name: quux
        |    a: 6
        |  - name: qux
        |    a: 3
        |""".stripMargin).toOption.get
    val cmd1 = ConfigModParser.block.parse(""".foo[.a == 2] := { name: "quux", a: 6 }""")
    val res1 = cmd1.apply(baseJson, false)
    assert(res1 == expected1)
  }

  test("test assign single entry from a list #3") {
    val expected1: Json = parse(
      """foo:
        |  - name: bar
        |    a: 1
        |  - name: baz
        |    a: 2
        |  - name: quux
        |    a: 7
        |""".stripMargin).toOption.get
    val cmd1 = ConfigModParser.block.parse(""".foo[.a == 3] := { name: "quux", a: 7 }""")
    val res1 = cmd1.apply(baseJson, false)
    assert(res1 == expected1)
  }

  test("test assign field from a single list entry #1") {
    val expected1: Json = parse(
      """foo:
        |  - name: bar
        |    a: 5
        |  - name: baz
        |    a: 2
        |  - name: qux
        |    a: 3
        |""".stripMargin).toOption.get
    val cmd1 = ConfigModParser.block.parse(""".foo[.a == 1].a := 5""")
    val res1 = cmd1.apply(baseJson, false)
    assert(res1 == expected1)
  }

  test("test assign field from a single list entry #2") {
    val expected1: Json = parse(
      """foo:
        |  - name: bar
        |    a: 1
        |  - name: baz
        |    a: 6
        |  - name: qux
        |    a: 3
        |""".stripMargin).toOption.get
    val cmd1 = ConfigModParser.block.parse(""".foo[.a == 2].a := 6""")
    val res1 = cmd1.apply(baseJson, false)
    assert(res1 == expected1)
  }

  test("test assign field from a single list entry #3") {
    val expected1: Json = parse(
      """foo:
        |  - name: bar
        |    a: 1
        |  - name: baz
        |    a: 2
        |  - name: qux
        |    a: 7
        |""".stripMargin).toOption.get
    val cmd1 = ConfigModParser.block.parse(""".foo[.a == 3].a := 7""")
    val res1 = cmd1.apply(baseJson, false)
    assert(res1 == expected1)
  }

  test("test assign multiple entries from a list #1") {
    val expected1: Json = parse(
      """foo:
        |  - name: bar
        |    a: 1
        |  - name: quux
        |    a: 5
        |  - name: quux
        |    a: 5
        |""".stripMargin).toOption.get
    val cmd1 = ConfigModParser.block.parse(""".foo[.a != 1] := { name: "quux", a: 5 }""")
    val res1 = cmd1.apply(baseJson, false)
    assert(res1 == expected1)
  }

  test("test assign multiple entries from a list #2") {
    val expected1: Json = parse(
      """foo:
        |  - name: quux
        |    a: 6
        |  - name: baz
        |    a: 2
        |  - name: quux
        |    a: 6
        |""".stripMargin).toOption.get
    val cmd1 = ConfigModParser.block.parse(""".foo[.a != 2] := { name: "quux", a: 6 }""")
    val res1 = cmd1.apply(baseJson, false)
    assert(res1 == expected1)
  }

  test("test assign multiple entries from a list #3") {
    val expected1: Json = parse(
      """foo:
        |  - name: quux
        |    a: 7
        |  - name: quux
        |    a: 7
        |  - name: qux
        |    a: 3
        |""".stripMargin).toOption.get
    val cmd1 = ConfigModParser.block.parse(""".foo[.a != 3] := { name: "quux", a: 7 }""")
    val res1 = cmd1.apply(baseJson, false)
    assert(res1 == expected1)
  }

  test("test assign field from multiple list entries #1") {
    val expected1: Json = parse(
      """foo:
        |  - name: bar
        |    a: 1
        |  - name: baz
        |    a: 5
        |  - name: qux
        |    a: 5
        |""".stripMargin).toOption.get
    val cmd1 = ConfigModParser.block.parse(""".foo[.a != 1].a := 5""")
    val res1 = cmd1.apply(baseJson, false)
    assert(res1 == expected1)
  }

  test("test assign field from multiple list entries #2") {
    val expected1: Json = parse(
      """foo:
        |  - name: bar
        |    a: 6
        |  - name: baz
        |    a: 2
        |  - name: qux
        |    a: 6
        |""".stripMargin).toOption.get
    val cmd1 = ConfigModParser.block.parse(""".foo[.a != 2].a := 6""")
    val res1 = cmd1.apply(baseJson, false)
    assert(res1 == expected1)
  }
  test("test assign field from multiple list entries #3") {
    val expected1: Json = parse(
      """foo:
        |  - name: bar
        |    a: 7
        |  - name: baz
        |    a: 7
        |  - name: qux
        |    a: 3
        |""".stripMargin).toOption.get
    val cmd1 = ConfigModParser.block.parse(""".foo[.a != 3].a := 7""")
    val res1 = cmd1.apply(baseJson, false)
    assert(res1 == expected1)
  }

  test("test assign all entries from a list") {
    val expected1: Json = parse(
      """foo:
        |  - name: quux
        |    a: 5
        |  - name: quux
        |    a: 5
        |  - name: quux
        |    a: 5
        |""".stripMargin).toOption.get
    val cmd1 = ConfigModParser.block.parse(""".foo[true] := { name: "quux", a: 5 }""")
    val res1 = cmd1.apply(baseJson, false)
    assert(res1 == expected1)
  }

  test("test assign field from all list entries") {
    val expected1: Json = parse(
      """foo:
        |  - name: bar
        |    a: 5
        |  - name: baz
        |    a: 5
        |  - name: qux
        |    a: 5
        |""".stripMargin).toOption.get
    val cmd1 = ConfigModParser.block.parse(""".foo[true].a := 5""")
    val res1 = cmd1.apply(baseJson, false)
    assert(res1 == expected1)
  }
}