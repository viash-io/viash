package io.viash.e2e.ns_build

import io.viash._

import io.viash.config.Config
import io.viash.helpers.{Exec, IO, Logger}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite

import java.io.File
import java.nio.file.Paths
import scala.io.Source
import java.io.ByteArrayOutputStream
import io.viash.helpers.data_structures._

class MainNSBuildNativeSuite extends AnyFunSuite with BeforeAndAfterAll{
  Logger.UseColorOverride.value = Some(false)
  // path to namespace components
  private val nsPath = getClass.getResource("/testns/").getPath

  private val temporaryFolder = IO.makeTemp("viash_ns_build")
  private val tempFolStr = temporaryFolder.toString
  private val nsFolder = Paths.get(tempFolStr, "executable/testns/").toFile

  def componentExecutableFile(componentName: String): File = {
    Paths.get(nsFolder.toString, s"$componentName/$componentName").toFile
  }

  private val components = List(
    ("ns_add",      1, 2, 3),
    ("ns_subtract", 7, 2, 5),
    ("ns_multiply", 4, 3, 12),
    ("ns_divide",   10, 2, 5),
    ("ns_power",    3, 4, 81)
  )

  // convert testbash
  test("viash ns can build") {
    val testOutput = TestHelper.testMain(
    "ns", "build",
      "-s", nsPath,
      "-t", tempFolStr
    )

    assert(nsFolder.exists)
    assert(nsFolder.isDirectory)
    assert(testOutput.exitCode == Some(1))

    for ((component, _, _, _) <- components) {
      val executable = componentExecutableFile(component)
      assert(executable.exists)
      assert(executable.canExecute)
    }

    val regexBuildError = raw"Reading file \'.*/src/ns_error/config\.vsh\.yaml\' failed".r
    assert(regexBuildError.findFirstIn(testOutput.stderr).isDefined, "Expecting to get an error because of an invalid yaml in ns_error")
  }

  test("Check whether the executable can run") {
    for ((component, _, _, _) <- components) {
      Exec.run(
        Seq(componentExecutableFile(component).toString, "--help")
      )
    }
  }

  for ((component, _, _, _) <- components) {
  test(s"Check whether particular keywords can be found in the usage with component $component") {
      val configFile = getClass.getResource(s"/testns/src/$component/config.vsh.yaml").getPath
      val errStream = new ByteArrayOutputStream()
      val config = Console.withErr(errStream) {
        Config.read(configFile)
      }
      val errString = errStream.toString
      assert(errString.isEmpty() || errString.matches("Warning: The status of the component 'ns_power' is set to deprecated.\\s*"))

      val stdout =
        Exec.run(
          Seq(componentExecutableFile(component).toString, "--help")
        )

      val stripAll = (s: String) => s.replaceAll(raw"\s+", " ").trim

      config.allArguments.foreach(arg => {
        for (opt <- arg.alternatives; value <- opt)
          assert(stdout.contains(value))
        for (description <- arg.description) {
          assert(stripAll(stdout).contains(stripAll(description)))
        }
      })
    }
  }

  test("Check whether output is correctly created") {
    for ((component, input1, input2, expectedOutput) <- components) {
      val output = Paths.get(tempFolStr, s"output_$component.txt").toFile

      Exec.run(
        Seq(
          componentExecutableFile(component).toString,
          "--input1", input1.toString,
          "--input2", input2.toString,
          "--output", output.toString,
        )
      )

      assert(output.exists())

      val outputSrc = Source.fromFile(output)
      try {
        val outputLines = outputSrc.mkString
        assert(outputLines.contains(s"input1: $input1 input2: $input2 result: $expectedOutput"))
      } finally {
        outputSrc.close()
      }
    }
  }

  test("Check uniqueness of component names, same name, different namespace") {
    val compStr =
      """functionality:
        |  name: comp
        |  namespace: %s
        |""".stripMargin
    val tempSrcDir = IO.makeTemp("viash_ns_build_check_uniqueness_src")
    IO.write(compStr.format("ns1"), tempSrcDir.resolve("config1.vsh.yaml"))
    IO.write(compStr.format("ns2"), tempSrcDir.resolve("config2.vsh.yaml"))

    val tempTargetDir = IO.makeTemp("viash_ns_build_check_uniqueness_target")

    val testOutput = TestHelper.testMain(
      "ns", "build",
        "-s", tempSrcDir.toString(),
        "-t", tempTargetDir.toString()
      )

    assert(testOutput.exitCode == Some(0))
    assert(testOutput.stderr.contains("All 2 configs built successfully"))
  }

  test("Check uniqueness of component names, different name, same namespace") {
    val compStr =
      """functionality:
        |  name: %s
        |  namespace: ns
        |""".stripMargin
    val tempSrcDir = IO.makeTemp("viash_ns_build_check_uniqueness_src")
    IO.write(compStr.format("comp1"), tempSrcDir.resolve("config1.vsh.yaml"))
    IO.write(compStr.format("comp2"), tempSrcDir.resolve("config2.vsh.yaml"))

    val tempTargetDir = IO.makeTemp("viash_ns_build_check_uniqueness_target")

    val testOutput = TestHelper.testMain(
      "ns", "build",
        "-s", tempSrcDir.toString(),
        "-t", tempTargetDir.toString()
      )

    assert(testOutput.exitCode == Some(0))
    assert(testOutput.stderr.contains("All 2 configs built successfully"))
  }

  test("Check uniqueness of component names, same name, same namespace") {
    val compStr =
      """functionality:
        |  name: %s
        |  namespace: ns
        |""".stripMargin
    val tempSrcDir = IO.makeTemp("viash_ns_build_check_uniqueness_src")
    IO.write(compStr.format("comp"), tempSrcDir.resolve("config1.vsh.yaml"))
    IO.write(compStr.format("comp"), tempSrcDir.resolve("config2.vsh.yaml"))

    val tempTargetDir = IO.makeTemp("viash_ns_build_check_uniqueness_target")

    val testOutput = TestHelper.testMainException[RuntimeException](
      "ns", "build",
        "-s", tempSrcDir.toString(),
        "-t", tempTargetDir.toString()
      )

    assert(!testOutput.stderr.contains("All 2 configs built successfully"))
    assert(testOutput.exceptionText.contains("Duplicate component name found: ns/comp"))
  }

  test("Check uniqueness of component names, same name, same namespace - multiple duplicates") {
    val compStr =
      """functionality:
        |  name: %s
        |  namespace: ns
        |""".stripMargin
    val tempSrcDir = IO.makeTemp("viash_ns_build_check_uniqueness_src")
    IO.write(compStr.format("comp1"), tempSrcDir.resolve("config1.vsh.yaml"))
    IO.write(compStr.format("comp1"), tempSrcDir.resolve("config2.vsh.yaml"))
    IO.write(compStr.format("comp2"), tempSrcDir.resolve("config3.vsh.yaml"))
    IO.write(compStr.format("comp2"), tempSrcDir.resolve("config4.vsh.yaml"))

    val tempTargetDir = IO.makeTemp("viash_ns_build_check_uniqueness_target")

    val testOutput = TestHelper.testMainException[RuntimeException](
      "ns", "build",
        "-s", tempSrcDir.toString(),
        "-t", tempTargetDir.toString()
      )

    assert(!testOutput.stderr.contains("All 2 configs built successfully"))
    assert(testOutput.exceptionText.contains("Duplicate component names found: ns/comp1, ns/comp2"))
  }

  override def afterAll(): Unit = {
    IO.deleteRecursively(temporaryFolder)
  }
}
