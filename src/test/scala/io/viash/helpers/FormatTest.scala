package io.viash.helpers

import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite

class FormatTest extends AnyFunSuite with BeforeAndAfterAll {
  test("A paragraph that contains no newlines that is longer than the wrap column is wrapped at the wrap column") {
    val paragraph = "A paragraph that contains no newlines that is longer than the wrap column is wrapped at the wrap column"
    val wrapColumn = 80
    val expected = List(
      "A paragraph that contains no newlines that is longer than the wrap column is",
      "wrapped at the wrap column"
    )
    val actual = Format.paragraphWrap(paragraph, wrapColumn)
    assert(actual == expected)
  }

  test("A paragraph that contains no newlines that is shorter than the wrap column is not wrapped") {
    val paragraph = "A paragraph that contains no newlines that is shorter than the wrap column"
    val wrapColumn = 80
    val expected = List(paragraph)
    val actual = Format.paragraphWrap(paragraph, wrapColumn)
    assert(actual == expected)
  }
  
  test("A paragraph that contains a newline is split at the newline") {
    val paragraph = "A paragraph that contains a newline\nand is longer than the wrap column"
    val wrapColumn = 80
    val expected = List(
      "A paragraph that contains a newline",
      "and is longer than the wrap column"
    )
    val actual = Format.paragraphWrap(paragraph, wrapColumn)
    assert(actual == expected)
  }

  test("A paragraph that contains a newline and is longer than the wrap column wraps at the wrap column after the newline") {
    val paragraph = "A paragraph that contains a newline\nand is longer than the wrap column after the newline"
    val wrapColumn = 40
    val expected = List(
      "A paragraph that contains a newline",
      "and is longer than the wrap column after",
      "the newline"
    )
    val actual = Format.paragraphWrap(paragraph, wrapColumn)
    assert(actual == expected)
  }
  
  test("A paragraph that contains a newline and is shorter than the wrap column is not wrapped") {
    val paragraph = "A paragraph that contains a newline\nand is shorter than the wrap column"
    val wrapColumn = 80
    val expected = List(
      "A paragraph that contains a newline",
      "and is shorter than the wrap column"
    )
    val actual = Format.paragraphWrap(paragraph, wrapColumn)
    assert(actual == expected)
  }

  test("A paragraph that contains multiple newlines is split at each newline") {
    val paragraph = "A paragraph that contains multiple newlines\nand is longer than the wrap column\nafter each newline"
    val wrapColumn = 80
    val expected = List(
      "A paragraph that contains multiple newlines",
      "and is longer than the wrap column",
      "after each newline"
    )
    val actual = Format.paragraphWrap(paragraph, wrapColumn)
    assert(actual == expected)
  }

  test("A paragraph that contains multiple newlines and is longer than the wrap column wraps at the wrap column after each newline") {
    val paragraph = "A paragraph that contains multiple newlines\nand is longer than the wrap column\nafter each newline"
    val wrapColumn = 40
    val expected = List(
      "A paragraph that contains multiple",
      "newlines",
      "and is longer than the wrap column",
      "after each newline"
    )
    val actual = Format.paragraphWrap(paragraph, wrapColumn)
    assert(actual == expected)
  }
}
