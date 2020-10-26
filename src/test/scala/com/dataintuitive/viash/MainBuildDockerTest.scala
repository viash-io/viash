package com.dataintuitive.viash

import org.scalatest.{BeforeAndAfterAll, FunSuite}
import java.nio.file.{Files, Paths}

import com.dataintuitive.viash.config.Config
import com.dataintuitive.viash.functionality.Functionality

import scala.io.Source
import com.dataintuitive.viash.helpers._

import scala.util.Try

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

  //<editor-fold desc="Test benches to build a generic script and run various commands to see if the functionality is correct">
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
  //</editor-fold>
  //<editor-fold desc="Test benches to check building tagged docker images">
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
  //</editor-fold>
  //<editor-fold desc="Test benches to check building with --meta flag">
  test("Get meta data of a docker", DockerTest) {
    // Create temporary folder to copy the files to so we can do a git init in that folder
    // This is needed to check the remote git repo value
    val tempMetaFolder = IO.makeTemp("viash_test_meta")
    val tempMetaFolStr = tempMetaFolder.toString

    try {
      // Copy all needed files to a temporary location
      for (name <- List("config.vsh.yaml", "code.sh", "resource1.txt")) {
        val originPath = Paths.get(getClass.getResource(s"/testbash/$name").getPath)
        val destPath = Paths.get(tempMetaFolStr, name)

        //println(s"Copy $originPath to $destPath")
        Files.copy(originPath, destPath)
      }

      val configMetaFile = Paths.get(tempMetaFolStr, "config.vsh.yaml").toString

      // Run the code
      // prepare the environment
      val stdout = TestHelper.testMain(Array(
        "build", configMetaFile,
        "-p", "docker",
        "-o", tempFolStr,
        "-m",
      ))

      assert(executableBashTagFile.exists)
      assert(executableBashTagFile.canExecute)

      val viashVersion = com.dataintuitive.viash.Main.version

      val regexViashVersion = s"viash version:\\s*$viashVersion".r
      val regexConfig = s"config:\\s*$configMetaFile".r
      val regexPlatform = "platform:\\s*docker".r
      val regexExecutable = s"executable:\\s*$tempFolStr/testbash".r
      val regexOutput = s"output:\\s*$tempFolStr".r
      val regexNoRemoteGitRepo = "remote git repo:\\s*<NA>".r

      assert(regexViashVersion.findFirstIn(stdout).isDefined, stdout)
      assert(regexConfig.findFirstIn(stdout).isDefined, stdout)
      assert(regexPlatform.findFirstIn(stdout).isDefined, stdout)
      assert(regexExecutable.findFirstIn(stdout).isDefined, stdout)
      assert(regexOutput.findFirstIn(stdout).isDefined, stdout)
      assert(regexNoRemoteGitRepo.findFirstIn(stdout).isDefined, stdout)

    }
    finally {
      IO.deleteRecursively(tempMetaFolder)
    }
  }

  test("Get meta data of a docker with git repo", DockerTest) {
    // Create temporary folder to copy the files to so we can do a git init in that folder
    // This is needed to check the remote git repo value
    val tempMetaFolder = IO.makeTemp("viash_test_meta")
    val tempMetaFolStr = tempMetaFolder.toString

    val fakeGitRepo = "git@non.existing.repo:viash/meta-test"

    try {
      // Copy all needed files to a temporary location
      for (name <- List("config.vsh.yaml", "code.sh", "resource1.txt")) {
        val originPath = Paths.get(getClass.getResource(s"/testbash/$name").getPath)
        val destPath = Paths.get(tempMetaFolStr, name)

        //println(s"Copy $originPath to $destPath")
        Files.copy(originPath, destPath)
      }

      assert(
        Exec.run2(
          List("git", "init"),
          cwd = Some(tempMetaFolder)
        ).exitValue == 0
        , "git init")

      assert(
        Exec.run2(
          List("git", "remote", "add", "origin", fakeGitRepo),
          cwd = Some(tempMetaFolder)
        ).exitValue == 0
        , "git remote add")

      val configMetaFile = Paths.get(tempMetaFolStr, "config.vsh.yaml").toString

      // Run the code
      // prepare the environment
      val stdout = TestHelper.testMain(Array(
        "build", configMetaFile,
        "-p", "docker",
        "-o", tempFolStr,
        "-m",
      ))

      assert(executableBashTagFile.exists)
      assert(executableBashTagFile.canExecute)

      val viashVersion = com.dataintuitive.viash.Main.version

      val regexViashVersion = s"viash version:\\s*$viashVersion".r
      val regexConfig = s"config:\\s*$configMetaFile".r
      val regexPlatform = "platform:\\s*docker".r
      val regexExecutable = s"executable:\\s*$tempFolStr/testbash".r
      val regexOutput = s"output:\\s*$tempFolStr".r
      val regexRemoteGitRepo = s"remote git repo:\\s*$fakeGitRepo".r

      assert(regexViashVersion.findFirstIn(stdout).isDefined, stdout)
      assert(regexConfig.findFirstIn(stdout).isDefined, stdout)
      assert(regexPlatform.findFirstIn(stdout).isDefined, stdout)
      assert(regexExecutable.findFirstIn(stdout).isDefined, stdout)
      assert(regexOutput.findFirstIn(stdout).isDefined, stdout)
      assert(regexRemoteGitRepo.findFirstIn(stdout).isDefined, stdout)

    }
    finally {
      IO.deleteRecursively(tempMetaFolder)
    }
  }

  test("Get meta data of a docker with git repo, no remote", DockerTest) {
    // Create temporary folder to copy the files to so we can do a git init in that folder
    // This is needed to check the remote git repo value
    val tempMetaFolder = IO.makeTemp("viash_test_meta")
    val tempMetaFolStr = tempMetaFolder.toString

    try {
      // Copy all needed files to a temporary location
      for (name <- List("config.vsh.yaml", "code.sh", "resource1.txt")) {
        val originPath = Paths.get(getClass.getResource(s"/testbash/$name").getPath)
        val destPath = Paths.get(tempMetaFolStr, name)

        //println(s"Copy $originPath to $destPath")
        Files.copy(originPath, destPath)
      }

      assert(
        Exec.run2(
          List("git", "init"),
          cwd = Some(tempMetaFolder)
        ).exitValue == 0
      , "git init")

      val configMetaFile = Paths.get(tempMetaFolStr, "config.vsh.yaml").toString

      // Run the code
      // prepare the environment
      val stdout = TestHelper.testMain(Array(
        "build", configMetaFile,
        "-p", "docker",
        "-o", tempFolStr,
        "-m",
      ))

      assert(executableBashTagFile.exists)
      assert(executableBashTagFile.canExecute)

      val viashVersion = com.dataintuitive.viash.Main.version

      val regexViashVersion = s"viash version:\\s*$viashVersion".r
      val regexConfig = s"config:\\s*$configMetaFile".r
      val regexPlatform = "platform:\\s*docker".r
      val regexExecutable = s"executable:\\s*$tempFolStr/testbash".r
      val regexOutput = s"output:\\s*$tempFolStr".r
      val regexRemoteGitRepo = "remote git repo:\\s*No remote configured".r

      assert(regexViashVersion.findFirstIn(stdout).isDefined, stdout)
      assert(regexConfig.findFirstIn(stdout).isDefined, stdout)
      assert(regexPlatform.findFirstIn(stdout).isDefined, stdout)
      assert(regexExecutable.findFirstIn(stdout).isDefined, stdout)
      assert(regexOutput.findFirstIn(stdout).isDefined, stdout)
      assert(regexRemoteGitRepo.findFirstIn(stdout).isDefined, stdout)

    }
    finally {
      IO.deleteRecursively(tempMetaFolder)
    }
  }

  //</editor-fold>

  override def afterAll() {
    IO.deleteRecursively(temporaryFolder)
  }
}