package io.viash.helpers.circe

import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import io.circe._
import io.circe.yaml.parser
import io.circe.generic.extras.semiauto.{deriveConfiguredDecoder, deriveConfiguredEncoder}

class ParseStringLikeTest extends AnyFunSuite with BeforeAndAfterAll {
  case class XXX(str: String)
  implicit val encodeXXX: Encoder.AsObject[XXX] = deriveConfiguredEncoder
  implicit val decodeXXX: Decoder[XXX] = deriveConfiguredDecoder

  test("parsing stringlike works with string") {
    val json = parser.parse("str: foo").getOrElse(Json.Null)
    val parsed = json.as[XXX].toOption.get
    assert(parsed == XXX("foo"))
  }

  test("parsing stringlike works with int") {
    val json = parser.parse("str: 10").getOrElse(Json.Null)
    val parsed = json.as[XXX].toOption.get
    assert(parsed == XXX("10"))
  }

  test("parsing stringlike works with double") {
    val json = parser.parse("str: 10.5").getOrElse(Json.Null)
    val parsed = json.as[XXX].toOption.get
    assert(parsed == XXX("10.5"))
  }

  test("parsing stringlike works with boolean") {
    val json = parser.parse("str: true").getOrElse(Json.Null)
    val parsed = json.as[XXX].toOption.get
    assert(parsed == XXX("true"))
  }
}