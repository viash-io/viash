package io.viash.e2e.build

import io.viash._

import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import java.nio.file.{Files, Paths, StandardCopyOption}
import io.viash.helpers.{IO, Exec, Logger}

import io.viash.config.Config

import scala.io.Source
import io.viash.config.resources.PlainFile

class DockerSetup extends AnyFunSuite with BeforeAndAfterAll {
  Logger.UseColorOverride.value = Some(false)
  // which config to test
  private val configFile = getClass.getResource(s"/testbash/config.vsh.yaml").getPath

  private val temporaryFolder = IO.makeTemp("viash_tester")
  private val tempFolStr = temporaryFolder.toString
  
  private val temporaryConfigFolder = IO.makeTemp(s"viash_${this.getClass.getName}_")
  private val configDeriver = ConfigDeriver(Paths.get(configFile), temporaryConfigFolder)

  // parse config from file
  private val config = Config.read(configFile)

  // check whether executable was created
  private val executable = Paths.get(tempFolStr, config.name).toFile

  test("viash without --setup doesn't create docker during build", DockerTest) {
    val tag = "mytestbash"
    
    //remove docker if it exists
    removeDockerImage(tag, "0.1-throwawayimage")
    assert(!checkDockerImageExists(tag, "0.1-throwawayimage"))

    // build viash wrapper without --setup
    TestHelper.testMain(
      "build",
      "--engine", "throwawayimage",
      "--runner", "executable",
      "-o", tempFolStr,
      configFile
    )

    assert(executable.exists)
    assert(executable.canExecute)

    // verify docker still doesn't exist
    assert(!checkDockerImageExists(tag, "0.1-throwawayimage"))

    // run viash wrapper with ---setup
    val out = Exec.runCatch(
      Seq(executable.toString, "---setup", "build")
    )
    assert(out.exitValue == 0)

    // verify docker now exists
    assert(checkDockerImageExists(tag, "0.1-throwawayimage"))
  }

  test("viash with --setup creates docker during build", DockerTest) {
    val tag = "mytestbash"

    // remove docker if it exists
    removeDockerImage(tag, "0.1-throwawayimage")
    assert(!checkDockerImageExists(tag, "0.1-throwawayimage"))

    // build viash wrapper with --setup
    TestHelper.testMain(
      "build",
      "--engine", "throwawayimage",
      "--runner", "executable",
      "-o", tempFolStr,
      "--setup", "build",
      configFile
    )

    assert(executable.exists)
    assert(executable.canExecute)

    // verify docker exists
    assert(checkDockerImageExists(tag, "0.1-throwawayimage"))
  }

  test("Get info of a docker image using docker inspect", DockerTest) {
    val newConfigFilePath = configDeriver.derive(
      List(
        ".name := 'docker_setup_inspect'",
        ".version := '0.123'"
      ),
      "docker_setup_inspect")

    val fakeGitRepo = "git@non.existing.repo:viash/meta-test"

    assert(
      Exec.runCatchPath(
        List("git", "init"),
        cwd = Some(temporaryConfigFolder)
      ).exitValue == 0
      , "git init")

    assert(
      Exec.runCatchPath(
        List("git", "config", "user.email", "\"viash_test_build@example.com\""),
        cwd = Some(temporaryConfigFolder)
      ).exitValue == 0
      , "git config")

    assert(
      Exec.runCatchPath(
        List("git", "config", "user.name", "\"viash CI\""),
        cwd = Some(temporaryConfigFolder)
      ).exitValue == 0
      , "git config")

    assert(
      Exec.runCatchPath(
        List("git", "remote", "add", "origin", fakeGitRepo),
        cwd = Some(temporaryConfigFolder)
      ).exitValue == 0
      , "git remote add")

    assert(
      Exec.runCatchPath(
        List("git", "add", "*"),
        cwd = Some(temporaryConfigFolder)
      ).exitValue == 0
      , "git add *")


    val commitOut = Exec.runCatchPath(
        List("git", "commit", "-m", "\"initial commit\""),
        cwd = Some(temporaryConfigFolder)
      )
    assert(
      commitOut.exitValue == 0
      , s"git commit: ${commitOut.output}")

    // assert(
    //   Exec.runCatchPath(
    //     List("git", "tag", "v1"),
    //     cwd = Some(tempMetaFolder)
    //   ).exitValue == 0
    //   , "git tag")

    // Run the code
    // prepare the environment
    // override the functionality name & version so we don't have collisions with other docker images
    val stdout = TestHelper.testMain(
      "build",
      "--engine", "docker",
      "--runner", "executable",
      "-o", tempFolStr,
      "--setup", "alwaysbuild",
      newConfigFilePath
    )

    val inspectOut = Exec.run(
      Seq("docker", "inspect", s"docker_setup_inspect:0.123")
    )

    val regexOciAuthors = """"org.opencontainers.image.authors": "Bob ''' \\"\\"\\" \\\\n ` \$ \\\\ Cando"""".r
    val regexOciCreated = raw""""org.opencontainers.image.created": "((?:(\d{4}-\d{2}-\d{2})T(\d{2}:\d{2}:\d{2}(?:\.\d+)?))(Z|[\+-]\d{2}:\d{2})?)"""".r
    val regexOciDescription = """"org.opencontainers.image.description": "Companion container for running component docker_setup_inspect"""".r
    val regexOciRevision = """"org.opencontainers.image.revision": "[0-9a-f]{40}"""".r
    val regexOciSource = """"org.opencontainers.image.source": "https://non.existing.repo/viash/meta-test"""".r
    val regexOciVersion = """"org.opencontainers.image.version": "0.123"""".r

    assert(regexOciAuthors.findFirstIn(inspectOut).isDefined, inspectOut)
    assert(regexOciCreated.findFirstIn(inspectOut).isDefined, inspectOut)
    assert(regexOciDescription.findFirstIn(inspectOut).isDefined, inspectOut)
    assert(regexOciRevision.findFirstIn(inspectOut).isDefined, inspectOut)
    assert(regexOciSource.findFirstIn(inspectOut).isDefined, inspectOut)
    assert(regexOciVersion.findFirstIn(inspectOut).isDefined, inspectOut)
  }

  test("Get info of a docker image with target_organization set", DockerTest) {
    val newConfigFilePath = configDeriver.derive(".engines[.id == 'throwawayimage'].target_organization := 'myorg'", "mytestbash-org")
    val tag = "myorg/mytestbash"

    // remove docker if it exists
    removeDockerImage(tag, "0.1-throwawayimage")
    assert(!checkDockerImageExists(tag, "0.1-throwawayimage"))

    // build viash wrapper with --setup
    TestHelper.testMain(
      "build",
      "--engine", "throwawayimage",
      "--runner", "executable",
      "-o", tempFolStr,
      "--setup", "build",
      newConfigFilePath
    )

    assert(executable.exists)
    assert(executable.canExecute)

    // verify docker exists
    assert(checkDockerImageExists(tag, "0.1-throwawayimage"))
  }

  test("Get info of a docker image with target_package set", DockerTest) {
    val newConfigFilePath = configDeriver.derive(".engines[.id == 'throwawayimage'].target_package := 'pack'", "mytestbash-pack")
    val tag = "pack/mytestbash"

    // remove docker if it exists
    removeDockerImage(tag, "0.1-throwawayimage")
    assert(!checkDockerImageExists(tag, "0.1-throwawayimage"))

    // build viash wrapper with --setup
    TestHelper.testMain(
      "build",
      "--engine", "throwawayimage",
      "--runner", "executable",
      "-o", tempFolStr,
      "--setup", "build",
      newConfigFilePath
    )

    assert(executable.exists)
    assert(executable.canExecute)

    // verify docker exists
    assert(checkDockerImageExists(tag, "0.1-throwawayimage"))
  }

  test("Get info of a docker image with target_organization and target_package set", DockerTest) {
    val newConfigFilePath = configDeriver.derive(
      List(
        ".engines[.id == 'throwawayimage'].target_organization := 'myorg'",
        ".engines[.id == 'throwawayimage'].target_package := 'pack'"
      ),
      "mytestbash-pack")
    val tag = "myorg/pack/mytestbash"

    // remove docker if it exists
    removeDockerImage(tag, "0.1-throwawayimage")
    assert(!checkDockerImageExists(tag, "0.1-throwawayimage"))

    // build viash wrapper with --setup
    TestHelper.testMain(
      "build",
      "--engine", "throwawayimage",
      "--runner", "executable",
      "-o", tempFolStr,
      "--setup", "build",
      newConfigFilePath
    )

    assert(executable.exists)
    assert(executable.canExecute)

    // verify docker exists
    assert(checkDockerImageExists(tag, "0.1-throwawayimage"))
  }

  test("Get info of a docker image with organization and package name set in the package config", DockerTest) {
    val newConfigFilePath = configDeriver.derive(Nil, "mytestbash-package-config")
    val tag = "myorg_pc/pack_pc/mytestbash"

    val package_config =
      """name: pack_pc
        |organization: myorg_pc
        |""".stripMargin
    Files.write(temporaryConfigFolder.resolve("_viash.yaml"), package_config.getBytes)

    // remove docker if it exists
    removeDockerImage(tag, "0.1-throwawayimage")
    assert(!checkDockerImageExists(tag, "0.1-throwawayimage"))

    // build viash wrapper with --setup
    TestHelper.testMain(
      workingDir = Some(temporaryConfigFolder),
      "build",
      "--engine", "throwawayimage",
      "--runner", "executable",
      "-o", tempFolStr,
      "--setup", "build",
      newConfigFilePath
    )

    assert(executable.exists)
    assert(executable.canExecute)

    // verify docker exists
    assert(checkDockerImageExists(tag, "0.1-throwawayimage"))
  }

  def checkDockerImageExists(name: String): Boolean = checkDockerImageExists(name, "latest")

  def checkDockerImageExists(name: String, tag: String): Boolean = {
    val out = Exec.runCatch(
      Seq("docker", "images", name)
    )

    // print(out)
    val regex = s"$name\\s*$tag".r

    regex.findFirstIn(out.output).isDefined
  }

  def removeDockerImage(name: String): Unit = {
    Exec.runCatch(
      Seq("docker", "rmi", name, "-f")
    )
  }

  def removeDockerImage(name: String, tag: String): Unit = {
    Exec.runCatch(
      Seq("docker", "rmi", s"$name:$tag", "-f")
    )
  }

  override def afterAll(): Unit = {
    IO.deleteRecursively(temporaryFolder)
    IO.deleteRecursively(temporaryConfigFolder)
  }
}