package io.viash.config_mods

import io.circe.Json
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import io.circe.syntax._

import io.circe.yaml.parser.parse

class ConditionSuite extends AnyFunSuite {
  // testing parsers
  // TODO

  // testing functionality
  val baseJson: Json = parse(
    """foo: bar
      |baz: 123
      |list_of_stuff: [4, 5, 6]
      |""".stripMargin).toOption.get
  
  test("test condition true") {
    val cmd1 = ConfigModParser.condition.parse("""true""")
    val res1 = cmd1.apply(baseJson)
    assert(res1 == true)
  }
  
  test("test condition false") {
    val cmd1 = ConfigModParser.condition.parse("""false""")
    val res1 = cmd1.apply(baseJson)
    assert(res1 == false)
  }
  
  test("test condition equals") {
    val cmd1 = ConfigModParser.condition.parse(""".foo == "bar"""")
    val res1 = cmd1.apply(baseJson)
    assert(res1 == true)

    val cmd2 = ConfigModParser.condition.parse(""".foo == "bing"""")
    val res2 = cmd2.apply(baseJson)
    assert(res2 == false)

    val cmd3 = ConfigModParser.condition.parse(""".baz == 123""")
    val res3 = cmd3.apply(baseJson)
    assert(res3 == true)

    val cmd4 = ConfigModParser.condition.parse(""".baz == 456""")
    val res4 = cmd4.apply(baseJson)
    assert(res4 == false)

    val cmd5 = ConfigModParser.condition.parse(""".foo == .foo""")
    val res5 = cmd5.apply(baseJson)
    assert(res5 == true)

    val cmd6 = ConfigModParser.condition.parse(""".foo == .baz""")
    val res6 = cmd6.apply(baseJson)
    assert(res6 == false)
  }
  
  test("test condition not equals") {
    val cmd1 = ConfigModParser.condition.parse(""".foo != "bar"""")
    val res1 = cmd1.apply(baseJson)
    assert(res1 == false)

    val cmd2 = ConfigModParser.condition.parse(""".foo != "bing"""")
    val res2 = cmd2.apply(baseJson)
    assert(res2 == true)

    val cmd3 = ConfigModParser.condition.parse(""".baz != 123""")
    val res3 = cmd3.apply(baseJson)
    assert(res3 == false)

    val cmd4 = ConfigModParser.condition.parse(""".baz != 456""")
    val res4 = cmd4.apply(baseJson)
    assert(res4 == true)

    val cmd5 = ConfigModParser.condition.parse(""".foo != .foo""")
    val res5 = cmd5.apply(baseJson)
    assert(res5 == false)

    val cmd6 = ConfigModParser.condition.parse(""".foo != .baz""")
    val res6 = cmd6.apply(baseJson)
    assert(res6 == true)
  }
  
  test("test condition has") {
    val cmd1 = ConfigModParser.condition.parse("""has(.foo)""")
    val res1 = cmd1.apply(baseJson)
    assert(res1 == true)
    
    val cmd2 = ConfigModParser.condition.parse("""has(.fang)""")
    val res2 = cmd2.apply(baseJson)
    assert(res2 == false)
  }
  
  test("test condition and") {
    val cmd1 = ConfigModParser.condition.parse(""".foo == "bar" && true""")
    val res1 = cmd1.apply(baseJson)
    assert(res1 == true) // true && true

    val cmd2 = ConfigModParser.condition.parse("""false && has(.baz)""")
    val res2 = cmd2.apply(baseJson)
    assert(res2 == false) // false && true

    val cmd3 = ConfigModParser.condition.parse(""".foo != "ping" && .foo == "ping"""")
    val res3 = cmd3.apply(baseJson)
    assert(res3 == false) // true && false

    // disabled until viash-io/viash#292 is resolved
    // val cmd4 = ConfigModParser.condition.parse(""".foo == "ping" && false"""")
    // val res4 = cmd4.apply(baseJson)
    // assert(res4 == false) // false && false
  }
  
  test("test condition or") {
    val cmd1 = ConfigModParser.condition.parse(""".foo == "bar" || true""")
    val res1 = cmd1.apply(baseJson)
    assert(res1 == true) // true || true

    val cmd2 = ConfigModParser.condition.parse("""false || has(.baz)""")
    val res2 = cmd2.apply(baseJson)
    assert(res2 == true) // false || true

    val cmd3 = ConfigModParser.condition.parse(""".foo != "ping" || .foo == "ping"""")
    val res3 = cmd3.apply(baseJson)
    assert(res3 == true) // true || false

    val cmd4 = ConfigModParser.condition.parse(""".foo == "ping" || false""")
    val res4 = cmd4.apply(baseJson)
    assert(res4 == false) // false || false
  }
  
  test("test condition not") {
    val cmd1 = ConfigModParser.condition.parse("""! .foo == "bar"""")
    val res1 = cmd1.apply(baseJson)
    assert(res1 == false)

    val cmd2 = ConfigModParser.condition.parse("""! false""")
    val res2 = cmd2.apply(baseJson)
    assert(res2 == true)
  }
}
