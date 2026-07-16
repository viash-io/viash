package io.viash.helpers.circe

import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import io.circe._
import io.circe.yaml.{parser => YamlParser}
import io.viash.helpers.{Yaml => YamlHelper}
import io.viash.helpers.Logger

class YamlTest extends AnyFunSuite with BeforeAndAfterAll {
  Logger.UseColorOverride.value = Some(false)
  
  test("checking whether Yaml works") {
    val out = Yaml("""
      |foo:
      |  bar:
      |    baz:
      |      10
      |arg: aaa
      """.stripMargin)
    val expectedOut = Json.fromJsonObject(JsonObject(
      "foo" -> Json.fromJsonObject(JsonObject(
        "bar" -> Json.fromJsonObject(JsonObject(
          "baz" -> Json.fromInt(10)
        ))
      )),
      "arg" -> Json.fromString("aaa")
    ))

    assert(out == expectedOut)
  }

  test(".inf values can be read by circe after \"fixing\" them") {
    // Expected behaviour is that circe still doesn't accept yaml +.inf, -.inf, or .nan number values.
    assertThrows[ParsingFailure] {
      val failure = Yaml("""val: +.inf""")
    }
    
    val out1 = Yaml("""val: "+.inf"""")
    val out2 = Yaml(YamlHelper.replaceInfinities("""val: +.inf"""))

    assert(out1 == out2)
  }

  test("replaceInfinities does not throw on an empty or comment-only yaml document") {
    // SnakeYAML's compose() returns a `null` node for these; make sure we don't try to serialize it.
    assert(YamlHelper.replaceInfinities("") == "")
    assert(YamlHelper.replaceInfinities("   \n") == "   \n")
    assert(YamlHelper.replaceInfinities("# just a comment\n") == "# just a comment\n")
  }

  test("an empty or comment-only yaml document is parsed by circe-yaml as Json.False") {
    // Documents the underlying circe-yaml quirk that Convert.textToJson and PackageConfig/Config rely on.
    assert(Yaml("") == Json.False)
    assert(Yaml("# just a comment\n") == Json.False)
  }
}