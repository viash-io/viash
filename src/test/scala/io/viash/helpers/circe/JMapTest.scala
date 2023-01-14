package io.viash.helpers.circe

import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import io.circe._
import io.circe.yaml.{parser => YamlParser}

class JMapTest extends AnyFunSuite with BeforeAndAfterAll {
  test("checking whether JMap works") {
    val out = JMap(
      "foo" -> JMap(
        "bar" -> JMap(
          "baz" -> Json.fromInt(10)
        )
      ), 
      "arg" -> Json.fromString("aaa")
    )
    val expectedOut = Yaml("""
      |foo:
      |  bar:
      |    baz:
      |      10
      |arg: aaa
      """.stripMargin)

    assert(out == expectedOut)
  }
}