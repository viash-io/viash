package io.viash.functionality

import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import java.nio.file.{Files, Paths, StandardCopyOption}
import scala.util.Try
import io.circe._
import io.circe.yaml.{parser => YamlParser}
import io.circe.syntax._
import io.viash.helpers.circe._
import io.viash.helpers.data_structures._


class FunctionalityTest extends AnyFunSuite with BeforeAndAfterAll {
  val infoJson = Yaml("""
    |foo:
    |  bar:
    |    baz:
    |      10
    |arg: aaa
    |""".stripMargin)

  test("Simple getters and helper functions") {
    val fun = Functionality(name = "foo")

    assert(fun.name == "foo")
    assert(fun.description == None)
    assert(fun.info == Json.Null)

    val funParsed = fun.asJson.as[Functionality].fold(throw _, a => a)
    assert(funParsed == fun)
  }

  test("Simple getters and helper functions on object with many non-default values") {
    val fun = Functionality(
      name = "one_two_three_four",
      description = Some("foo"),
      info = infoJson
    )

    assert(fun.name == "one_two_three_four")
    assert(fun.description == Some("foo"))
    assert(fun.info == infoJson)
    
    val funParsed = fun.asJson.as[Functionality].fold(throw _, a => a)
    assert(funParsed == fun)
  }

  // TODO: expand functionality tests
}