package io.viash.functionality.dependencies

import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite

import io.viash.config.Config
import io.viash.helpers.{IO, Exec}

import java.nio.file.{Files, Paths}
import io.viash.{ConfigDeriver, TestHelper}

class DependencyTest extends AnyFunSuite with BeforeAndAfterAll {

  private val configFile = getClass.getResource("/testbash/config.vsh.yaml").getPath

  private val temporaryFolder = IO.makeTemp(s"viash_${this.getClass.getName}_")
  private val temporaryConfigFolder = IO.makeTemp(s"viash_${this.getClass.getName}_configs_")
  private val tempFolStr = temporaryFolder.toString
  private val configDeriver = ConfigDeriver(Paths.get(configFile), temporaryConfigFolder)

  test("Add a local dependency") {
    val dependencyConfig = configDeriver.derive(List(
      """.functionality.name := "someDependency"""",
      """.functionality.namespace := "dependencyTest""""),
      "dependencyTest")
    val newConfigFilePath = configDeriver.derive(List(
      """.functionality.namespace := "dependencyTest"""",
      """.functionality.dependencies := [{ "name": "dependencyTest/someDependency" }]"""),
      "confWithDep")

    val out = TestHelper.testMainWithStdErr(
      "ns", "build",
      "-n", "dependencyTest",
      "--engine", "native",
      "--runner", "native",
      "--src", temporaryConfigFolder.toString(),
      "--target", temporaryFolder.resolve("target").toString()
    )

    val cleanOut = TestHelper.cleanConsoleControls(out._2)
    // Output is supposed to be have control characters removed but somehow matching for `(?s)\\s*2/\\d+` or `(?s)\\D*2/\\d+` still doesn't work
    assert(cleanOut.matches("(?s).*2/\\d+ configs built successfully.*"), "check build was successful")

    val outputPath = temporaryFolder.resolve("target/native/dependencyTest/testbash/testbash")
    val outputText = IO.read(outputPath.toUri())
    assert(outputText.contains("VIASH_DEP_DEPENDENCYTEST_SOMEDEPENDENCY="), "check the dependency is set in the output script")
  }

  override def afterAll(): Unit = {
    IO.deleteRecursively(temporaryFolder)
    IO.deleteRecursively(temporaryConfigFolder)
  }
}
