package io.viash.e2e.build

import io.viash._

import org.scalatest.{BeforeAndAfterAll, FunSuite}
import java.nio.file.{Files, Paths, StandardCopyOption}
import io.viash.helpers.{IO, Exec}

import io.viash.config.Config

import scala.io.Source

class DockerMeta extends FunSuite with BeforeAndAfterAll {
  // which platform to test
  private val configFile = getClass.getResource(s"/testbash/config.vsh.yaml").getPath

  // parse functionality from file
  private val functionality = Config.read(configFile).functionality


  //</editor-fold>
  //<editor-fold desc="Test benches to check building with --meta flag">
  test("Get meta data of a docker", DockerTest) {
    // Create temporary folder to copy the files to so we can do a git init in that folder
    // This is needed to check the remote git repo value
    val tempMetaFolder = IO.makeTemp("viash_test_meta")

    try {
      // Copy all needed files to a temporary location
      for (name <- List("config.vsh.yaml", "code.sh", "resource1.txt")) {
        val originPath = Paths.get(getClass.getResource(s"/testbash/$name").getPath)
        val destPath = tempMetaFolder.resolve(name)

        //println(s"Copy $originPath to $destPath")
        Files.copy(originPath, destPath)
      }

      val binDir = tempMetaFolder.resolve("bin")
      val configMetaFile = tempMetaFolder.resolve("config.vsh.yaml")
      val exec = binDir.resolve(functionality.name)
      val meta = binDir.resolve(".config.vsh.yaml")

      // Run the code
      // prepare the environment
      val stdout = TestHelper.testMain(
        "build",
        "-p", "docker",
        "-o", binDir.toString,
        configMetaFile.toString
      )
      
      // check exec
      assert(exec.toFile.exists)
      assert(exec.toFile.canExecute)

      // check meta
      assert(meta.toFile.exists)
      val metaStr = scala.io.Source.fromFile(meta.toFile).getLines.mkString("\n")

      val viashVersion = io.viash.Main.version

      val regexViashVersion = s"""viash_version: "${viashVersion}"""".r
      val regexConfig = s"""config: "${configMetaFile}"""".r
      val regexPlatform = """platform: "docker"""".r
      val regexExecutable = s"""executable: "$binDir/testbash"""".r
      val regexOutput = s"""output: "$binDir"""".r
      val regexNoRemoteGitRepo = "git_remote:".r

      assert(regexViashVersion.findFirstIn(metaStr).isDefined, stdout)
      assert(regexConfig.findFirstIn(metaStr).isDefined, stdout)
      assert(regexPlatform.findFirstIn(metaStr).isDefined, stdout)
      assert(regexExecutable.findFirstIn(metaStr).isDefined, stdout)
      assert(regexOutput.findFirstIn(metaStr).isDefined, stdout)
      assert(regexNoRemoteGitRepo.findFirstIn(metaStr).isEmpty, stdout)

    }
    finally {
      IO.deleteRecursively(tempMetaFolder)
    }
  }

  test("Get meta data of a docker with git repo", DockerTest) {
    // Create temporary folder to copy the files to so we can do a git init in that folder
    // This is needed to check the remote git repo value
    val tempMetaFolder = IO.makeTemp("viash_test_meta")

    val fakeGitRepo = "git@non.existing.repo:viash/meta-test"

    try {
      // Copy all needed files to a temporary location
      for (name <- List("config.vsh.yaml", "code.sh", "resource1.txt")) {
        val originPath = Paths.get(getClass.getResource(s"/testbash/$name").getPath)
        val destPath = tempMetaFolder.resolve(name)

        //println(s"Copy $originPath to $destPath")
        Files.copy(originPath, destPath)
      }

      assert(
        Exec.runCatchPath(
          List("git", "init"),
          cwd = Some(tempMetaFolder)
        ).exitValue == 0
        , "git init")

      assert(
        Exec.runCatchPath(
          List("git", "remote", "add", "origin", fakeGitRepo),
          cwd = Some(tempMetaFolder)
        ).exitValue == 0
        , "git remote add")

      val configMetaFile = tempMetaFolder.resolve("config.vsh.yaml")
      val binDir = tempMetaFolder.resolve("bin")
      val exec = binDir.resolve(functionality.name)
      val meta = binDir.resolve(".config.vsh.yaml")

      // Run the code
      // prepare the environment
      val stdout = TestHelper.testMain(
        "build",
        "-p", "docker",
        "-o", binDir.toString,
        configMetaFile.toString
      )
      
      // check exec
      assert(exec.toFile.exists)
      assert(exec.toFile.canExecute)

      // check meta
      assert(meta.toFile.exists)
      val metaStr = scala.io.Source.fromFile(meta.toFile).getLines.mkString("\n")

      val viashVersion = io.viash.Main.version

      val regexViashVersion = s"""viash_version: "$viashVersion"""".r
      val regexConfig = s"""config: "$configMetaFile"""".r
      val regexPlatform = """platform: "docker"""".r
      val regexExecutable = s"""executable: "$binDir/testbash"""".r
      val regexOutput = s"""output: "$binDir"""".r
      val regexRemoteGitRepo = s"""git_remote: "$fakeGitRepo"""".r

      assert(regexViashVersion.findFirstIn(metaStr).isDefined, stdout)
      assert(regexConfig.findFirstIn(metaStr).isDefined, stdout)
      assert(regexPlatform.findFirstIn(metaStr).isDefined, stdout)
      assert(regexExecutable.findFirstIn(metaStr).isDefined, stdout)
      assert(regexOutput.findFirstIn(metaStr).isDefined, stdout)
      assert(regexRemoteGitRepo.findFirstIn(metaStr).isDefined, stdout)

    }
    finally {
      IO.deleteRecursively(tempMetaFolder)
    }
  }

  test("Get meta data of a docker with git repo, no remote", DockerTest) {
    // Create temporary folder to copy the files to so we can do a git init in that folder
    // This is needed to check the remote git repo value
    val tempMetaFolder = IO.makeTemp("viash_test_meta")

    try {
      // Copy all needed files to a temporary location
      for (name <- List("config.vsh.yaml", "code.sh", "resource1.txt")) {
        val originPath = Paths.get(getClass.getResource(s"/testbash/$name").getPath)
        val destPath = tempMetaFolder.resolve(name)

        //println(s"Copy $originPath to $destPath")
        Files.copy(originPath, destPath)
      }

      assert(
        Exec.runCatchPath(
          List("git", "init"),
          cwd = Some(tempMetaFolder)
        ).exitValue == 0
      , "git init")

      val configMetaFile = tempMetaFolder.resolve("config.vsh.yaml")
      val binDir = tempMetaFolder.resolve("bin")
      val exec = binDir.resolve(functionality.name)
      val meta = binDir.resolve(".config.vsh.yaml")

      // Run the code
      // prepare the environment
      val stdout = TestHelper.testMain(
        "build",
        "-p", "docker",
        "-o", binDir.toString,
        configMetaFile.toString
      )
      
      // check exec
      assert(exec.toFile.exists)
      assert(exec.toFile.canExecute)

      // check meta
      assert(meta.toFile.exists, meta.toString + " should exist")
      val metaStr = scala.io.Source.fromFile(meta.toFile).getLines.mkString("\n")

      val viashVersion = io.viash.Main.version

      val regexViashVersion = s"""viash_version: "$viashVersion"""".r
      val regexConfig = s"""config: "$configMetaFile"""".r
      val regexPlatform = """platform: "docker"""".r
      val regexExecutable = s"""executable: "$binDir/testbash"""".r
      val regexOutput = s"""output: "$binDir"""".r
      val regexRemoteGitRepo = """git_remote:"""".r

      assert(regexViashVersion.findFirstIn(metaStr).isDefined, stdout)
      assert(regexConfig.findFirstIn(metaStr).isDefined, stdout)
      assert(regexPlatform.findFirstIn(metaStr).isDefined, stdout)
      assert(regexExecutable.findFirstIn(metaStr).isDefined, stdout)
      assert(regexOutput.findFirstIn(metaStr).isDefined, stdout)
      assert(regexRemoteGitRepo.findFirstIn(metaStr).isEmpty, stdout)

    }
    finally {
      IO.deleteRecursively(tempMetaFolder)
    }
  }

}