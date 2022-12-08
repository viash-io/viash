package io.viash.e2e.build

import io.viash._

import org.scalatest.{BeforeAndAfterAll, FunSuite}
import java.nio.file.{Files, Paths, StandardCopyOption}
import io.viash.helpers.{IO, Exec}

import io.viash.config.Config

import scala.io.Source
import io.viash.functionality.resources.PlainFile

class DockerSetup extends FunSuite with BeforeAndAfterAll {
  // which platform to test
  private val configFile = getClass.getResource(s"/testbash/config.vsh.yaml").getPath

  private val temporaryFolder = IO.makeTemp("viash_tester")
  private val tempFolStr = temporaryFolder.toString
  private val temporaryConfigFolder = IO.makeTemp("viash_tester_configs")

  private val configDeriver = ConfigDeriver(Paths.get(configFile), temporaryConfigFolder)

  // parse functionality from file
  private val functionality = Config.read(configFile).functionality
  private def configAndResources = PlainFile(path = Some(configFile.toString)) :: functionality.resources

  // check whether executable was created
  private val executable = Paths.get(tempFolStr, functionality.name).toFile
  private val execPathInDocker = Paths.get("/viash_automount", executable.getPath).toFile.toString

  //</editor-fold>
  //<editor-fold desc="Test benches to check building with or without --setup flag">
  test("viash without --setup doesn't create docker during build", DockerTest) {
    val tag = "throwawayimage"
    
    //remove docker if it exists
    removeDockerImage(tag, "0.1")
    assert(!checkDockerImageExists(tag, "0.1"))

    // build viash wrapper without --setup
    TestHelper.testMain(
      "build",
      "-p", "throwawayimage",
      "-o", tempFolStr,
      configFile
    )

    assert(executable.exists)
    assert(executable.canExecute)

    // verify docker still doesn't exist
    assert(!checkDockerImageExists(tag, "0.1"))

    // run viash wrapper with ---setup
    val out = Exec.runCatch(
      Seq(executable.toString, "---setup", "build")
    )
    assert(out.exitValue == 0)

    // verify docker now exists
    assert(checkDockerImageExists(tag, "0.1"))
  }

  test("viash with --setup creates docker during build", DockerTest) {
    val tag = "throwawayimage"

    // remove docker if it exists
    removeDockerImage(tag, "0.1")
    assert(!checkDockerImageExists(tag, "0.1"))

    // build viash wrapper with --setup
    TestHelper.testMain(
      "build",
      "-p", "throwawayimage",
      "-o", tempFolStr,
      "--setup", "build",
      configFile
    )

    assert(executable.exists)
    assert(executable.canExecute)

    // verify docker exists
    assert(checkDockerImageExists(tag, "0.1"))
  }
  //</editor-fold>

  test("Get info of a docker image using docker inspect", DockerTest) {
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
          List("git", "config", "user.email", "\"viash_test_build@example.com\""),
          cwd = Some(tempMetaFolder)
        ).exitValue == 0
        , "git config")

      assert(
        Exec.runCatchPath(
          List("git", "config", "user.name", "\"viash CI\""),
          cwd = Some(tempMetaFolder)
        ).exitValue == 0
        , "git config")

      assert(
        Exec.runCatchPath(
          List("git", "remote", "add", "origin", fakeGitRepo),
          cwd = Some(tempMetaFolder)
        ).exitValue == 0
        , "git remote add")

      assert(
        Exec.runCatchPath(
          List("git", "add", "*"),
          cwd = Some(tempMetaFolder)
        ).exitValue == 0
        , "git add *")


      val commitOut = Exec.runCatchPath(
          List("git", "commit", "-m", "\"initial commit\""),
          cwd = Some(tempMetaFolder)
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

      val configFile = tempMetaFolder.resolve("config.vsh.yaml")

      // Run the code
      // prepare the environment
      val stdout = TestHelper.testMain(
        "build",
        "-p", "docker",
        "-o", tempFolStr,
        "--setup", "alwaysbuild",
        configFile.toString
      )

      assert(executable.exists)
      assert(executable.canExecute)

      val inspectOut = Exec.run(
        Seq("docker", "inspect", s"${functionality.name}:${functionality.version.get}")
      )

      val regexOciAuthors = """"org.opencontainers.image.authors": "Bob ''' \\"\\"\\" \\\\n ` \$ \\\\ Cando"""".r
      val regexOciCreated = raw""""org.opencontainers.image.created": "((?:(\d{4}-\d{2}-\d{2})T(\d{2}:\d{2}:\d{2}(?:\.\d+)?))(Z|[\+-]\d{2}:\d{2})?)"""".r
      val regexOciDescription = """"org.opencontainers.image.description": "Companion container for running component testbash"""".r
      val regexOciRevision = """"org.opencontainers.image.revision": "[0-9a-f]{40}"""".r
      val regexOciSource = """"org.opencontainers.image.source": "https://non.existing.repo/viash/meta-test"""".r
      val regexOciVersion = """"org.opencontainers.image.version": "0.1"""".r


      assert(regexOciAuthors.findFirstIn(inspectOut).isDefined, inspectOut)
      assert(regexOciCreated.findFirstIn(inspectOut).isDefined, inspectOut)
      assert(regexOciDescription.findFirstIn(inspectOut).isDefined, inspectOut)
      assert(regexOciRevision.findFirstIn(inspectOut).isDefined, inspectOut)
      assert(regexOciSource.findFirstIn(inspectOut).isDefined, inspectOut)
      assert(regexOciVersion.findFirstIn(inspectOut).isDefined, inspectOut)

    }
    finally {
      IO.deleteRecursively(tempMetaFolder)
    }
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

  override def afterAll() {
    IO.deleteRecursively(temporaryFolder)
    IO.deleteRecursively(temporaryConfigFolder)
  }
}