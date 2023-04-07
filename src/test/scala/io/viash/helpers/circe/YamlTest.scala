package io.viash.helpers.circe

import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import io.circe._
import io.circe.yaml.{parser => YamlParser}

class YamlTest extends AnyFunSuite with BeforeAndAfterAll {
  test("checking whether Yaml works") {
    val out = Yaml("""
      |foo:
      |  bar:
      |    baz:
      |      10
      |arg: aaa
      """.stripMargin)
    val expectedOut = Json.fromJsonObject(JsonObject(
      "foo" -> Json.fromJsonObject(JsonObject(
        "bar" -> Json.fromJsonObject(JsonObject(
          "baz" -> Json.fromInt(10)
        ))
      )),
      "arg" -> Json.fromString("aaa")
    ))

    assert(out == expectedOut)
  }
}