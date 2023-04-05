package io.viash.helpers.circe

import io.circe.Json
import io.circe.JsonObject
import org.scalatest.funsuite.AnyFunSuite

class RichJsonObjectTest extends AnyFunSuite {

  test("RichJsonObject.map should apply function to all key-value pairs") {
    val inputJson = JsonObject(
      "key1" -> Json.fromString("value1"),
      "key2" -> Json.fromInt(42)
    )

    val modifiedJson = inputJson.map {
      case (key, value) => (key.toUpperCase, value.mapString(_.toUpperCase()))
    }

    val expectedJson = JsonObject(
      "KEY1" -> Json.fromString("VALUE1"),
      "KEY2" -> Json.fromInt(42)
    )

    assert(modifiedJson == expectedJson)
  }

  test("RichJsonObject.map should return empty JsonObject when input JsonObject is empty") {
    val inputJson = JsonObject.empty

    val modifiedJson = inputJson.map {
      case (key, value) => (key.toUpperCase, value.mapString(_.toUpperCase()))
    }

    val expectedJson = JsonObject.empty

    assert(modifiedJson == expectedJson)
  }

  test("RichJsonObject.map should work with nested JsonObjects") {
    val inputJson = JsonObject(
      "key1" -> Json.fromString("value1"),
      "key2" -> Json.fromInt(42),
      "nested" -> Json.obj("nestedKey" -> Json.fromString("nestedValue"))
    )

    val modifiedJson = inputJson.map {
      case (key, value) => (key.toUpperCase, value.mapString(_.toUpperCase()))
    }

    val expectedJson = JsonObject(
      "KEY1" -> Json.fromString("VALUE1"),
      "KEY2" -> Json.fromInt(42),
      "NESTED" -> Json.obj("nestedKey" -> Json.fromString("nestedValue"))
    )

    assert(modifiedJson == expectedJson)
  }
}
