package io.viash.config_mods

import io.circe.Json
import org.scalatest.funsuite.AnyFunSuite
import io.circe.syntax._
import io.viash.helpers.Logger

class DeleteTest extends AnyFunSuite {
  Logger.UseColorOverride.value = Some(false)
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