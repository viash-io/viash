package io.viash.functionality.arguments

import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import java.nio.file.{Files, Paths, StandardCopyOption}
import scala.util.Try
import io.circe._
import io.circe.syntax._
import io.circe.yaml.{parser => YamlParser}
import io.viash.helpers.circe._
import io.viash.helpers.data_structures._
import io.viash.helpers.Logger

class StringArgumentTest extends AnyFunSuite with BeforeAndAfterAll {
  Logger.UseColorOverride.value = Some(false)
  val infoJson = Yaml("""
    |foo:
    |  bar:
    |    baz:
    |      10
    |arg: aaa
    |""".stripMargin)

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
    assert(arg.info == Json.Null)
    assert(arg.example == OneOrMore())
    assert(arg.default == OneOrMore())
    assert(!arg.required)
    assert(arg.choices == Nil)
    assert(arg.direction == Input)
    assert(!arg.multiple)
    assert(arg.multiple_sep == ";")
    assert(arg.dest == "par")

    val argParsed = arg.asJson.as[StringArgument].fold(throw _, a => a)
    assert(argParsed == arg)
  }

  test("Simple getters and helper functions on object with many non-default values") {
    val arg = StringArgument(
      name = "one_two_three_four",
      alternatives = List("zero", "-one", "--two"),
      description = Some("foo"),
      info = infoJson,
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
    assert(arg.info == infoJson)
    assert(arg.example == OneOrMore("ten"))
    assert(arg.default == OneOrMore("bar"))
    assert(arg.required)
    assert(arg.choices == List("bar", "zing", "bang"))
    assert(arg.direction == Output)
    assert(arg.multiple)
    assert(arg.multiple_sep == "-")
    assert(arg.dest == "meta")

    val argParsed = arg.asJson.as[StringArgument].fold(throw _, a => a)
    // override dest parameter as that is internal functionality and is not serialized
    assert(argParsed == arg.copyArg(dest = "par"))
  }

  test("copyArg helper function") {
    val arg = StringArgument(name = "--foo")
    
    val arg2generic = arg.copyArg(
      name = "one_two_three_four",
      alternatives = List("zero", "-one", "--two"),
      description = Some("foo"),
      info = infoJson,
      example = OneOrMore("ten"),
      default = OneOrMore("bar"),
      required = true,
      direction = Output,
      multiple = true,
      multiple_sep = "-",
      dest = "meta"
    )

    val arg2GParsed = arg2generic.asJson.as[Argument[_]].fold(throw _, a => a)
    // override dest parameter as that is internal functionality and is not serialized
    assert(arg2GParsed == arg2generic.copyArg(dest = "par"))

    assert(arg2generic.isInstanceOf[StringArgument])
    val arg2 = arg2generic.asInstanceOf[StringArgument]

    assert(arg2.name == "one_two_three_four")
    assert(arg2.alternatives == OneOrMore("zero", "-one", "--two"))
    assert(arg2.description == Some("foo"))
    assert(arg2.info == infoJson)
    assert(arg2.example == OneOrMore("ten"))
    assert(arg2.default == OneOrMore("bar"))
    assert(arg2.required)
    assert(arg2.choices == Nil)
    assert(arg2.direction == Output)
    assert(arg2.multiple)
    assert(arg2.multiple_sep == "-")
    assert(arg2.dest == "meta")

    val arg2Parsed = arg2.asJson.as[StringArgument].fold(throw _, a => a)
    // override dest parameter as that is internal functionality and is not serialized
    assert(arg2Parsed == arg2.copyArg(dest = "par"))
  }
}
