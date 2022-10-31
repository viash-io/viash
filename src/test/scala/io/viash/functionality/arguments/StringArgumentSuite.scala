package io.viash.functionality.arguments

import org.scalatest.{BeforeAndAfterAll, FunSuite}
import java.nio.file.{Files, Paths, StandardCopyOption}
import scala.util.Try
import io.viash.helpers.data_structures._

class StringArgumentSuite extends FunSuite with BeforeAndAfterAll {

  test("Simple getters and helper functions") {
    val arg = StringArgument(name = "--foo")

    assert(arg.`type` == "string")
    assert(arg.par == "par_foo")
    assert(arg.VIASH_PAR == "VIASH_PAR_FOO")
    assert(arg.flags == "--")
    assert(arg.plainName == "foo")

    assert(arg.name == "--foo")
    assert(arg.alternatives == OneOrMore())
    assert(arg.description == None)
    assert(arg.example == OneOrMore())
    assert(arg.default == OneOrMore())
    assert(!arg.required)
    assert(arg.choices == Nil)
    assert(arg.direction == Input)
    assert(!arg.multiple)
    assert(arg.multiple_sep == ":")
    assert(arg.dest == "par")
  }

  test("Simple getters and helper functions on object with many non-default values") {
    val arg = StringArgument(
      name = "one_two_three_four",
      alternatives = List("zero", "-one", "--two"),
      description = Some("foo"),
      example = OneOrMore("ten"),
      default = OneOrMore("bar"),
      required = true,
      choices = List("bar", "zing", "bang"),
      direction = Output,
      multiple = true,
      multiple_sep = "-",
      dest = "meta"
    )

    assert(arg.`type` == "string")
    assert(arg.par == "meta_one_two_three_four")
    assert(arg.VIASH_PAR == "VIASH_META_ONE_TWO_THREE_FOUR")
    assert(arg.flags == "")
    assert(arg.plainName == "one_two_three_four")
    
    assert(arg.name == "one_two_three_four")
    assert(arg.alternatives == OneOrMore("zero", "-one", "--two"))
    assert(arg.description == Some("foo"))
    assert(arg.example == OneOrMore("ten"))
    assert(arg.default == OneOrMore("bar"))
    assert(arg.required)
    assert(arg.choices == List("bar", "zing", "bang"))
    assert(arg.direction == Output)
    assert(arg.multiple)
    assert(arg.multiple_sep == "-")
    assert(arg.dest == "meta")
  }

  test("copyArg helper function") {
    val arg = StringArgument(name = "--foo")
    
    val arg2generic = arg.copyArg(
      name = "one_two_three_four",
      alternatives = List("zero", "-one", "--two"),
      description = Some("foo"),
      example = OneOrMore("ten"),
      default = OneOrMore("bar"),
      required = true,
      direction = Output,
      multiple = true,
      multiple_sep = "-",
      dest = "meta"
    )

    assert(arg2generic.isInstanceOf[StringArgument])
    val arg2 = arg2generic.asInstanceOf[StringArgument]

    assert(arg2.name == "one_two_three_four")
    assert(arg2.alternatives == OneOrMore("zero", "-one", "--two"))
    assert(arg2.description == Some("foo"))
    assert(arg2.example == OneOrMore("ten"))
    assert(arg2.default == OneOrMore("bar"))
    assert(arg2.required)
    assert(arg2.choices == Nil)
    assert(arg2.direction == Output)
    assert(arg2.multiple)
    assert(arg2.multiple_sep == "-")
    assert(arg2.dest == "meta")
  }
}