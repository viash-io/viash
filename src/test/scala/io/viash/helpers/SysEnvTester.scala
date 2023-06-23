package io.viash.helpers

import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite

class SysEnvTest extends AnyFunSuite with BeforeAndAfterAll {

  test("Verify VIASH_VERSION is unset") {
    assert(SysEnv.viashVersion.isEmpty)
  }

  test("Can override variables") {
    SysEnv.set("VIASH_VERSION", "foo")
    assert(SysEnv.viashVersion == Some("foo"))
  }

  test("Can erase variables") {
    SysEnv.remove("VIASH_VERSION")
    assert(SysEnv.viashVersion.isEmpty)
  }
}