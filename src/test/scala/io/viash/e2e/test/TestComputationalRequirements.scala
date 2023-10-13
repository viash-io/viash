package io.viash.e2e.test

import io.viash.{ConfigDeriver, TestHelper}
import io.viash.helpers.{IO, Logger}
import java.nio.file.Paths
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite

class TestComputationalRequirements extends AnyFunSuite with BeforeAndAfterAll {
  Logger.UseColorOverride.value = Some(false)
  private val configFile = getClass.getResource("/testbash/check_computational_requirements.vsh.yaml").getPath
  private val temporaryFolder = IO.makeTemp(s"viash_${this.getClass.getName}_")
  private val tempFolStr = temporaryFolder.toString

  private val configDeriver = ConfigDeriver(Paths.get(configFile), temporaryFolder)
  
  test("Check without computational requirements") {
    val testOutput = TestHelper.testMain(
      "test",
      configFile
    )

    assert(testOutput.stdout.contains("cpus unset"))
    assert(testOutput.stdout.contains("memory unset"))
  }

  test("Check set cpus in CLI") {
    val testOutput = TestHelper.testMain(
      "test",
      "--cpus", "2",
      configFile
    )

    assert(testOutput.stdout.contains("cpus: 2"))
    assert(testOutput.stdout.contains("memory unset"))
  }

  test("Check set memory in CLI") {
    val testOutput = TestHelper.testMain(
      "test",
      "--memory", "2mb",
      configFile
    )

    assert(testOutput.stdout.contains("cpus unset"))
    assert(testOutput.stdout.contains("memory: 2"))
  }

  test("Check set cpus in config") {
    val newConfigFilePath = configDeriver.derive(""".functionality.requirements := {cpus: 3}""", "cpus_set")
    val testOutput = TestHelper.testMain(
      "test",
      newConfigFilePath
    )

    assert(testOutput.stdout.contains("cpus: 3"))
    assert(testOutput.stdout.contains("memory unset"))
  }

  test("Check set memory in config") {
    val newConfigFilePath = configDeriver.derive(""".functionality.requirements := {memory: "3 mb"}""", "memory_set")
    val testOutput = TestHelper.testMain(
      "test",
      newConfigFilePath
    )

    assert(testOutput.stdout.contains("cpus unset"))
    assert(testOutput.stdout.contains("memory: 3"))
  }

  test("Check set cpus and memory in config") {
    val newConfigFilePath = configDeriver.derive(""".functionality.requirements := {cpus: 3, memory: "3 mb"}""", "cpus_memory_set")
    val testOutput = TestHelper.testMain(
      "test",
      newConfigFilePath
    )

    assert(testOutput.stdout.contains("cpus: 3"))
    assert(testOutput.stdout.contains("memory: 3"))
  }

  test("Check set cpus in config and CLI") {
    val newConfigFilePath = configDeriver.derive(""".functionality.requirements := {cpus: 3}""", "cpus_set2")
    val testOutput = TestHelper.testMain(
      "test",
      "--cpus", "2",
      newConfigFilePath
    )

    assert(testOutput.stdout.contains("cpus: 2"))
    assert(testOutput.stdout.contains("memory unset"))
  }

  test("Check set memory in config and CLI") {
    val newConfigFilePath = configDeriver.derive(""".functionality.requirements := {memory: "3 mb"}""", "memory_set2")
    val testOutput = TestHelper.testMain(
      "test",
      "--memory", "2mb",
      newConfigFilePath
    )

    assert(testOutput.stdout.contains("cpus unset"))
    assert(testOutput.stdout.contains("memory: 2"))
  }

  test("Check set cpus and memory in config and CLI") {
    val newConfigFilePath = configDeriver.derive(""".functionality.requirements := {cpus: 3, memory: "3 mb"}""", "cpus_memory_set2")
    val testOutput = TestHelper.testMain(
      "test",
      "--cpus", "2",
      "--memory", "2mb",
      newConfigFilePath
    )

    assert(testOutput.stdout.contains("cpus: 2"))
    assert(testOutput.stdout.contains("memory: 2"))
  }

  override def afterAll(): Unit = {
    IO.deleteRecursively(temporaryFolder)
  }

}
