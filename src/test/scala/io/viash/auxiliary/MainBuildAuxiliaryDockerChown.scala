package io.viash.auxiliary

import io.viash.{DockerTest, TestHelper}
import io.viash.helpers.{IO, Exec, Logger}
import io.viash.config.Config
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite

import java.nio.file.{Files, Paths, StandardCopyOption}
import scala.io.Source
import io.viash.ConfigDeriver
import org.scalatest.ParallelTestExecution
import io.viash.exceptions.ConfigParserException

class MainBuildAuxiliaryDockerChown extends AnyFunSuite with BeforeAndAfterAll with ParallelTestExecution {
  Logger.UseColorOverride.value = Some(false)
  private val temporaryFolder = IO.makeTemp("viash_tester")
  private val tempFolStr = temporaryFolder.toString
  private val temporaryConfigFolder = IO.makeTemp(s"viash_${this.getClass.getName}_")

  private val configFile = getClass.getResource("/testbash/config.vsh.yaml").getPath
  private val configDeriver = ConfigDeriver(Paths.get(configFile), temporaryConfigFolder)

  val singleOutputmods = List(
    """.resources[.type == "bash_script"].path := "docker_options/code.sh"""",
  )

  val twoOutputsmods = List(
    """.argument_groups[.name == "Arguments"].arguments += {name: "--output2", type: "file", direction: "output"}""",
    """.resources[.type == "bash_script"].path := "docker_options/code_two_output.sh"""",
  )

  val multipleOutputsMods = List(
    """.argument_groups[.name == "Arguments"].arguments[.name == "--output"].required := false""",
    """.resources[.type == "bash_script"].path := "docker_options/code_multiple_output.sh"""",
  )

  // rootless podman, used through a `docker` symlink on the PATH
  private lazy val podmanShimDir: Option[String] = {
    val rootless = Exec.runCatch(Seq("podman", "info", "--format", "{{.Host.Security.Rootless}}"))
    if (rootless.exitValue == 0 && rootless.output.trim == "true") {
      val podmanPath = Exec.run(Seq("sh", "-c", "command -v podman")).trim
      val shimDir = temporaryFolder.resolve("podman_shim")
      Files.createDirectories(shimDir)
      Files.createSymbolicLink(shimDir.resolve("docker"), Paths.get(podmanPath))
      Some(shimDir.toString)
    } else {
      None
    }
  }

  private lazy val dockerIsRootless: Boolean =
    Exec.runCatch(Seq("docker", "info", "--format", "{{.SecurityOptions}}")).output.contains("rootless")

  def dockerChownGetOwner(
    mods: List[String],
    amount: Int,
    dockerId: String,
    extraEnv: Seq[(String, String)] = Nil,
    usePodman: Boolean = false
  ): List[String] = {
    assert(amount > 0)
    assert(amount < 4)

    val engineMod = s""".engines := [{"type": "docker", "image": "docker.io/library/bash:3.2", "id": "$dockerId", "setup": [{"type": "apk", "packages": ["jq"]}]}]"""
    val modsWithEngine = mods :+ engineMod
    val localConfigFile = configDeriver.derive(modsWithEngine, dockerId)
    val localConfig = Config.read(localConfigFile)
    val localExecutable = Paths.get(tempFolStr, localConfig.name).toFile

    // the executable builds the image itself when running with podman
    val setupArgs = if (usePodman) Nil else Seq("--setup", "build")
    val runEnv = if (usePodman) {
      extraEnv :+ ("PATH" -> s"${podmanShimDir.get}:${sys.env("PATH")}")
    } else {
      extraEnv
    }

    // prepare the environment
    TestHelper.testMain(
      Seq(
        "build",
        "--engine", dockerId,
        "-o", tempFolStr
      ) ++ setupArgs ++ Seq(localConfigFile)*
    )

    assert(localExecutable.exists)
    assert(localExecutable.canExecute)

    // run the script
    val pattern = f"$tempFolStr/output_${dockerId}_*.txt"

    val outputParams = amount match {
      case 1 => Seq("--output", pattern.replace("*", "1"))
      case 2 => Seq("--output", pattern.replace("*", "1"), "--output2", pattern.replace("*", "2"))
      case 3 => Seq("--multiple_output", pattern)
    }

    Exec.run(
      Seq(
        localExecutable.toString,
        localExecutable.toString,
        "--real_number", "10.5",
        "--whole_number=10",
        "-s", "a string with a few spaces",
      ) ++ outputParams,
      extraEnv = runEnv
    )

    // find files based on the pattern
    import scala.jdk.CollectionConverters._
    val foundFiles = Files.list(temporaryFolder).iterator().asScala.toList
    val outputFiles = foundFiles.filter(_.toString.matches(pattern.replace("*", ".*")))
    assert(outputFiles.length == amount, s"Expected $amount files, got ${outputFiles.length}.\nDirectory contents: ${foundFiles.mkString(", ")}")
    outputFiles.map(file => Files.getOwner(file).toString)
  }

  test("Test behaviour with one output file", DockerTest) {
    val owners = dockerChownGetOwner(singleOutputmods, 1, "chown_default")
    assert(owners.length == 1)
    owners.foreach(owner => {
      assert(owner.nonEmpty)
      assert(owner != "root")
    })
  }

  test("Test behaviour with two output files", DockerTest) {
    val owners = dockerChownGetOwner(twoOutputsmods, 2, "two_chown_default")
    assert(owners.length == 2)
    owners.foreach(owner => {
      assert(owner.nonEmpty)
      assert(owner != "root")
    })
  }

  test("Test behaviour with multiple output files", DockerTest) {
    val owners = dockerChownGetOwner(multipleOutputsMods, 3, "multiple_chown_default")
    assert(owners.length == 3)
    owners.foreach(owner => {
      assert(owner.nonEmpty)
      assert(owner != "root")
    })
  }

  test("Test that chown can be disabled with VIASH_DOCKER_CHOWN=false", DockerTest) {
    assume(!dockerIsRootless, "Docker is running in rootless mode, output files are never owned by root")
    val owners = dockerChownGetOwner(singleOutputmods, 1, "chown_disabled", extraEnv = Seq("VIASH_DOCKER_CHOWN" -> "false"))
    assert(owners == List("root"))
  }

  private val currentUser = System.getProperty("user.name")

  test("Test ownership with rootless podman and one output file", DockerTest) {
    assume(podmanShimDir.isDefined, "Rootless podman is not available")
    val owners = dockerChownGetOwner(singleOutputmods, 1, "podman_chown", usePodman = true)
    assert(owners == List(currentUser))
  }

  test("Test ownership with rootless podman and multiple output files", DockerTest) {
    assume(podmanShimDir.isDefined, "Rootless podman is not available")
    val owners = dockerChownGetOwner(multipleOutputsMods, 3, "podman_multiple_chown", usePodman = true)
    assert(owners == List.fill(3)(currentUser))
  }

  override def afterAll(): Unit = {
    IO.deleteRecursively(temporaryFolder)
    IO.deleteRecursively(temporaryConfigFolder)
  }
}
