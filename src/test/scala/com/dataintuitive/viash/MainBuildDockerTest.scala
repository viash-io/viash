package com.dataintuitive.viash

import org.scalatest.{BeforeAndAfterAll, FunSuite}
import java.nio.file.Paths

import com.dataintuitive.viash.config.Config
import com.dataintuitive.viash.functionality.Functionality

import scala.io.Source
import com.dataintuitive.viash.helpers._

class MainBuildDockerTest extends FunSuite with BeforeAndAfterAll {
  // which platform to test
  private val configFile = getClass.getResource(s"/testbash/config.vsh.yaml").getPath

  private val temporaryFolder = IO.makeTemp("viash_tester")
  private val tempFolStr = temporaryFolder.toString

  // parse functionality from file
  private val functionality = Config.read(configFile, modifyFun = false).functionality

  // check whether executable was created
  private val executable = Paths.get(tempFolStr, functionality.name).toFile
  private val execPathInDocker = Paths.get("/viash_automount", executable.getPath).toFile.toString

  private val configBashTagFile = getClass.getResource(s"/testbash/config_bash_tag.vsh.yaml").getPath
  private val functionalityBashTag = Config.read(configBashTagFile, modifyFun = false).functionality
  private val executableBashTagFile = Paths.get(tempFolStr, functionalityBashTag.name).toFile

  // convert testbash
  test("viash can create an executable") {
    TestHelper.testMain(Array(
      "build", configFile,
      "-p", "docker",
      "-o", tempFolStr
    ))

    assert(executable.exists)
    assert(executable.canExecute)
  }

  test("Check whether the executable can build the image", DockerTest) {
    val out = Exec.run2(
      Seq(executable.toString, "---setup")
    )
    assert(out.exitValue == 0)
  }

  test("Check whether the executable can run", DockerTest) {
    Exec.run(
      Seq(executable.toString, "-h")
    )
  }

  test("Check whether particular keywords can be found in the usage", DockerTest) {
    val stdout =
      Exec.run(
        Seq(executable.toString, "--help")
      )

    functionality.arguments.foreach(arg => {
      for (opt <- arg.alternatives; value <- opt)
        assert(stdout.contains(value))
      for (opt <- arg.description; value <- opt)
        assert(stdout.contains(value))
    })

  }

  test("Check whether output is correctly created", DockerTest) {
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
        "--output", output.getPath,
        "--log", log.getPath,
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
      assert(outputLines.contains(s"""input: |$execPathInDocker|"""))
      assert(outputLines.contains("""real_number: |10.5|"""))
      assert(outputLines.contains("""whole_number: |10|"""))
      assert(outputLines.contains("""s: |a string with a few spaces|"""))
      assert(outputLines.contains("""truth: |true|"""))
      assert(outputLines.contains(s"""output: |/viash_automount${output.getPath}|"""))
      assert(outputLines.contains(s"""log: |/viash_automount${log.getPath}|"""))
      assert(outputLines.contains("""optional: |foo|"""))
      assert(outputLines.contains("""optional_with_default: |bar|"""))
      assert(outputLines.contains("""multiple: |foo:bar|"""))
      assert(outputLines.contains("""multiple_pos: |a:b:c:d:e:f|"""))
      val regex = s"""resources_dir: |/viash_automount.*$tempFolStr/|""".r
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

  test("Alternative params", DockerTest) {
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

    assert(stdout.contains(s"""input: |$execPathInDocker|"""))
    assert(stdout.contains("""real_number: |123.456|"""))
    assert(stdout.contains("""whole_number: |789|"""))
    assert(stdout.contains("""s: |my$weird#string|"""))
    assert(stdout.contains("""truth: |false|"""))
    assert(stdout.contains("""optional: ||"""))
    assert(stdout.contains("""optional_with_default: |The default value.|"""))
    assert(stdout.contains("""multiple: ||"""))
    assert(stdout.contains("""multiple_pos: ||"""))
    val regex = s"""resources_dir: |/viash_automount.*$tempFolStr/|""".r
    assert(regex.findFirstIn(stdout).isDefined)

    assert(stdout.contains("INFO: Parsed input arguments"))
  }

  test("Get tagged version of a docker image for bash 5.0", DockerTest) {
    // prepare the environment
    TestHelper.testMain(Array(
      "build", configBashTagFile,
      "-p", "docker_5_0",
      "-o", tempFolStr
    ))

    assert(executableBashTagFile.exists)
    assert(executableBashTagFile.canExecute)

    // create the docker image
    val out = Exec.run2(
      Seq(executableBashTagFile.toString, "---setup")
    )
    assert(out.exitValue == 0)
    assert(out.exitValue == 0)

    // run the script
    val stdout =
      Exec.run(
        Seq(
          executable.toString
        )
      )

    assert(stdout.contains("GNU bash, version 5.0"))
  }

  test("Get tagged version of a docker image for bash 3.2", DockerTest) {
    // prepare the environment
    TestHelper.testMain(Array(
      "build", configBashTagFile,
      "-p", "docker_3_2",
      "-o", tempFolStr
    ))

    assert(executableBashTagFile.exists)
    assert(executableBashTagFile.canExecute)

    // create the docker image
    val out = Exec.run2(
      Seq(executableBashTagFile.toString, "---setup")
    )
    assert(out.exitValue == 0)
    assert(out.exitValue == 0)

    // run the script
    val stdout =
      Exec.run(
        Seq(
          executable.toString
        )
      )

    assert(stdout.contains("GNU bash, version 3.2"))
  }

  override def afterAll() {
    IO.deleteRecursively(temporaryFolder)
  }
}