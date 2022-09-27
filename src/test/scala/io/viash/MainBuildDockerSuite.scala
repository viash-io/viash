package io.viash

import org.scalatest.{BeforeAndAfterAll, FunSuite}
import java.nio.file.{Files, Paths, StandardCopyOption}
import io.viash.helpers.{IO, Exec}

import io.viash.config.Config

import scala.io.Source

class MainBuildDockerSuite extends FunSuite with BeforeAndAfterAll {
  // which platform to test
  private val configFile = getClass.getResource(s"/testbash/config.vsh.yaml").getPath

  private val configPlatformFile = getClass.getResource(s"/testbash/config_platform_docker.vsh.yaml").getPath

  private val temporaryFolder = IO.makeTemp("viash_tester")
  private val tempFolStr = temporaryFolder.toString
  private val temporaryConfigFolder = IO.makeTemp("viash_tester_configs")

  private val configDeriver = ConfigDeriver(Paths.get(configFile), temporaryConfigFolder)

  // parse functionality from file
  private val functionality = Config.read(configFile, applyPlatform = false).functionality

  // check whether executable was created
  private val executable = Paths.get(tempFolStr, functionality.name).toFile
  private val execPathInDocker = Paths.get("/viash_automount", executable.getPath).toFile.toString

  //<editor-fold desc="Test benches to build a generic script and run various commands to see if the functionality is correct">
  // convert testbash
  test("viash can create an executable") {
    TestHelper.testMain(
      "build",
      "-p", "docker",
      "-o", tempFolStr,
      configFile,
    )

    assert(executable.exists)
    assert(executable.canExecute)
  }

  test("Check whether the executable can build the image", DockerTest) {
    val out = Exec.runCatch(
      Seq(executable.toString, "---setup", "build")
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

    val stripAll = (s : String) => s.replaceAll(raw"\s+", " ").trim

    functionality.allArguments.foreach(arg => {
      for (opt <- arg.alternatives; value <- opt)
        assert(stdout.contains(value))
      for (description <- arg.description) {
        assert(stripAll(stdout).contains(stripAll(description)))
      }
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
        "d", "e", "f",
        "---n_proc", "2",
        "---memory", "1gb"
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
      val regex = s"""meta_resources_dir: \\|/viash_automount.*$tempFolStr/\\|""".r
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
    val regex = s"""meta_resources_dir: \\|/viash_automount.*$tempFolStr/\\|""".r
    assert(regex.findFirstIn(stdout).isDefined)

    assert(stdout.contains("INFO: Parsed input arguments"))
  }

  test("viash build with trailing arguments") {
    TestHelper.testMain(
      "build",
      configFile,
      "-p", "docker",
      "-o", tempFolStr,
    )

    assert(executable.exists)
    assert(executable.canExecute)
  }

  test("Specify platform (docker) in config yaml", DockerTest) {
    val testText = TestHelper.testMain(
      "build",
      "-o", tempFolStr,
      "-m",
      configPlatformFile,
    )

    assert(executable.exists)
    assert(executable.canExecute)

    val out = Exec.runCatch(
      Seq(executable.toString, "---setup", "build")
    )
    assert(out.exitValue == 0)

    val regexPlatform = "platform:\\s*<NA>".r
    assert(regexPlatform.findFirstIn(testText).isDefined, testText)
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
      val stdout = TestHelper.testMain(
        "build",
        "-p", "docker",
        "-o", tempFolStr,
        "-m",
        configMetaFile
      )

      assert(executable.exists)
      assert(executable.canExecute)

      val viashVersion = io.viash.Main.version

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

      val configMetaFile = Paths.get(tempMetaFolStr, "config.vsh.yaml").toString

      // Run the code
      // prepare the environment
      val stdout = TestHelper.testMain(
        "build",
        "-p", "docker",
        "-o", tempFolStr,
        "-m",
        configMetaFile
      )

      assert(executable.exists)
      assert(executable.canExecute)

      val viashVersion = io.viash.Main.version

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
        Exec.runCatchPath(
          List("git", "init"),
          cwd = Some(tempMetaFolder)
        ).exitValue == 0
      , "git init")

      val configMetaFile = Paths.get(tempMetaFolStr, "config.vsh.yaml").toString

      // Run the code
      // prepare the environment
      val stdout = TestHelper.testMain(
        "build",
        "-p", "docker",
        "-o", tempFolStr,
        "-m",
        configMetaFile
      )

      assert(executable.exists)
      assert(executable.canExecute)

      val viashVersion = io.viash.Main.version

      val regexViashVersion = s"viash version:\\s*$viashVersion".r
      val regexConfig = s"config:\\s*$configMetaFile".r
      val regexPlatform = "platform:\\s*docker".r
      val regexExecutable = s"executable:\\s*$tempFolStr/testbash".r
      val regexOutput = s"output:\\s*$tempFolStr".r
      val regexRemoteGitRepo = "remote git repo:\\s*<NA>".r

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
  //<editor-fold desc="Test benches to check building with or without --setup flag">
  test("viash without --setup doesn't create docker during build", DockerTest) {
    //remove docker if it exists
    removeDockerImage("throwawayimage", "0.1")
    assert(!checkDockerImageExists("throwawayimage", "0.1"))

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
    assert(!checkDockerImageExists("throwawayimage", "0.1"))

    // run viash wrapper with ---setup
    val out = Exec.runCatch(
      Seq(executable.toString, "---setup", "build")
    )
    assert(out.exitValue == 0)

    // verify docker now exists
    assert(checkDockerImageExists("throwawayimage", "0.1"))
  }

  test("viash with --setup creates docker during build", DockerTest) {
    // remove docker if it exists
    removeDockerImage("throwawayimage", "0.1")
    assert(!checkDockerImageExists("throwawayimage", "0.1"))

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
    assert(checkDockerImageExists("throwawayimage", "0.1"))
  }
  //</editor-fold>

  test("Get info of a docker image using docker inspect", DockerTest) {
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

      val configMetaFile = Paths.get(tempMetaFolStr, "config.vsh.yaml").toString

      // Run the code
      // prepare the environment
      val stdout = TestHelper.testMain(
        "build",
        "-p", "docker",
        "-o", tempFolStr,
        "--setup", "alwaysbuild",
        configMetaFile
      )

      assert(executable.exists)
      assert(executable.canExecute)

      val inspectOut = Exec.run(
        Seq("docker", "inspect", s"${functionality.name}:${functionality.version.get}")
      )

      val regexOciAuthors = raw""""org.opencontainers.image.authors": "Bob Cando <bob@cando.com> \(maintainer, author\) \{github: bobcando, orcid: XXXAAABBB\}"""".r
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

  test("Prepare base config derivation and verify", DockerTest) {
    val rootPath = getClass.getResource(s"/testbash/").getPath
    TestHelper.copyFolder(rootPath, temporaryConfigFolder.toString)

    val newConfigFilePath = configDeriver.derive(
      Nil,
      "commands_default"
    )
    
    val stdout = TestHelper.testMain(
      "build",
      "-p", "docker",
      "-o", tempFolStr,
      newConfigFilePath,
      "--setup", "alwaysbuild"
    )

    assert(stdout.matches("\\[notice\\] Building container 'testbash:0\\.1' with Dockerfile\\s*"), stdout)
  }

  test("Verify adding extra commands to verify", DockerTest) {
    val newConfigFilePath = configDeriver.derive(
      """.functionality.requirements := { commands: ["which", "bash", "ps", "grep"] }""",
      "commands_extra"
    )
    
    val stdout = TestHelper.testMain(
      "build",
      "-p", "docker",
      "-o", tempFolStr,
      newConfigFilePath,
      "--setup", "alwaysbuild"
    )

    assert(stdout.matches("\\[notice\\] Building container 'testbash:0\\.1' with Dockerfile\\s*"), stdout)
  }

  test("Verify base adding an extra required command that doesn't exist", DockerTest) {
    val newConfigFilePath = configDeriver.derive(
      """.functionality.requirements := { commands: ["which", "bash", "ps", "grep", "non_existing_command"] }""",
      "non_existing_command"
    )
    
    val stdout = TestHelper.testMain(
      "build",
      "-p", "docker",
      "-o", tempFolStr,
      newConfigFilePath,
      "--setup", "alwaysbuild"
    )

    assert(stdout.contains("[notice] Building container 'testbash:0.1' with Dockerfile"))
    assert(stdout.contains("[error] Docker container 'testbash:0.1' does not contain command 'non_existing_command'."))
  }

  test("Check deprecated warning", DockerTest) {
    val newConfigFilePath = configDeriver.derive(""".functionality.status := "deprecated"""", "deprecated")
    
    val (stdout, stderr) = TestHelper.testMainWithStdErr(
      "build",
      "-p", "docker",
      "-o", tempFolStr,
      newConfigFilePath,
      "--setup", "alwaysbuild"
    )

    assert(stderr.contains("The status of the component 'testbash' is set to deprecated."))
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