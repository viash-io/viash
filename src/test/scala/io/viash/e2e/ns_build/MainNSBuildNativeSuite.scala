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

  test("Check whether the .build.yaml contains the package config") {
    val tempTarget = IO.makeTemp("viash_ns_build_build_yaml")
    TestHelper.testMain(
      "ns", "build",
      "-s", nsPath,
      "-t", tempTarget.toString,
      "-c", ".version := '1.2.3'"
    )

    val buildYaml = tempTarget.resolve(".build.yaml")
    assert(buildYaml.toFile.exists)

    val json = io.viash.helpers.circe.Convert.textToJson(IO.read(buildYaml.toUri), buildYaml.toString)
    val packageConfig = json.hcursor.downField("package_config")
    // no _viash.yaml, so absolute paths are anonymized
    assert(packageConfig.downField("source").as[String] == Right("[anonymized]/testns"))
    assert(packageConfig.downField("target").as[String] == Right(s"[anonymized]/${tempTarget.getFileName}"))
    assert(packageConfig.downField("config_mods").as[List[String]] == Right(List(".version := '1.2.3'")))

    IO.deleteRecursively(tempTarget)
  }

  test("Check whether the .build.yaml contains paths relative to the _viash.yaml") {
    val tempPackage = IO.makeTemp("viash_ns_build_build_yaml_relative")
    IO.copyFolder(nsPath, tempPackage.toString)
    val buildTarget = tempPackage.resolve("build_target")

    TestHelper.testMain(
      Some(tempPackage),
      "ns", "build",
      "-s", tempPackage.resolve("src").toString,
      "-t", buildTarget.toString,
      "-c", ".version := '1.2.3'"
    )

    val buildYaml = buildTarget.resolve(".build.yaml")
    assert(buildYaml.toFile.exists)

    val json = io.viash.helpers.circe.Convert.textToJson(IO.read(buildYaml.toUri), buildYaml.toString)
    val packageConfig = json.hcursor.downField("package_config")
    // absolute paths inside the package are made relative to the directory of the _viash.yaml
    assert(packageConfig.downField("source").as[String] == Right("src"))
    assert(packageConfig.downField("target").as[String] == Right("build_target"))
    // config mods from the _viash.yaml come first, followed by the ones passed on the command line
    val configMods = packageConfig.downField("config_mods").as[List[String]]
    assert(configMods.map(_.length) == Right(2))
    assert(configMods.map(_.head.contains(".info := ")) == Right(true))
    assert(configMods.map(_.last) == Right(".version := '1.2.3'"))

    IO.deleteRecursively(tempPackage)
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
      """name: comp
        |namespace: %s
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

    IO.deleteRecursively(tempSrcDir)
    IO.deleteRecursively(tempTargetDir)
  }

  test("Check uniqueness of component names, different name, same namespace") {
    val compStr =
      """name: %s
        |namespace: ns
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

    IO.deleteRecursively(tempSrcDir)
    IO.deleteRecursively(tempTargetDir)
  }

  test("Check uniqueness of component names, same name, same namespace") {
    val compStr =
      """name: %s
        |namespace: ns
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

    IO.deleteRecursively(tempSrcDir)
    IO.deleteRecursively(tempTargetDir)
  }

  test("Check uniqueness of component names, same name, same namespace - multiple duplicates") {
    val compStr =
      """name: %s
        |namespace: ns
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

    IO.deleteRecursively(tempSrcDir)
    IO.deleteRecursively(tempTargetDir)
  }

  override def afterAll(): Unit = {
    IO.deleteRecursively(temporaryFolder)
  }
}
