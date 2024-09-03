package io.viash.config

import org.scalatest.funsuite.AnyFunSuite
import io.viash.config.ComputationalRequirements
import io.viash.helpers.Logger

class ComputationalRequirementsTests extends AnyFunSuite {
  Logger.UseColorOverride.value = Some(false)

  test("Empty ComputationalRequirements") {
    val compReq = ComputationalRequirements()

    assert(compReq.cpus == None)
    assert(compReq.memory == None)
    assert(compReq.commands == Nil)

    assert(compReq.memoryAsBytes == None)
  }

  test("test SI units B") {
    val compReq = ComputationalRequirements(memory = Some("10B"))
    assert(compReq.memoryAsBytes == Some(BigInt("10")))
  }

  test("test SI units KB") {
    val compReq = ComputationalRequirements(memory = Some("10KB"))
    assert(compReq.memoryAsBytes == Some(BigInt("10000")))
  }

  test("test SI units MB") {
    val compReq = ComputationalRequirements(memory = Some("10MB"))
    assert(compReq.memoryAsBytes == Some(BigInt("10000000")))
  }

  test("test SI units GB") {
    val compReq = ComputationalRequirements(memory = Some("10GB"))
    assert(compReq.memoryAsBytes == Some(BigInt("10000000000")))
  }

  test("test SI units TB") {
    val compReq = ComputationalRequirements(memory = Some("10TB"))
    assert(compReq.memoryAsBytes == Some(BigInt("10000000000000")))
  }

  test("test SI units PB") {
    val compReq = ComputationalRequirements(memory = Some("10PB"))
    assert(compReq.memoryAsBytes == Some(BigInt("10000000000000000")))
  }

  test("test IEC units B") {
    val compReq = ComputationalRequirements(memory = Some("10B"))
    assert(compReq.memoryAsBytes == Some(BigInt("10")))
  }

  test("test IEC units KiB") {
    val compReq = ComputationalRequirements(memory = Some("10KiB"))
    assert(compReq.memoryAsBytes == Some(BigInt("10240")))
  }

  test("test IEC units MiB") {
    val compReq = ComputationalRequirements(memory = Some("10MiB"))
    assert(compReq.memoryAsBytes == Some(BigInt("10485760")))
  }

  test("test IEC units GiB") {
    val compReq = ComputationalRequirements(memory = Some("10GiB"))
    assert(compReq.memoryAsBytes == Some(BigInt("10737418240")))
  }

  test("test IEC units TiB") {
    val compReq = ComputationalRequirements(memory = Some("10TiB"))
    assert(compReq.memoryAsBytes == Some(BigInt("10995116277760")))
  }

  test("test IEC units PiB") {
    val compReq = ComputationalRequirements(memory = Some("10PiB"))
    assert(compReq.memoryAsBytes == Some(BigInt("11258999068426240")))
  }

  test("test SI in lowercase") {
    val compReq = ComputationalRequirements(memory = Some("10gb"))
    assert(compReq.memoryAsBytes == Some(BigInt("10000000000")))
  }

  test("test IEC in lowercase") {
    val compReq = ComputationalRequirements(memory = Some("10gib"))
    assert(compReq.memoryAsBytes == Some(BigInt("10737418240")))
  }

  test("test without B suffix") {
    val compReq = ComputationalRequirements(memory = Some("10"))
    assertThrows[RuntimeException] {
      compReq.memoryAsBytes
    }
  }

  test("test SI without B suffix") {
    val compReq = ComputationalRequirements(memory = Some("10G"))
    assert(compReq.memoryAsBytes == Some(BigInt("10000000000")))
  }

  test("test IEC without B suffix") {
    val compReq = ComputationalRequirements(memory = Some("10Gi"))
    assert(compReq.memoryAsBytes == Some(BigInt("10737418240")))
  }

  test("test invalid value") {
    val compReq = ComputationalRequirements(memory = Some("10GiBb"))
    assertThrows[RuntimeException] {
      compReq.memoryAsBytes
    }
  }

  test("test no value") {
    val compReq = ComputationalRequirements(memory = Some(""))
    assertThrows[RuntimeException] {
      compReq.memoryAsBytes
    }
  }

  test("test no value, but unit") {
    val compReq = ComputationalRequirements(memory = Some("GB"))
    assertThrows[RuntimeException] {
      compReq.memoryAsBytes
    }
  }

  test("test very long byte value") {
    val compReq = ComputationalRequirements(memory = Some("1234567890123456789012345678901234567890B"))
    assert(compReq.memoryAsBytes == Some(BigInt("1234567890123456789012345678901234567890")))
  }
  
}
