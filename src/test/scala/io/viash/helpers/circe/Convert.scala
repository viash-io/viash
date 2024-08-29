package io.viash.helpers.circe

import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import io.circe._
import io.viash.exceptions.{ConfigYamlException, ConfigParserException}

// import shapeless.Lazy
// import scala.reflect.runtime.universe._

import io.circe.Decoder
// import io.circe.generic.extras.decoding.ConfiguredDecoder
// import io.circe.generic.extras.semiauto.deriveConfiguredDecoder
import io.viash.helpers.circe.DeriveConfiguredDecoder._
import io.viash.helpers.circe.DeriveConfiguredEncoder._
import io.viash.helpers.Logger

class ConvertTest extends AnyFunSuite with BeforeAndAfterAll {
  Logger.UseColorOverride.value = Some(false)

  case class Foo (
    a: String,
    b: Int
  )
  implicit val decodeFoo: Decoder[Foo] = deriveConfiguredDecoder


  test("can convert valid yaml") {
    val inputText = """
      |a: foo
      |b: 5
      """.stripMargin
    val expectedOut = Json.fromJsonObject(JsonObject(
      "a" -> Json.fromString("foo"),
      "b" -> Json.fromInt(5)
    ))

    val out = Convert.textToJson(inputText, "foo")

    assert(out == expectedOut)
  }

  test("invalid yaml throws an exception") {
    val inputText = """
      |a: foo
      | b: 5
      """.stripMargin

    assertThrows[ConfigYamlException] {
      Convert.textToJson(inputText, "foo")
    }
  }

  test("valid class json converts to class") {
    val inputText = """
      |a: foo
      |b: 5
      """.stripMargin

    val yaml = Convert.textToJson(inputText, "foo")
    val foo = Convert.jsonToClass[Foo](yaml, "foo")
  }

  test("invalid class json throws an exception")
  {
    val inputText = """
      |a: foo
      |c: 5
      """.stripMargin

    val yaml = Convert.textToJson(inputText, "foo")
    assertThrows[ConfigParserException] {
      Convert.jsonToClass[Foo](yaml, "foo")
    }
  }


}