package io.viash.helpers.data_structures

import org.scalatest.{BeforeAndAfterAll, FunSuite}

class OneOrMoreTest extends FunSuite with BeforeAndAfterAll {
  test("works with one element") {
    val oom = OneOrMore("foo")
    assert(oom.toList == List("foo"))
  }

  test("works with no elements") {
    val oom = OneOrMore()
    assert(oom.toList == Nil)
  }

  test("works with more elements") {
    val oom = OneOrMore(1, 2, 3, 4)
    assert(oom.toList == List(1, 2, 3, 4))
  }

  test("implicit conversion to a list works") {
    val oom = OneOrMore("foo")

    // since .length is not a function of OneOrMore, this should not even compile
    // if implicit conversion is not working
    assert(oom.length == 1)
  }

  test("implicit conversion from a list works") {
    val li = List(1, 2, 3)
    val oom: OneOrMore[Int] = li
    assert(oom.toList == li)
  }

  // parsing related
  import io.circe._
  import io.circe.yaml.parser
  import io.viash.helpers.circe._
  import io.circe.generic.extras.semiauto.{deriveConfiguredDecoder, deriveConfiguredEncoder}
  
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