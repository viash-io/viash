package com.dataintuitive.viash

import org.scalatest.{BeforeAndAfterAll, FunSuite}
import java.nio.file.Paths

import com.dataintuitive.viash.config.Config

import scala.io.Source
import com.dataintuitive.viash.helpers._

class MainBuildNativeSuite extends FunSuite with BeforeAndAfterAll {
  // which platform to test
  private val configFile = getClass.getResource(s"/testbash/config.vsh.yaml").getPath
  private val configNoPlatformFile = getClass.getResource(s"/testbash/config_no_platform.vsh.yaml").getPath
  private val configPlatformFile = getClass.getResource(s"/testbash/config_platform_native.vsh.yaml").getPath

  private val temporaryFolder = IO.makeTemp("viash_tester")
  private val tempFolStr = temporaryFolder.toString

  // parse functionality from file
  private val functionality = Config.read(configFile, modifyFun = false).functionality

  // check whether executable was created
  private val executable = Paths.get(tempFolStr, functionality.name).toFile

  // convert testbash
  test("viash can create an executable") {
    TestHelper.testMain(
      "build",
      "-p", "native",
      "-o", tempFolStr,
      configFile
    )

    assert(executable.exists)
    assert(executable.canExecute)
  }

  test("Check whether the executable can run") {
    Exec.run(
      Seq(executable.toString, "--help")
    )
  }

  test("Check whether particular keywords can be found in the usage") {
    val stdout =
      Exec.run(
        Seq(executable.toString, "--help")
      )

    val stripAll = (s : String) => s.replaceAll(raw"\s+", " ").strip()

    functionality.arguments.foreach(arg => {
      for (opt <- arg.alternatives; value <- opt)
        assert(stdout.contains(value))
      for (description <- arg.description) {
        assert(stripAll(stdout).contains(stripAll(description)))
      }
    })
  }

  test("Check whether output is correctly created") {
    val output = Paths.get(tempFolStr, "output.txt").toFile
    val log = Paths.get(tempFolStr, "log.txt").toFile

    Exec.run(
      Seq(
        executable.toString,
        executable.toString,
        "--real_number", "10.5",
        "--whole_number=10",
        "-s", "a string with a few spaces",
        "a", "b", "c",
        "--truth",
        "--output", output.toString,
        "--log", log.toString,
        "--optional", "foo",
        "--optional_with_default", "bar",
        "--multiple", "foo",
        "--multiple=bar",
        "d", "e", "f"
      )
    )

    assert(output.exists())
    assert(log.exists())

    val outputSrc = Source.fromFile(output)
    try {
      val outputLines = outputSrc.mkString
      assert(outputLines.contains(s"""input: |${executable.toString}|"""))
      assert(outputLines.contains("""real_number: |10.5|"""))
      assert(outputLines.contains("""whole_number: |10|"""))
      assert(outputLines.contains("""s: |a string with a few spaces|"""))
      assert(outputLines.contains("""truth: |true|"""))
      assert(outputLines.contains(s"""output: |${output.toString}|"""))
      assert(outputLines.contains(s"""log: |${log.toString}|"""))
      assert(outputLines.contains("""optional: |foo|"""))
      assert(outputLines.contains("""optional_with_default: |bar|"""))
      assert(outputLines.contains("""multiple: |foo:bar|"""))
      assert(outputLines.contains("""multiple_pos: |a:b:c:d:e:f|"""))
      val regex = s"""resources_dir: \\|.*$tempFolStr\\|""".r
      assert(regex.findFirstIn(outputLines).isDefined)
    } finally {
      outputSrc.close()
    }

    val logSrc = Source.fromFile(log)
    try {
      val logLines = logSrc.mkString
      assert(logLines.contains("INFO: Parsed input arguments"))
    } finally {
      logSrc.close()
    }
  }

  test("Alternative params") {
    val stdout =
      Exec.run(
        Seq(
          executable.toString,
          executable.toString,
          "--real_number", "123.456",
          "--whole_number", "789",
          "-s", "my$weird#string"
        )
      )

    assert(stdout.contains(s"""input: |${executable.toString}|"""))
    assert(stdout.contains("""real_number: |123.456|"""))
    assert(stdout.contains("""whole_number: |789|"""))
    assert(stdout.contains("""s: |my$weird#string|"""))
    assert(stdout.contains("""truth: |false|"""))
    assert(stdout.contains("""optional: ||"""))
    assert(stdout.contains("""optional_with_default: |The default value.|"""))
    assert(stdout.contains("""multiple: ||"""))
    assert(stdout.contains("""multiple_pos: ||"""))
    val regex = s"""resources_dir: \\|.*$tempFolStr\\|""".r
    assert(regex.findFirstIn(stdout).isDefined)

    assert(stdout.contains("INFO: Parsed input arguments"))
  }

  test("when -p is omitted, the system should run as native") {
    val testText = TestHelper.testMain(
      "build",
      "-o", tempFolStr,
      "-m",
      configNoPlatformFile
    )

    assert(executable.exists)
    assert(executable.canExecute)

    Exec.run(
      Seq(executable.toString, "--help")
    )

    val regexPlatform = "platform:\\s*<NA>".r
    assert(regexPlatform.findFirstIn(testText).isDefined, testText)
  }

  test("Specify platform (native) in config yaml") {
    val testText = TestHelper.testMain(
      "build",
      "-o", tempFolStr,
      "-m",
      configPlatformFile
    )

    assert(executable.exists)
    assert(executable.canExecute)

    val out = Exec.run2(
      Seq(executable.toString, "--help")
    )
    assert(out.exitValue == 0)

    val regexPlatform = "platform:\\s*<NA>".r
    assert(regexPlatform.findFirstIn(testText).isDefined, testText)
  }

  override def afterAll() {
    IO.deleteRecursively(temporaryFolder)
  }
}