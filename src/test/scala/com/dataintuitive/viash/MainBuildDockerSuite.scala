package com.dataintuitive.viash

import org.scalatest.{BeforeAndAfterAll, FunSuite}
import java.nio.file.{Files, Paths, StandardCopyOption}

import com.dataintuitive.viash.config.Config

import scala.io.Source
import com.dataintuitive.viash.helpers._

class MainBuildDockerSuite extends FunSuite with BeforeAndAfterAll {
  // which platform to test
  private val configFile = getClass.getResource(s"/testbash/config.vsh.yaml").getPath

  private val configPlatformFile = getClass.getResource(s"/testbash/config_platform_docker.vsh.yaml").getPath

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

  private val configRequirementsFile = getClass.getResource(s"/testbash/config_requirements.vsh.yaml").getPath
  private val functionalityRequirements = Config.read(configRequirementsFile, modifyFun = false).functionality
  private val executableRequirementsFile = Paths.get(tempFolStr, functionalityRequirements.name).toFile

  private val configResourcesCopyFile = getClass.getResource("/testbash/config_resource_test.vsh.yaml").getPath
  private val configResourcesUnsupportedProtocolFile = getClass.getResource("/testbash/config_resource_unsupported_protocol.vsh.yaml").getPath

  private val configDockerOptionsChownFile = getClass.getResource("/testbash/docker_options/config_chown.vsh.yaml").getPath
  private val configDockerOptionsChownTwoOutputFile = getClass.getResource("/testbash/docker_options/config_chown_two_output.vsh.yaml").getPath
  private val configDockerOptionsChownMultipleOutputFile = getClass.getResource("/testbash/docker_options/config_chown_multiple_output.vsh.yaml").getPath


  //<editor-fold desc="Test benches to build a generic script and run various commands to see if the functionality is correct">
  // convert testbash
  test("viash can create an executable") {
    TestHelper.testMain(Array(
      "build",
      "-p", "docker",
      "-o", tempFolStr,
      configFile,
    ))

    assert(executable.exists)
    assert(executable.canExecute)
  }

  test("Check whether the executable can build the image", DockerTest) {
    val out = Exec.run2(
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

  test("viash build with trailing arguments") {
    TestHelper.testMain(Array(
      "build",
      configFile,
      "-p", "docker",
      "-o", tempFolStr,
    ))

    assert(executable.exists)
    assert(executable.canExecute)
  }

  test("Specify platform (docker) in config yaml", DockerTest) {
    val testText = TestHelper.testMain(Array(
      "build",
      "-o", tempFolStr,
      "-m",
      configPlatformFile,
    ))

    assert(executable.exists)
    assert(executable.canExecute)

    val out = Exec.run2(
      Seq(executable.toString, "---setup", "build")
    )
    assert(out.exitValue == 0)

    val regexPlatform = "platform:\\s*<NA>".r
    assert(regexPlatform.findFirstIn(testText).isDefined, testText)
  }
  //</editor-fold>
  //<editor-fold desc="Test benches to check building with tags, versions & registries">
  test("Get tagged version of a docker image for bash 5.0", DockerTest) {
    // prepare the environment
    TestHelper.testMain(Array(
      "build",
      "-p", "testtag1",
      "-o", tempFolStr,
      configBashTagFile
    ))

    assert(executableBashTagFile.exists)
    assert(executableBashTagFile.canExecute)

    // create the docker image
    val out = Exec.run2(
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
    //
    // RUN :
    // Allow for extra spaces just in case the format changes slightly format-wise but without functional differences
    val regex = """^FROM bash:5\.0[\r\n\s]*RUN\s+:\s*$""".r
    assert(regex.findFirstIn(dockerout).isDefined, regex.toString)
  }

  test("Get tagged version of a docker image for bash 3.2", DockerTest) {
    // prepare the environment
    TestHelper.testMain(Array(
      "build",
      "-p", "testtag2",
      "-o", tempFolStr,
      configBashTagFile
    ))

    assert(executableBashTagFile.exists)
    assert(executableBashTagFile.canExecute)

    // create the docker image
    val out = Exec.run2(
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
    //
    // RUN :
    // Allow for extra spaces just in case the format changes slightly format-wise but without functional differences
    val regex = """^FROM bash:3\.2[\r\n\s]*RUN\s+:\s*$""".r
    assert(regex.findFirstIn(dockerout).isDefined, regex.toString)
  }

  test("Check whether target image name is well formed without target_image, version or registry", DockerTest) {
    // prepare the environment
    TestHelper.testMain(Array(
      "build",
      "-p", "testtargetimage1",
      "-o", tempFolStr,
      configBashTagFile
    ))

    assert(executableBashTagFile.exists)
    assert(executableBashTagFile.canExecute)

    // create the docker image
    val out = Exec.run2(
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
    assert(content.exists(_.matches("cat << VIASHEOF \\| eval docker run .* testbash:latest")))
  }

  test("Check whether target image name is well formed with target_image, version, and registry", DockerTest) {
    // prepare the environment
    TestHelper.testMain(Array(
      "build",
      "-p", "testtargetimage2",
      "-o", tempFolStr,
      configBashTagFile
    ))

    assert(executableBashTagFile.exists)
    assert(executableBashTagFile.canExecute)

    // create the docker image
    val out = Exec.run2(
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
    TestHelper.testMain(Array(
      "build",
      "-p", "testtargetimage3",
      "-o", tempFolStr,
      configBashTagFile
    ))

    assert(executableBashTagFile.exists)
    assert(executableBashTagFile.canExecute)

    // create the docker image
    val out = Exec.run2(
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
        "build",
        "-p", "docker",
        "-o", tempFolStr,
        "-m",
        configMetaFile
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
        "build",
        "-p", "docker",
        "-o", tempFolStr,
        "-m",
        configMetaFile
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
        "build",
        "-p", "docker",
        "-o", tempFolStr,
        "-m",
        configMetaFile
      ))

      assert(executableBashTagFile.exists)
      assert(executableBashTagFile.canExecute)

      val viashVersion = com.dataintuitive.viash.Main.version

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
    removeDockerImage("busybox")
    assert(!checkDockerImageExists("busybox"))

    // build viash wrapper without --setup
    TestHelper.testMain(Array(
      "build",
      "-p", "busybox",
      "-o", tempFolStr,
      configFile
    ))

    assert(executable.exists)
    assert(executable.canExecute)

    // verify docker still doesn't exist
    assert(!checkDockerImageExists("busybox"))

    // run viash wrapper with ---setup
    val out = Exec.run2(
      Seq(executable.toString, "---setup", "build")
    )
    assert(out.exitValue == 0)

    // verify docker now exists
    assert(checkDockerImageExists("busybox"))
  }

  test("viash with --setup creates docker during build", DockerTest) {
    // remove docker if it exists
    removeDockerImage("busybox")
    assert(!checkDockerImageExists("busybox"))

    // build viash wrapper with --setup
    TestHelper.testMain(Array(
      "build",
      "-p", "busybox",
      "-o", tempFolStr,
      "--setup", "build",
      configFile
    ))

    assert(executable.exists)
    assert(executable.canExecute)

    // verify docker exists
    assert(checkDockerImageExists("busybox"))
  }
  //</editor-fold>
  //<editor-fold desc="Test benches to check additional installation of required packages">
  test("check base image for apk still does not contain the fortune package", DockerTest) {
    TestHelper.testMain(Array(
      "build",
      "-p", "viash_requirement_apk_base",
      "-o", tempFolStr,
      "--setup", "build",
      configRequirementsFile
    ))

    assert(executableRequirementsFile.exists)
    assert(executableRequirementsFile.canExecute)

    val output = Exec.run2(
      Seq(
        executable.toString,
        "--which", "fortune"
      )
    )

    assert(output.output == "")
  }

  test("check docker requirements using apk to add the fortune package", DockerTest) {
    // remove docker if it exists
    removeDockerImage("viash_requirement_apk")
    assert(!checkDockerImageExists("viash_requirement_apk"))

    // build viash wrapper with --setup
    TestHelper.testMain(Array(
      "build",
      "-p", "viash_requirement_apk",
      "-o", tempFolStr,
      "--setup", "build",
      configRequirementsFile
    ))

    // verify docker exists
    assert(checkDockerImageExists("viash_requirement_apk"))

    assert(executableRequirementsFile.exists)
    assert(executableRequirementsFile.canExecute)

    val output = Exec.run2(
      Seq(
        executable.toString,
        "--which", "fortune"
      )
    )

    assert(output.output == "/usr/bin/fortune\n")

    // Tests finished, remove docker image
    removeDockerImage("viash_requirement_apk")
  }

  test("check base image for apt still does not contain the cowsay package", DockerTest) {
    TestHelper.testMain(Array(
      "build",
      "-p", "viash_requirement_apt_base",
      "-o", tempFolStr,
      "--setup", "build",
      configRequirementsFile
    ))

    assert(executableRequirementsFile.exists)
    assert(executableRequirementsFile.canExecute)

    val output = Exec.run2(
      Seq(
        executableRequirementsFile.toString,
        "--which", "cowsay"
      )
    )

    assert(output.output == "")
  }

  test("check docker requirements using apt to add the cowsay package", DockerTest) {
    // remove docker if it exists
    removeDockerImage("viash_requirement_apt")
    assert(!checkDockerImageExists("viash_requirement_apt"))

    // build viash wrapper with --setup
    val _ = TestHelper.testMain(Array(
      "build",
      "-p", "viash_requirement_apt",
      "-o", tempFolStr,
      "--setup", "build",
      configRequirementsFile
    ))

    // verify docker exists
    assert(checkDockerImageExists("viash_requirement_apt"))

    assert(executableRequirementsFile.exists)
    assert(executableRequirementsFile.canExecute)

    val output = Exec.run2(
      Seq(
        executable.toString,
        "--file", "/usr/games/cowsay"
      )
    )

    assert(output.output == "/usr/games/cowsay exists.\n")

    // Tests finished, remove docker image
    removeDockerImage("viash_requirement_apt")
  }

  //</editor-fold>
  //<editor-fold desc="Verify correct copying of resources">
  test("Check resources are copied from and to the correct location") {

    // copy some resources to /tmp/viash_tmp_resources/ so we can test absolute path resources
    val tmpFolderResourceSourceFile = Paths.get(getClass.getResource("/testbash/resource3.txt").getFile)

    val tmpFolderResourceDestinationFolder = Paths.get("/tmp/viash_tmp_resources/").toFile
    val tmpFolderResourceDestinationFile = Paths.get(tmpFolderResourceDestinationFolder.getPath, "resource3.txt")

    if (!tmpFolderResourceDestinationFolder.exists())
      tmpFolderResourceDestinationFolder.mkdir()

    Files.copy(tmpFolderResourceSourceFile, tmpFolderResourceDestinationFile, StandardCopyOption.REPLACE_EXISTING)

    // generate viash script
    TestHelper.testMain(
      Array(
        "build",
        "-p", "docker",
        "-o", tempFolStr,
        configResourcesCopyFile
      ))

    assert(executable.exists)
    assert(executable.canExecute)

    // List all expected resources and their md5sum
    val expectedResources = List(
      //("check_bash_version.sh", "0c3c134d4ff0ea3a4a3b32e09fb7c100"),
      ("code.sh", "efa9e1aa1c5f2a0b91f558ead5917c68"),
      ("NOTICE", "d64d250d1c3a5af25977651b5443aedb"),
      ("resource1.txt", "bc9171172c4723589a247f99b838732d"),
      ("resource2.txt", "9cd530447200979dbf9e117915cbcc74"),
      ("resource_folder/resource_L1_1.txt", "51954bf10062451e683121e58d858417"),
      ("resource_folder/resource_L1_2.txt", "b43991c0ef5d15710faf976e02cbb206"),
      ("resource_folder/resource_L2/resource_L2_1.txt", "63165187f791a8dfff628ef8090e56ff"),
      ("target_folder/relocated_file_1.txt", "bc9171172c4723589a247f99b838732d"),
      ("target_folder/relocated_file_2.txt", "51954bf10062451e683121e58d858417"),
      ("target_folder/relocated_file_3.txt", "6b0e05ae3d38b7db48ebdfc564366bce"),
      ("resource3.txt", "aa2037b3d308bcb6a78a3d4fbf04b297"),
      ("target_folder/relocated_file_4.txt", "aa2037b3d308bcb6a78a3d4fbf04b297")
    )

    // Check all resources can be found in the folder
    for ((name, md5sum) <- expectedResources) {
      val resourceFile = Paths.get(tempFolStr, name).toFile

      assert(resourceFile.exists, s"Could not find $name")

      val hash = TestHelper.computeHash(resourceFile.getPath)
      assert(hash == md5sum, s"Calculated md5sum doesn't match the given md5sum for $name")
    }
  }

  test("Check resources with unsupported format") {
    // generate viash script
    val testOutput = TestHelper.testMainException2[RuntimeException](
      Array(
        "build",
        "-p", "docker",
        "-o", tempFolStr,
        configResourcesUnsupportedProtocolFile
      ))

    assert(testOutput.exceptionText == "Unsupported scheme: ftp")
  }
  //</editor-fold>
  //<editor-fold desc="Test docker options chown, port and workdir">
  def docker_chown_get_owner(dockerId: String): String = {
    val localConfig = configDockerOptionsChownFile
    val localFunctionality = Config.read(localConfig, modifyFun = false).functionality
    val localExecutable = Paths.get(tempFolStr, localFunctionality.name).toFile

    // prepare the environment
    TestHelper.testMain(Array(
      "build",
      "-p", dockerId,
      "-o", tempFolStr,
      "--setup", "build",
      localConfig
    ))

    assert(localExecutable.exists)
    assert(localExecutable.canExecute)

    // run the script
    val output = Paths.get(tempFolStr, s"output_" + dockerId + ".txt").toFile

    Exec.run(
      Seq(
        localExecutable.toString,
        localExecutable.toString,
        "--real_number", "10.5",
        "--whole_number=10",
        "-s", "a string with a few spaces",
        "--output", output.getPath
      )
    )

    assert(output.exists())

    val owner = Files.getOwner(output.toPath)
    owner.toString
  }

  def docker_chown_get_owner_two_outputs(dockerId: String): (String,String) = {
    val localConfig = configDockerOptionsChownTwoOutputFile
    val localFunctionality = Config.read(localConfig, modifyFun = false).functionality
    val localExecutable = Paths.get(tempFolStr, localFunctionality.name).toFile

    // prepare the environment
    TestHelper.testMain(Array(
      "build",
      "-p", dockerId,
      "-o", tempFolStr,
      "--setup", "build",
      localConfig
    ))

    assert(localExecutable.exists)
    assert(localExecutable.canExecute)

    // run the script
    val output = Paths.get(tempFolStr, "output_" + dockerId + ".txt").toFile
    val output2 = Paths.get(tempFolStr, "output_" + dockerId +"_2.txt").toFile

    val _ = Exec.run(
      Seq(
        localExecutable.toString,
        localExecutable.toString,
        "--real_number", "10.5",
        "--whole_number=10",
        "-s", "a string with a few spaces",
        "--output", output.getPath,
        "--output2", output2.getPath
      )
    )

    assert(output.exists())
    assert(output2.exists())

    val owner = Files.getOwner(output.toPath)
    val owner2 = Files.getOwner(output2.toPath)
    (owner.toString, owner2.toString)
  }

  def docker_chown_get_owner_multiple_outputs(dockerId: String): (String,String,String) = {
    val localConfig = configDockerOptionsChownMultipleOutputFile
    val localFunctionality = Config.read(localConfig, modifyFun = false).functionality
    val localExecutable = Paths.get(tempFolStr, localFunctionality.name).toFile

    // prepare the environment
    TestHelper.testMain(Array(
      "build",
      "-p", dockerId,
      "-o", tempFolStr,
      "--setup", "build",
      localConfig
    ))

    assert(localExecutable.exists)
    assert(localExecutable.canExecute)

    // run the script
    val output = Paths.get(tempFolStr, "output_" + dockerId + ".txt").toFile
    val output2 = Paths.get(tempFolStr, "output_" + dockerId +"_2.txt").toFile
    val output3 = Paths.get(tempFolStr, "output_" + dockerId +"_3.txt").toFile

    Exec.run(
      Seq(
        localExecutable.toString,
        localExecutable.toString,
        "--real_number", "10.5",
        "--whole_number=10",
        "-s", "a string with a few spaces",
        "--output", output.getPath, output2.getPath, output3.getPath
      )
    )

    assert(output.exists())
    assert(output2.exists())
    assert(output3.exists())

    val owner = Files.getOwner(output.toPath)
    val owner2 = Files.getOwner(output2.toPath)
    val owner3 = Files.getOwner(output3.toPath)
    (owner.toString, owner2.toString, owner3.toString)
  }

  test("Test default behaviour when chown is not specified", DockerTest) {
    val owner = docker_chown_get_owner("chown_default")
    assert(owner.nonEmpty)
    assert(owner != "root")
  }

  test("Test default behaviour when chown is set to true", DockerTest) {
    val owner = docker_chown_get_owner("chown_true")
    assert(owner.nonEmpty)
    assert(owner != "root")
  }

  test("Test default behaviour when chown is set to false", DockerTest) {
    val owner = docker_chown_get_owner("chown_false")
    assert(owner == "root")
  }

  test("Test default behaviour when chown is not specified with two output files", DockerTest) {
    val owner = docker_chown_get_owner_two_outputs("two_chown_default")
    assert(owner._1.nonEmpty)
    assert(owner._2.nonEmpty)
    assert(owner._1 != "root")
    assert(owner._2 != "root")
  }

  test("Test default behaviour when chown is set to true with two output files", DockerTest) {
    val owner = docker_chown_get_owner_two_outputs("two_chown_true")
    assert(owner._1.nonEmpty)
    assert(owner._2.nonEmpty)
    assert(owner._1 != "root")
    assert(owner._2 != "root")
  }

  test("Test default behaviour when chown is set to false with two output files", DockerTest) {
    val owner = docker_chown_get_owner_two_outputs("two_chown_false")
    assert(owner._1 == "root")
    assert(owner._2 == "root")
  }

  test("Test default behaviour when chown is not specified with multiple output files", DockerTest) {
    val owner = docker_chown_get_owner_multiple_outputs("multiple_chown_default")
    assert(owner._1.nonEmpty)
    assert(owner._2.nonEmpty)
    assert(owner._3.nonEmpty)
    assert(owner._1 != "root")
    assert(owner._2 != "root")
    assert(owner._3 != "root")
  }

  test("Test default behaviour when chown is set to true with multiple output files", DockerTest) {
    val owner = docker_chown_get_owner_multiple_outputs("multiple_chown_true")
    assert(owner._1.nonEmpty)
    assert(owner._2.nonEmpty)
    assert(owner._3.nonEmpty)
    assert(owner._1 != "root")
    assert(owner._2 != "root")
    assert(owner._3 != "root")
  }

  test("Test default behaviour when chown is set to false with multiple output files", DockerTest) {
    val owner = docker_chown_get_owner_multiple_outputs("multiple_chown_false")
    assert(owner._1 == "root")
    assert(owner._2 == "root")
    assert(owner._3 == "root")
  }
  //</editor-fold>

  def checkDockerImageExists(name: String): Boolean = {
    val out = Exec.run2(
      Seq("docker", "images", name)
    )

    // print(out)
    val regex = s"$name\\s*latest".r

    regex.findFirstIn(out.output).isDefined
  }

  def removeDockerImage(name: String): Unit = {
    Exec.run2(
      Seq("docker", "rmi", name, "-f")
    )
  }

  override def afterAll() {
    IO.deleteRecursively(temporaryFolder)
  }
}