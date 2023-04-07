package io.viash.helpers.circe

import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import io.circe._
import io.circe.yaml.parser
import io.viash.helpers.circe._
import io.circe.generic.extras.semiauto.{deriveConfiguredDecoder, deriveConfiguredEncoder}
import io.viash.helpers.data_structures._

class ParseOneOrMoreTest extends AnyFunSuite with BeforeAndAfterAll {
  case class XXX(str: OneOrMore[String])
  implicit val encodeXXX: Encoder.AsObject[XXX] = deriveConfiguredEncoder
  implicit val decodeXXX: Decoder[XXX] = deriveConfiguredDecoder

  test("parsing with circe actually works with one element") {
    val json = parser.parse("str: foo").getOrElse(Json.Null)
    val parsed = json.as[XXX].toOption.get
    assert(parsed == XXX(str = OneOrMore("foo")))
  }

  test("parsing with circe actually works with more elements") {
    val json = parser.parse("str: [a, b, c]").getOrElse(Json.Null)
    val parsed = json.as[XXX].toOption.get
    assert(parsed == XXX(str = OneOrMore("a", "b", "c")))
  }

  test("parsing with circe actually works with empty list") {
    val json = parser.parse("str: []").getOrElse(Json.Null)
    val parsed = json.as[XXX].toOption.get
    assert(parsed == XXX(str = OneOrMore()))
  }
}