package io.viash.helpers.circe

import org.scalatest.{BeforeAndAfterAll, FunSuite}
import io.circe._
import io.circe.yaml.parser
import io.viash.helpers.circe._
import io.circe.generic.extras.semiauto.{deriveConfiguredDecoder, deriveConfiguredEncoder}
import io.viash.helpers.data_structures._

class ParseOneOrMoreTest extends FunSuite with BeforeAndAfterAll {
  case class XXX(str: OneOrMore[String])
  implicit val encodeXXX: Encoder.AsObject[XXX] = deriveConfiguredEncoder
  implicit val decodeXXX: Decoder[XXX] = deriveConfiguredDecoder

  test("parsing with circe actually works with one element") {
    val json: Json = parser.parse("str: foo").getOrElse(Json.Null)
    val parsed = json.as[XXX].right.get
    assert(parsed == XXX(str = OneOrMore("foo")))
  }

  test("parsing with circe actually works with more elements") {
    val json: Json = parser.parse("str: [a, b, c]").getOrElse(Json.Null)
    val parsed = json.as[XXX].right.get
    assert(parsed == XXX(str = OneOrMore("a", "b", "c")))
  }

  test("parsing with circe actually works with empty list") {
    val json: Json = parser.parse("str: []").getOrElse(Json.Null)
    val parsed = json.as[XXX].right.get
    assert(parsed == XXX(str = OneOrMore()))
  }
}