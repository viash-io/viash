package io.viash.config

import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import java.nio.file.{Files, Paths, StandardCopyOption}
import scala.util.Try
import io.circe._
import io.circe.yaml.{parser => YamlParser}
import io.circe.syntax._
import io.viash.helpers.circe._
import io.viash.helpers.data_structures._
import io.viash.helpers.Logger

class ConfigTest extends AnyFunSuite with BeforeAndAfterAll {
  Logger.UseColorOverride.value = Some(false)
  val infoJson = Yaml("""
    |foo:
    |  bar:
    |    baz:
    |      10
    |arg: aaa
    |""".stripMargin)

  test("Simple getters and helper functions") {
    val conf = Config(name = "foo")

    assert(conf.name == "foo")
    assert(conf.description == None)
    assert(conf.info == Json.Null)

    val confParsed = conf.asJson.as[Config].fold(throw _, a => a)
    assert(confParsed == conf)
  }

  test("Simple getters and helper functions on object with many non-default values") {
    val conf = Config(
      name = "one_two_three_four",
      description = Some("foo"),
      info = infoJson
    )

    assert(conf.name == "one_two_three_four")
    assert(conf.description == Some("foo"))
    assert(conf.info == infoJson)
    
    val confParsed = conf.asJson.as[Config].fold(throw _, a => a)
    assert(confParsed == conf)
  }

  // TODO: expand functionality tests
}