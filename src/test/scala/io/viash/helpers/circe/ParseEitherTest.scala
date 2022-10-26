package io.viash.helpers.circe

import org.scalatest.{BeforeAndAfterAll, FunSuite}
import io.circe._
import io.circe.yaml.parser
import io.circe.generic.extras.semiauto.{deriveConfiguredDecoder, deriveConfiguredEncoder}

class ParseEitherTest extends FunSuite with BeforeAndAfterAll {
  case class A(foo: String)
  case class B(bar: Int)
  case class XXX(ab: Either[A, B])
  implicit val encodeA: Encoder.AsObject[A] = deriveConfiguredEncoder
  implicit val decodeA: Decoder[A] = deriveConfiguredDecoder
  implicit val encodeB: Encoder.AsObject[B] = deriveConfiguredEncoder
  implicit val decodeB: Decoder[B] = deriveConfiguredDecoder
  implicit val encodeXXX: Encoder.AsObject[XXX] = deriveConfiguredEncoder
  implicit val decodeXXX: Decoder[XXX] = deriveConfiguredDecoder
  
  test("parsing either works with left element") {
    val json = parser.parse("ab:\n  foo: str").getOrElse(Json.Null)
    val parsed = json.as[XXX].right.get
    assert(parsed == XXX(ab = Left(A("str"))))
  }

  test("parsing either works with right element") {
    val json = parser.parse("ab:\n  bar: 10").getOrElse(Json.Null)
    val parsed = json.as[XXX].right.get
    assert(parsed == XXX(ab = Right(B(10))))
  }
}