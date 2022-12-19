package io.viash.e2e.ns_build

import io.viash._

import io.viash.config.Config
import io.viash.helpers.{Exec, IO}
import org.scalatest.{BeforeAndAfterAll, FunSuite}

import java.io.File
import java.nio.file.Paths
import scala.io.Source

class MainNSBuildNativeSuite extends FunSuite with BeforeAndAfterAll{
  // path to namespace components
  private val nsPath = getClass.getResource("/testns/").getPath

  private val temporaryFolder = IO.makeTemp("viash_ns_build")
  private val tempFolStr = temporaryFolder.toString
  private val nsFolder = Paths.get(tempFolStr, "native/testns/").toFile

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
    val (stdout, stderr, exitCode) = TestHelper.testMainWithStdErr(
    "ns", "build",
      "-s", nsPath,
      "-t", tempFolStr
    )

    assert(nsFolder.exists)
    assert(nsFolder.isDirectory)
    assert(exitCode == 1)

    for ((component, _, _, _) <- components) {
      val executable = componentExecutableFile(component)
      assert(executable.exists)
      assert(executable.canExecute)
    }

    val regexBuildError = raw"Reading file \'.*/src/ns_error/config\.vsh\.yaml\' failed".r
    assert(regexBuildError.findFirstIn(stderr).isDefined, "Expecting to get an error because of an invalid yaml in ns_error")
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
      val functionality = Config.read(configFile).functionality

      val stdout =
        Exec.run(
          Seq(componentExecutableFile(component).toString, "--help")
        )

      val stripAll = (s: String) => s.replaceAll(raw"\s+", " ").trim

      functionality.allArguments.foreach(arg => {
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

  override def afterAll() {
    IO.deleteRecursively(temporaryFolder)
  }
}
