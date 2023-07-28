package io.viash.functionality.arguments

import org.scalatest.funsuite.AnyFunSuite
import io.circe._

import io.viash.helpers.Logger
import io.viash.functionality.arguments.DoubleArgument
import io.viash.helpers.Yaml
import io.viash.helpers.circe.Convert

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

  test("Regular .nan values are parsed correctly") {
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

  test("Regular +.inf values are parsed correctly") {
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
  
  test("Regular -.inf values are parsed correctly") {
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

}