package com.dataintuitive.viash

import org.scalatest.FunSuite
import java.nio.file.{Path, Paths, Files}
import java.io.File
import sys.process.Process
import com.dataintuitive.viash.functionality.Functionality
import scala.io.Source
import scala.reflect.io.Directory

class E2EBashNative extends FunSuite {
  // which platform to test
  val testName = "testbash"
  val funcFile = getClass.getResource(s"/$testName/functionality.yaml").getPath
  val platFile = getClass.getResource(s"/$testName/platform_native.yaml").getPath

  val temporaryFolder = Files.createTempDirectory(Paths.get("/tmp"), "viash_tester").toFile()

  val tempFolStr = temporaryFolder.toString()

  // parse functionality from file
  val functionality = Functionality.parse(new File(funcFile))

  // convert testpython
  val params = Array(
    "-f", funcFile,
    "-p", platFile,
    "export",
    "-o", tempFolStr
   )
  Main.main(params)

  // check whether executable was created
  val executable = Paths.get(tempFolStr, functionality.name).toFile()

  test("Viash should have created an executable") {
    assert(executable.exists())
    assert(executable.canExecute())
  }

  test("Check whether the executable can run") {
    Exec.run(
      Seq(executable.toString(), "--help"),
      temporaryFolder
    )
  }

  test("Check whether particular keywords can be found in the usage") {
    val stdout =
      Exec.run(
        Seq(executable.toString(), "--help"),
        temporaryFolder
      )

    functionality.arguments.foreach(arg => {
      assert(stdout.contains(arg.name))
      for (opt <- arg.alternatives; value <- opt)
        assert(stdout.contains(value))
      for (opt <- arg.description; value <- opt)
        assert(stdout.contains(value))
    })

  }

  test("Check whether output is correctly created") {
    val output = Paths.get(tempFolStr, "output.txt").toFile()
    val log = Paths.get(tempFolStr, "log.txt").toFile()

    val stdout =
      Exec.run(
        Seq(
          executable.toString(),
          executable.toString(),
          "--real_number", "10.5",
          "--whole_number=10",
          "-s", "a string with a few spaces",
          "--truth",
          "--output", output.toString(),
          "--log", log.toString(),
          "--optional", "foo",
          "--optional_with_default", "bar",
          "--passthrough=you shall$not#pass"
        ),
        temporaryFolder
      )

    assert(output.exists())
    assert(log.exists())

    val outputLines = Source.fromFile(output).mkString
    assert(outputLines.contains(s"""input: "${executable.toString()}""""))
    assert(outputLines.contains("""real_number: "10.5""""))
    assert(outputLines.contains("""whole_number: "10""""))
    assert(outputLines.contains("""s: "a string with a few spaces""""))
    assert(outputLines.contains("""truth: "true""""))
    assert(outputLines.contains(s"""output: "${output.toString()}""""))
    assert(outputLines.contains(s"""log: "${log.toString()}""""))
    assert(outputLines.contains("""optional: "foo""""))
    assert(outputLines.contains("""optional_with_default: "bar""""))
    assert(outputLines.contains("""passthrough: "you shall$not#pass"""")) // TODO: one set of quotes should be removed
    assert(outputLines.contains("""PASSTHROUGH: " --passthrough='you shall$not#pass'""""))
    assert(outputLines.contains(s"""resources_dir: "$tempFolStr""""))

    val logLines = Source.fromFile(log).mkString
    assert(logLines.contains("INFO: Parsed input arguments"))
  }

  test("Alternative params") {
    val stdout =
      Exec.run(
        Seq(
          executable.toString(),
          "testinput",
          "--real_number", "123.456",
          "--whole_number", "789",
          "-s", "my$weird#string"
        ),
        temporaryFolder
      )

    assert(stdout.contains(s"""input: "testinput""""))
    assert(stdout.contains("""real_number: "123.456""""))
    assert(stdout.contains("""whole_number: "789""""))
    assert(stdout.contains("""s: "my$weird#string""""))
    assert(stdout.contains("""truth: "false""""))
    assert(stdout.contains("""optional: """""))
    assert(stdout.contains("""optional_with_default: "The default value.""""))
    assert(stdout.contains("""passthrough: """""))
    assert(stdout.contains(s"""PASSTHROUGH: """""))
    assert(stdout.contains(s"""resources_dir: "$tempFolStr""""))

    assert(stdout.contains("INFO: Parsed input arguments"))
  }
}