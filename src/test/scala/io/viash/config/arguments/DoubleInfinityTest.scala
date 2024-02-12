package io.viash.config.arguments

import org.scalatest.funsuite.AnyFunSuite
import io.circe._

import io.viash.helpers.Logger
import io.viash.config.arguments.DoubleArgument
import io.viash.helpers.Yaml
import io.viash.helpers.circe.Convert
import io.viash.config.arguments.encodeArgument
import io.circe.yaml.Printer

class DoubleInfinityTest extends AnyFunSuite {
  Logger.UseColorOverride.value = Some(false)

  test("Regular double values are parsed correctly") {
    val inputText = 
      """
        |name: "--double"
        |min: 5
        |""".stripMargin
    val yaml = Yaml.replaceInfinities(inputText)
    val yaml2 = Convert.textToJson(inputText, "foo")
    val arg = Convert.jsonToClass[DoubleArgument](yaml2, "foo")

    assert(arg.min == Some(5))
  }

  test(".nan values are parsed correctly") {
    val inputText = 
      """
        |name: "--double"
        |min: ".nan"
        |""".stripMargin
    val yaml = Yaml.replaceInfinities(inputText)
    val yaml2 = Convert.textToJson(yaml, "foo")
    val arg = Convert.jsonToClass[DoubleArgument](yaml2, "foo")

    assert(arg.min.isDefined)
    assert(arg.min.get.isNaN)
  }

  test("+.inf values are parsed correctly") {
    val inputText = 
      """
        |name: "--double"
        |min: +.inf
        |""".stripMargin
    val yaml = Yaml.replaceInfinities(inputText)
    val yaml2 = Convert.textToJson(yaml, "foo")
    val arg = Convert.jsonToClass[DoubleArgument](yaml2, "foo")

    assert(arg.min.isDefined)
    assert(arg.min.get.isPosInfinity)
  }
  
  test("-.inf values are parsed correctly") {
    val inputText = 
      """
        |name: "--double"
        |min: -.inf
        |""".stripMargin
    val yaml = Yaml.replaceInfinities(inputText)
    val yaml2 = Convert.textToJson(yaml, "foo")
    val arg = Convert.jsonToClass[DoubleArgument](yaml2, "foo")

    assert(arg.min.isDefined)
    assert(arg.min.get.isNegInfinity)
  }

  test("Regular double values can be serialized and parsed back") {
    val inputArg = DoubleArgument(
      name = "--double",
      min = Some(5)
    )
    val json = encodeArgument(inputArg)
    val yaml = Printer.spaces2.pretty(json)

    assert(yaml.contains("min: 5.0"))

    val yaml2 = Convert.textToJson(yaml, "foo")
    val outputArg = Convert.jsonToClass[DoubleArgument](yaml2, "foo")

    assert(outputArg.min == Some(5))
  }

  test(".nan values can be serialized and parsed back") {
    val inputArg = DoubleArgument(
      name = "--double",
      min = Some(Double.NaN)
    )
    val json = encodeArgument(inputArg)
    val yaml = Printer.spaces2.pretty(json)

    assert(yaml.contains("min: NaN"))

    val yaml2 = Convert.textToJson(yaml, "foo")
    val outputArg = Convert.jsonToClass[DoubleArgument](yaml2, "foo")

    assert(outputArg.min.isDefined)
    assert(outputArg.min.get.isNaN)
  }

  test("+.inf values can be serialized and parsed back") {
    val inputArg = DoubleArgument(
      name = "--double",
      min = Some(Double.PositiveInfinity)
    )
    val json = encodeArgument(inputArg)
    val yaml = Printer.spaces2.pretty(json)

    assert(yaml.contains("min: +Infinity"))

    val yaml2 = Convert.textToJson(yaml, "foo")
    val outputArg = Convert.jsonToClass[DoubleArgument](yaml2, "foo")

    assert(outputArg.min.isDefined)
    assert(outputArg.min.get.isPosInfinity)
  }

  test("-.inf values can be serialized and parsed back") {
    val inputArg = DoubleArgument(
      name = "--double",
      min = Some(Double.NegativeInfinity)
    )
    val json = encodeArgument(inputArg)
    val yaml = Printer.spaces2.pretty(json)

    assert(yaml.contains("min: -Infinity"))

    val yaml2 = Convert.textToJson(yaml, "foo")
    val outputArg = Convert.jsonToClass[DoubleArgument](yaml2, "foo")

    assert(outputArg.min.isDefined)
    assert(outputArg.min.get.isNegInfinity)
  }

}