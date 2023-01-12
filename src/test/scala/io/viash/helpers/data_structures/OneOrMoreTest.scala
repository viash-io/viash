package io.viash.helpers.data_structures

import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite

class OneOrMoreTest extends AnyFunSuite with BeforeAndAfterAll {
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
}