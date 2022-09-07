package io.viash.auxiliary

import io.viash.{DockerTest, TestHelper}
import io.viash.config.Config
import io.viash.helpers.{IO, Exec}
import org.scalatest.{BeforeAndAfterAll, FunSuite}

import java.nio.file.{Files, Paths}
import scala.io.Source

class MainBuildAuxiliaryDockerTag extends FunSuite with BeforeAndAfterAll {
  private val temporaryFolder = IO.makeTemp("viash_tester")
  private val tempFolStr = temporaryFolder.toString

  private val configBashTagFile = getClass.getResource(s"/testbash/auxiliary_tag/config_bash_tag.vsh.yaml").getPath
  private val functionalityBashTag = Config.read(configBashTagFile, applyPlatform = false).functionality
  private val executableBashTagFile = Paths.get(tempFolStr, functionalityBashTag.name).toFile

  test("Get tagged version of a docker image for bash 5.0", DockerTest) {
    // prepare the environment
    TestHelper.testMain(
      "build",
      "-p", "testtag1",
      "-o", tempFolStr,
      configBashTagFile
    )

    assert(executableBashTagFile.exists)
    assert(executableBashTagFile.canExecute)

    // create the docker image
    val out = Exec.runCatch(
      Seq(executableBashTagFile.toString, "---setup", "build")
    )
    assert(out.exitValue == 0)

    // run the script
    val stdout = Exec.run(Seq(executableBashTagFile.toString))
    assert(stdout.contains("GNU bash, version 5.0"))

    // check whether the internal docker is correct
    val dockerout = Exec.run(Seq(executableBashTagFile.toString, "---dockerfile"))
    // we expect something basic like
    // FROM bash:5.0
    // LABEL ...
    // RUN :
    // Allow for extra spaces just in case the format changes slightly format-wise but without functional differences
    val regex = """^FROM bash:5\.0[\r\n\s]*.*""".r
    assert(regex.findFirstIn(dockerout).isDefined, regex.toString)
  }

  test("Get tagged version of a docker image for bash 3.2", DockerTest) {
    // prepare the environment
    TestHelper.testMain(
      "build",
      "-p", "testtag2",
      "-o", tempFolStr,
      configBashTagFile
    )

    assert(executableBashTagFile.exists)
    assert(executableBashTagFile.canExecute)

    // create the docker image
    val out = Exec.runCatch(
      Seq(executableBashTagFile.toString, "---setup", "build")
    )
    assert(out.exitValue == 0)

    // run the script
    val stdout = Exec.run(Seq(executableBashTagFile.toString))
    assert(stdout.contains("GNU bash, version 3.2"))

    // check whether the internal docker is correct
    val dockerout = Exec.run(Seq(executableBashTagFile.toString, "---dockerfile"))
    // we expect something basic like
    // FROM bash:3.2
    // LABEL ...
    // RUN :
    // Allow for extra spaces just in case the format changes slightly format-wise but without functional differences
    val regex = """^FROM bash:3\.2[\r\n\s]*.*""".r
    assert(regex.findFirstIn(dockerout).isDefined, regex.toString)
  }

  test("Check whether target image name is well formed without target_image, version or registry", DockerTest) {
    // prepare the environment
    TestHelper.testMain(
      "build",
      "-p", "testtargetimage1",
      "-o", tempFolStr,
      configBashTagFile
    )

    assert(executableBashTagFile.exists)
    assert(executableBashTagFile.canExecute)

    // create the docker image
    val out = Exec.runCatch(
      Seq(executableBashTagFile.toString, "---setup", "build")
    )
    assert(out.exitValue == 0)

    // run the script
    val stdout = Exec.run(Seq(executableBashTagFile.toString))
    assert(stdout.contains("GNU bash, version 5.0"))

    // check whether script is using the expected docker image
    val contentSource = Source.fromFile(executableBashTagFile)
    val content = try {
      contentSource.getLines().toList
    } finally {
      contentSource.close()
    }
    assert(content.exists(_.matches("cat << VIASHEOF \\| eval docker run .* testbash_tag:latest")))
  }

  test("Check whether target image name is well formed with target_image, version, and registry", DockerTest) {
    // prepare the environment
    TestHelper.testMain(
      "build",
      "-p", "testtargetimage2",
      "-o", tempFolStr,
      configBashTagFile
    )

    assert(executableBashTagFile.exists)
    assert(executableBashTagFile.canExecute)

    // create the docker image
    val out = Exec.runCatch(
      Seq(executableBashTagFile.toString, "---setup", "build")
    )
    assert(out.exitValue == 0)

    // run the script
    val stdout = Exec.run(Seq(executableBashTagFile.toString))
    assert(stdout.contains("GNU bash, version 5.0"))

    // check whether script is using the expected docker image
    val contentSource = Source.fromFile(executableBashTagFile)
    val content = try {
      contentSource.getLines().toList
    } finally {
      contentSource.close()
    }
    assert(content.exists(_.matches("cat << VIASHEOF \\| eval docker run .* foo.io/bar:0\\.0\\.1")))
  }

  test("Check whether target image name is well formed with target_image, target_tag", DockerTest) {
    // prepare the environment
    TestHelper.testMain(
      "build",
      "-p", "testtargetimage3",
      "-o", tempFolStr,
      configBashTagFile
    )

    assert(executableBashTagFile.exists)
    assert(executableBashTagFile.canExecute)

    // create the docker image
    val out = Exec.runCatch(
      Seq(executableBashTagFile.toString, "---setup", "build")
    )
    assert(out.exitValue == 0)

    // run the script
    val stdout = Exec.run(Seq(executableBashTagFile.toString))
    assert(stdout.contains("GNU bash, version 3.2"))

    // check whether script is using the expected docker image
    val contentSource = Source.fromFile(executableBashTagFile)
    val content = try {
      contentSource.getLines().toList
    } finally {
      contentSource.close()
    }
    assert(content.exists(_.matches("cat << VIASHEOF \\| eval docker run .* bar:0\\.0\\.2")))
  }

  override def afterAll() {
    IO.deleteRecursively(temporaryFolder)
  }
}