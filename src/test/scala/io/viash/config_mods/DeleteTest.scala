package io.viash.config_mods

import io.circe.Json
import org.scalatest.FunSuite
import io.circe.syntax._

class DeleteTest extends FunSuite {
  // testing parsing
  test("parsing delete command") {
    val expected = ConfigMods(List(
      Delete(
        Path(List(Attribute("x")))
      )
    ))
    val command = """del(.x)"""
    val result = ConfigModParser.block.parse(command)
    assert(result == expected)
  }

  // testing functionality
  // TODO
}