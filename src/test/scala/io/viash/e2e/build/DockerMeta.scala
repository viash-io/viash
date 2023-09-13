package io.viash.e2e.build

import io.viash._

import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import java.nio.file.{Files, Paths, StandardCopyOption}
import io.viash.helpers.{IO, Exec, Logger}

import io.viash.config.Config

import scala.io.Source
import cats.instances.function
import io.viash.functionality.resources.PlainFile

class DockerMeta extends AnyFunSuite with BeforeAndAfterAll {
  Logger.UseColorOverride.value = Some(false)
  // which platform to test
  private val configFile = getClass.getResource(s"/testbash/config.vsh.yaml").getPath

  // parse functionality from file
  private val functionality = Config.read(configFile).functionality
  private def configAndResources = PlainFile(path = Some(configFile.toString)) :: functionality.resources

  test("Get meta data of a docker", DockerTest) {
    // Create temporary folder to copy the files to so we can do a git init in that folder
    // This is needed to check the remote git repo value
    val tempMetaFolder = IO.makeTemp("viash_test_meta")
    

    try {
      // Copy all needed files to a temporary location
      IO.writeResources(configAndResources, tempMetaFolder)

      val binDir = tempMetaFolder.resolve("bin")
      val configFile = tempMetaFolder.resolve("config.vsh.yaml")
      val exec = binDir.resolve(functionality.name)
      val meta = binDir.resolve(".config.vsh.yaml")

      // Run the code
      // prepare the environment
      val stdout = TestHelper.testMain(
        "build",
        "--engine", "docker",
        "--runner", "docker",
        "-o", binDir.toString,
        configFile.toString
      )
      
      // check exec
      assert(exec.toFile.exists)
      assert(exec.toFile.canExecute)
      
      // check meta
      assert(meta.toFile.exists, meta.toString + " should exist")
      val metaStr = Source.fromFile(meta.toFile).getLines().mkString("\n")

      val viashVersion = io.viash.Main.version

      val regexViashVersion = s"""viash_version: "${viashVersion}"""".r
      val regexConfig = s"""config: "${configFile}"""".r
      val regexEngine = """engine: "docker"""".r
      val regexRunner = """runner: "docker"""".r
      val regexExecutable = s"""executable: "$binDir/testbash"""".r
      val regexOutput = s"""output: "$binDir"""".r
      val regexNoRemoteGitRepo = "git_remote:".r

      assert(regexViashVersion.findFirstIn(metaStr).isDefined, stdout)
      assert(regexConfig.findFirstIn(metaStr).isDefined, stdout)
      assert(regexEngine.findFirstIn(metaStr).isDefined, stdout)
      assert(regexRunner.findFirstIn(metaStr).isDefined, stdout)
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
      IO.writeResources(configAndResources, tempMetaFolder)

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
        "--engine", "docker",
        "--runner", "docker",
        "-o", binDir.toString,
        configMetaFile.toString
      )
      
      // check exec
      assert(exec.toFile.exists)
      assert(exec.toFile.canExecute)

      // check meta
      assert(meta.toFile.exists, meta.toString + " should exist")
      val metaStr = Source.fromFile(meta.toFile).getLines().mkString("\n")

      val viashVersion = io.viash.Main.version

      val regexViashVersion = s"""viash_version: "$viashVersion"""".r
      val regexConfig = s"""config: "$configMetaFile"""".r
      val regexEngine = """engine: "docker"""".r
      val regexRunner = """runner: "docker"""".r
      val regexExecutable = s"""executable: "$binDir/testbash"""".r
      val regexOutput = s"""output: "$binDir"""".r
      val regexRemoteGitRepo = s"""git_remote: "$fakeGitRepo"""".r

      assert(regexViashVersion.findFirstIn(metaStr).isDefined, stdout)
      assert(regexConfig.findFirstIn(metaStr).isDefined, stdout)
      assert(regexEngine.findFirstIn(metaStr).isDefined, stdout)
      assert(regexRunner.findFirstIn(metaStr).isDefined, stdout)
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
      IO.writeResources(configAndResources, tempMetaFolder)

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
        "--engine", "docker",
        "--runner", "docker",
        "-o", binDir.toString,
        configMetaFile.toString
      )
      
      // check exec
      assert(exec.toFile.exists)
      assert(exec.toFile.canExecute)

      // check meta
      assert(meta.toFile.exists, meta.toString + " should exist")
      val metaStr = Source.fromFile(meta.toFile).getLines().mkString("\n")

      val viashVersion = io.viash.Main.version

      val regexViashVersion = s"""viash_version: "$viashVersion"""".r
      val regexConfig = s"""config: "$configMetaFile"""".r
      val regexEngine = """engine: "docker"""".r
      val regexRunner = """runner: "docker"""".r
      val regexExecutable = s"""executable: "$binDir/testbash"""".r
      val regexOutput = s"""output: "$binDir"""".r
      val regexRemoteGitRepo = """git_remote:"""".r

      assert(regexViashVersion.findFirstIn(metaStr).isDefined, stdout)
      assert(regexConfig.findFirstIn(metaStr).isDefined, stdout)
      assert(regexEngine.findFirstIn(metaStr).isDefined, stdout)
      assert(regexRunner.findFirstIn(metaStr).isDefined, stdout)
      assert(regexExecutable.findFirstIn(metaStr).isDefined, stdout)
      assert(regexOutput.findFirstIn(metaStr).isDefined, stdout)
      assert(regexRemoteGitRepo.findFirstIn(metaStr).isEmpty, stdout)

    }
    finally {
      IO.deleteRecursively(tempMetaFolder)
    }
  }
}