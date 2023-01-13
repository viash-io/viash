package io.viash.e2e.run

import io.viash.{ConfigDeriver, TestHelper}
import io.viash.helpers.IO
import java.nio.file.Paths
import org.scalatest.{BeforeAndAfterAll, FunSuite}


class RunComputationalRequirements extends FunSuite with BeforeAndAfterAll {

  private val configFile = getClass.getResource("/testbash/check_computational_requirements.vsh.yaml").getPath
  private val temporaryFolder = IO.makeTemp(s"viash_${this.getClass.getName}_")
  private val tempFolStr = temporaryFolder.toString

  private val configDeriver = ConfigDeriver(Paths.get(configFile), temporaryFolder)
  
  test("Check without computational requirements") {
    val output = TestHelper.testMain(
      "run",
      configFile
    )

    assert(output.contains("cpus unset"))
    assert(output.contains("memory unset"))
  }

  test("Check set cpus in CLI") {
    val output = TestHelper.testMain(
      "run",
      "--cpus", "2",
      configFile
    )

    assert(output.contains("cpus: 2"))
    assert(output.contains("memory unset"))
  }

  test("Check set memory in CLI") {
    val output = TestHelper.testMain(
      "run",
      "--memory", "2mb",
      configFile
    )

    assert(output.contains("cpus unset"))
    assert(output.contains("memory: 2"))
  }

  test("Check set cpus in config") {
    val newConfigFilePath = configDeriver.derive(""".functionality.requirements := {cpus: 3}""", "cpus_set")
    val output = TestHelper.testMain(
      "run",
      newConfigFilePath
    )

    assert(output.contains("cpus: 3"))
    assert(output.contains("memory unset"))
  }

  test("Check set memory in config") {
    val newConfigFilePath = configDeriver.derive(""".functionality.requirements := {memory: "3 mb"}""", "memory_set")
    val output = TestHelper.testMain(
      "run",
      newConfigFilePath
    )

    assert(output.contains("cpus unset"))
    assert(output.contains("memory: 3"))
  }

  test("Check set cpus and memory in config") {
    val newConfigFilePath = configDeriver.derive(""".functionality.requirements := {cpus: 3, memory: "3 mb"}""", "cpus_memory_set")
    val output = TestHelper.testMain(
      "run",
      newConfigFilePath
    )

    assert(output.contains("cpus: 3"))
    assert(output.contains("memory: 3"))
  }

  test("Check set cpus in config and CLI") {
    val newConfigFilePath = configDeriver.derive(""".functionality.requirements := {cpus: 3}""", "cpus_set2")
    val output = TestHelper.testMain(
      "run",
      "--cpus", "2",
      newConfigFilePath
    )

    assert(output.contains("cpus: 2"))
    assert(output.contains("memory unset"))
  }

  test("Check set memory in config and CLI") {
    val newConfigFilePath = configDeriver.derive(""".functionality.requirements := {memory: "3 mb"}""", "memory_set2")
    val output = TestHelper.testMain(
      "run",
      "--memory", "2mb",
      newConfigFilePath
    )

    assert(output.contains("cpus unset"))
    assert(output.contains("memory: 2"))
  }

  test("Check set cpus and memory in config and CLI") {
    val newConfigFilePath = configDeriver.derive(""".functionality.requirements := {cpus: 3, memory: "3 mb"}""", "cpus_memory_set2")
    val output = TestHelper.testMain(
      "run",
      "--cpus", "2",
      "--memory", "2mb",
      newConfigFilePath
    )

    assert(output.contains("cpus: 2"))
    assert(output.contains("memory: 2"))
  }

  override def afterAll() {
    IO.deleteRecursively(temporaryFolder)
  }

}
