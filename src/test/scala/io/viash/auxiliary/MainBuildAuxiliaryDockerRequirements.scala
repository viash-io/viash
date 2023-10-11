package io.viash.auxiliary

import io.viash.config.Config
import io.viash.helpers.{IO, Exec, Logger}
import io.viash.{DockerTest, TestHelper}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.FixtureAnyFunSuite

import java.nio.file.{Files, Paths}
import scala.io.Source
import io.viash.ConfigDeriver

abstract class AbstractMainBuildAuxiliaryDockerRequirements extends FixtureAnyFunSuite with BeforeAndAfterAll {
  Logger.UseColorOverride.value = Some(false)
  private val temporaryFolder = IO.makeTemp("viash_tester")
  protected val tempFolStr = temporaryFolder.toString
  private val temporaryConfigFolder = IO.makeTemp("viash_tester_configs")

  private val configRequirementsFile = getClass.getResource(s"/testbash/auxiliary_requirements/config_requirements.vsh.yaml").getPath
  private val functionalityRequirements = Config.read(configRequirementsFile).functionality
  protected val executableRequirementsFile = Paths.get(tempFolStr, functionalityRequirements.name).toFile

  protected val configDeriver = ConfigDeriver(Paths.get(configRequirementsFile), temporaryConfigFolder)

  protected val image = "bash:3.2"
  protected val dockerTag = "viash_requirements_testbench"

  case class FixtureParam()

  // Fixture will remove the docker image before starting and remove it again after finishing
  def withFixture(test: OneArgTest) = {
    // remove docker if it exists
    removeDockerImage(dockerTag)
    assert(!checkDockerImageExists(dockerTag))

    val theFixture = FixtureParam()

    val outcome = withFixture(test.toNoArgTest(theFixture)) // "loan" the fixture to the test

    // Tests finished, remove docker image
    removeDockerImage(dockerTag)

    outcome
  }

  def checkDockerImageExists(name: String): Boolean = {
    val out = Exec.runCatch(
      Seq("docker", "images", name)
    )
    val regex = s"$name\\s*latest".r
    regex.findFirstIn(out.output).isDefined
  }

  def removeDockerImage(name: String): Unit = {
    Exec.runCatch(
      Seq("docker", "rmi", name, "-f")
    )
  }

  def deriveEngineConfig(setup: Option[String], test_setup: Option[String], name: String): String = {
    val setupStr = setup.map(s => s""", "setup": $s""").getOrElse("")
    val testSetupStr = test_setup.map(s => s""", "test_setup": $s""").getOrElse("")

    configDeriver.derive(
      s""".engines := [{ "type": "docker", "image": "$image", "target_image": "$dockerTag" $setupStr $testSetupStr }]""",
      name
    )
  }

  override def afterAll(): Unit = {
    IO.deleteRecursively(temporaryFolder)
    IO.deleteRecursively(temporaryConfigFolder)
  }
}

class MainBuildAuxiliaryDockerRequirementsApk extends AbstractMainBuildAuxiliaryDockerRequirements {
  override val dockerTag = "viash_requirements_testbench_apk"
  override val image = "bash:3.2"

  test("setup; check base image for apk still does not contain the fortune package", DockerTest) { f =>
    val newConfigFilePath = deriveEngineConfig(None, None, "apk_base")

    TestHelper.testMain(
      "build",
      "--engine", "docker",
      // "--runner", "docker",
      "-o", tempFolStr,
      "--setup", "build",
      newConfigFilePath
    )

    assert(checkDockerImageExists(dockerTag))
    assert(executableRequirementsFile.exists)
    assert(executableRequirementsFile.canExecute)

    val output = Exec.runCatch(
      Seq(
        executableRequirementsFile.toString,
        "--which", "fortune"
      )
    )

    assert(output.output == "")
  }

  test("setup; check docker requirements using apk to add the fortune package", DockerTest) { f =>
    val newConfigFilePath = deriveEngineConfig(Some("""[{ "type": "apk", "packages": ["fortune"] }]"""), None, "apk_fortune")

    TestHelper.testMain(
      "build",
      "-o", tempFolStr,
      "--setup", "build",
      newConfigFilePath
    )

    assert(checkDockerImageExists(dockerTag))
    assert(executableRequirementsFile.exists)
    assert(executableRequirementsFile.canExecute)

    val output = Exec.runCatch(
      Seq(
        executableRequirementsFile.toString,
        "--which", "fortune"
      )
    )

    assert(output.output == "/usr/bin/fortune\n")
  }

  test("setup; check docker requirements using apk but with an empty list", DockerTest) { f =>
    val newConfigFilePath = deriveEngineConfig(Some("""[{ "type": "apk", "packages": [] }]"""), None, "apk_empty")

    TestHelper.testMain(
      "build",
      "-o", tempFolStr,
      "--setup", "build",
      newConfigFilePath
    )

    assert(checkDockerImageExists(dockerTag))
    assert(executableRequirementsFile.exists)
    assert(executableRequirementsFile.canExecute)

    val output = Exec.runCatch(
      Seq(
        executableRequirementsFile.toString,
        "--which", "fortune"
      )
    )

    assert(output.output == "")
  }
}

class MainBuildAuxiliaryDockerRequirementsApt extends AbstractMainBuildAuxiliaryDockerRequirements {
  override val dockerTag = "viash_requirements_testbench_apt"
  override val image = "debian:bullseye-slim"

  test("setup; check base image for apt still does not contain the cowsay package", DockerTest) { f =>
    val newConfigFilePath = deriveEngineConfig(None, None, "apt_base")

    TestHelper.testMain(
      "build",
      "-o", tempFolStr,
      "--setup", "build",
      newConfigFilePath
    )

    assert(checkDockerImageExists(dockerTag))
    assert(executableRequirementsFile.exists)
    assert(executableRequirementsFile.canExecute)

    val output = Exec.runCatch(
      Seq(
        executableRequirementsFile.toString,
        "--file", "/usr/games/cowsay"
      )
    )

    assert(output.output.contains("/usr/games/cowsay doesn't exist."))
  }

  test("setup; check docker requirements using apt to add the cowsay package", DockerTest) { f =>
    val newConfigFilePath = deriveEngineConfig(Some("""[{ "type": "apt", "packages": ["cowsay"] }]"""), None, "apt_cowsay")

    TestHelper.testMain(
      "build",
      "-o", tempFolStr,
      "--setup", "build",
      newConfigFilePath
    )

    assert(checkDockerImageExists(dockerTag))
    assert(executableRequirementsFile.exists)
    assert(executableRequirementsFile.canExecute)

    val output = Exec.runCatch(
      Seq(
        executableRequirementsFile.toString,
        "--file", "/usr/games/cowsay"
      )
    )

    assert(output.output.contains("/usr/games/cowsay exists."))
  }

  test("setup; check docker requirements using apt but with an empty list", DockerTest) { f =>
    val newConfigFilePath = deriveEngineConfig(Some("""[{ "type": "apt", "packages": [] }]"""), None, "apt_empty")

    TestHelper.testMain(
      "build",
      "-o", tempFolStr,
      "--setup", "build",
      newConfigFilePath
    )

    assert(checkDockerImageExists(dockerTag))
    assert(executableRequirementsFile.exists)
    assert(executableRequirementsFile.canExecute)

    val output = Exec.runCatch(
      Seq(
        executableRequirementsFile.toString,
        "--file", "/usr/games/cowsay"
      )
    )

    assert(output.output.contains("/usr/games/cowsay doesn't exist."))
  }
}

class MainBuildAuxiliaryDockerRequirementsYum extends AbstractMainBuildAuxiliaryDockerRequirements{
  override val dockerTag = "viash_requirements_testbench_yum"
  override val image = "fedora:38"

  test("setup; check base image for yum still does not contain the which package", DockerTest) { f =>
    val newConfigFilePath = deriveEngineConfig(None, None, "yum_base")

    TestHelper.testMain(
      "build",
      "-o", tempFolStr,
      "--setup", "build",
      newConfigFilePath
    )

    assert(checkDockerImageExists(dockerTag))
    assert(executableRequirementsFile.exists)
    assert(executableRequirementsFile.canExecute)

    val output = Exec.runCatch(
      Seq(
        executableRequirementsFile.toString,
        "--file", "/usr/bin/which"
      )
    )

    assert(output.output.contains("/usr/bin/which doesn't exist."))
  }

  test("setup; check docker requirements using yum to add the which package", DockerTest) { f =>
    val newConfigFilePath = deriveEngineConfig(Some("""[{ "type": "yum", "packages": ["which"] }]"""), None, "yum_which")

    TestHelper.testMain(
      "build",
      "-o", tempFolStr,
      "--setup", "build",
      newConfigFilePath
    )

    assert(checkDockerImageExists(dockerTag))
    assert(executableRequirementsFile.exists)
    assert(executableRequirementsFile.canExecute)

    val output = Exec.runCatch(
      Seq(
        executableRequirementsFile.toString,
        "--file", "/usr/bin/which"
      )
    )

    assert(output.output.contains("/usr/bin/which exists."))
  }

  test("setup; check docker requirements using yum but with an empty list", DockerTest) { f =>
    val newConfigFilePath = deriveEngineConfig(Some("""[{ "type": "yum", "packages": [] }]"""), None, "yum_empty")

    TestHelper.testMain(
      "build",
      "-o", tempFolStr,
      "--setup", "build",
      newConfigFilePath
    )

    assert(checkDockerImageExists(dockerTag))
    assert(executableRequirementsFile.exists)
    assert(executableRequirementsFile.canExecute)

    val output = Exec.runCatch(
      Seq(
        executableRequirementsFile.toString,
        "--file", "/usr/bin/which"
      )
    )

    assert(output.output.contains("/usr/bin/which doesn't exist."))
  }
}

class MainBuildAuxiliaryDockerRequirementsRuby extends AbstractMainBuildAuxiliaryDockerRequirements{
  override val dockerTag = "viash_requirements_testbench_ruby"
  override val image = "ruby:slim-bullseye"

  test("setup; check base image for yum still does not contain the which package", DockerTest) { f =>
    val newConfigFilePath = deriveEngineConfig(None, None, "ruby_base")

    TestHelper.testMain(
      "build",
      "-o", tempFolStr,
      "--setup", "build",
      newConfigFilePath
    )

    assert(checkDockerImageExists(dockerTag))
    assert(executableRequirementsFile.exists)
    assert(executableRequirementsFile.canExecute)

    val output = Exec.runCatch(
      Seq(
        executableRequirementsFile.toString,
        "--file", "/usr/local/bundle/gems/tzinfo-2.0.4/lib/tzinfo.rb"
      )
    )

    assert(output.output.contains("/usr/local/bundle/gems/tzinfo-2.0.4/lib/tzinfo.rb doesn't exist."))
  }

  test("setup; check docker requirements using yum to add the tzinfo package", DockerTest) { f =>
    val newConfigFilePath = deriveEngineConfig(Some("""[{ "type": "ruby", "packages": ["tzinfo:2.0.4"] }]"""), None, "ruby_tzinfo")

    TestHelper.testMain(
      "build",
      "-o", tempFolStr,
      "--setup", "build",
      newConfigFilePath
    )

    assert(checkDockerImageExists(dockerTag))
    assert(executableRequirementsFile.exists)
    assert(executableRequirementsFile.canExecute)

    val output = Exec.runCatch(
      Seq(
        executableRequirementsFile.toString,
        "--file", "/usr/local/bundle/gems/tzinfo-2.0.4/lib/tzinfo.rb"
      )
    )

    assert(output.output.contains("/usr/local/bundle/gems/tzinfo-2.0.4/lib/tzinfo.rb exists."))
  }

  test("setup; check docker requirements using yum but with an empty list", DockerTest) { f =>
    val newConfigFilePath = deriveEngineConfig(Some("""[{ "type": "ruby", "packages": [] }]"""), None, "ruby_empty")

    TestHelper.testMain(
      "build",
      "-o", tempFolStr,
      "--setup", "build",
      newConfigFilePath
    )

    assert(checkDockerImageExists(dockerTag))
    assert(executableRequirementsFile.exists)
    assert(executableRequirementsFile.canExecute)

    val output = Exec.runCatch(
      Seq(
        executableRequirementsFile.toString,
        "--file", "/usr/local/bundle/gems/tzinfo-2.0.4/lib/tzinfo.rb"
      )
    )

    assert(output.output.contains("/usr/local/bundle/gems/tzinfo-2.0.4/lib/tzinfo.rb doesn't exist."))
  }
}

class MainBuildAuxiliaryDockerRequirementsR extends AbstractMainBuildAuxiliaryDockerRequirements{
  override val dockerTag = "viash_requirements_testbench_r"
  override val image = "r-base:4.3.1"

  test("setup; check base image for r still does not contain the glue package", DockerTest) { f =>
    val newConfigFilePath = deriveEngineConfig(None, None, "r_base")

    TestHelper.testMain(
      "build",
      "-o", tempFolStr,
      "--setup", "build",
      newConfigFilePath
    )

    assert(checkDockerImageExists(dockerTag))
    assert(executableRequirementsFile.exists)
    assert(executableRequirementsFile.canExecute)

    val output = Exec.runCatch(
      Seq(
        executableRequirementsFile.toString,
        "--file", "/usr/local/lib/R/site-library/glue/R/glue"
      )
    )

    assert(output.output.contains("/usr/local/lib/R/site-library/glue/R/glue doesn't exist."))
  }

  test("setup; check docker requirements using r to add the glue package", DockerTest) { f =>
    val newConfigFilePath = deriveEngineConfig(Some("""[{ "type": "r", "packages": ["glue"] }]"""), None, "r_glue")

    TestHelper.testMain(
      "build",
      "-o", tempFolStr,
      "--setup", "build",
      newConfigFilePath
    )

    assert(checkDockerImageExists(dockerTag))
    assert(executableRequirementsFile.exists)
    assert(executableRequirementsFile.canExecute)

    val output = Exec.runCatch(
      Seq(
        executableRequirementsFile.toString,
        "--file", "/usr/local/lib/R/site-library/glue/R/glue"
      )
    )

    assert(output.output.contains("/usr/local/lib/R/site-library/glue/R/glue exists."))
  }

  test("setup; check docker requirements using r but with an empty list", DockerTest) { f =>
    val newConfigFilePath = deriveEngineConfig(Some("""[{ "type": "r", "packages": [] }]"""), None, "r_empty")

    TestHelper.testMain(
      "build",
      "-o", tempFolStr,
      "--setup", "build",
      newConfigFilePath
    )

    assert(checkDockerImageExists(dockerTag))
    assert(executableRequirementsFile.exists)
    assert(executableRequirementsFile.canExecute)

    val output = Exec.runCatch(
      Seq(
        executableRequirementsFile.toString,
        "--file", "/usr/local/lib/R/site-library/glue/R/glue"
      )
    )

    assert(output.output.contains("/usr/local/lib/R/site-library/glue/R/glue doesn't exist."))
  }
}

class MainBuildAuxiliaryDockerRequirementsRBioc extends AbstractMainBuildAuxiliaryDockerRequirements{
  override val dockerTag = "viash_requirements_testbench_rbioc"
  override val image = "r-base:4.3.1"

  test("setup; check base image for r-bioc still does not contain the BiocGenerics package", DockerTest) { f =>
    val newConfigFilePath = deriveEngineConfig(None, None, "rbioc_base")

    TestHelper.testMain(
      "build",
      "-o", tempFolStr,
      "--setup", "build",
      newConfigFilePath
    )

    assert(checkDockerImageExists(dockerTag))
    assert(executableRequirementsFile.exists)
    assert(executableRequirementsFile.canExecute)

    val output = Exec.runCatch(
      Seq(
        executableRequirementsFile.toString,
        "--file", "/usr/local/lib/R/site-library/BiocGenerics/R/BiocGenerics"
      )
    )

    assert(output.output.contains("/usr/local/lib/R/site-library/BiocGenerics/R/BiocGenerics doesn't exist."))
  }

  test("setup; check docker requirements using r to add the BiocGenerics package", DockerTest) { f =>
    val newConfigFilePath = deriveEngineConfig(Some("""[{ "type": "r", "bioc": ["BiocGenerics"] }]"""), None, "rbioc_biocgenerics")

    TestHelper.testMain(
      "build",
      "-o", tempFolStr,
      "--setup", "build",
      newConfigFilePath
    )

    assert(checkDockerImageExists(dockerTag))
    assert(executableRequirementsFile.exists)
    assert(executableRequirementsFile.canExecute)

    val output = Exec.runCatch(
      Seq(
        executableRequirementsFile.toString,
        "--file", "/usr/local/lib/R/site-library/BiocGenerics/R/BiocGenerics"
      )
    )

    assert(output.output.contains("/usr/local/lib/R/site-library/BiocGenerics/R/BiocGenerics exists."))
  }

  test("setup; check docker requirements using r but with an empty list", DockerTest) { f =>
    val newConfigFilePath = deriveEngineConfig(Some("""[{ "type": "r", "bioc": [] }]"""), None, "rbioc_empty")

    TestHelper.testMain(
      "build",
      "-o", tempFolStr,
      "--setup", "build",
      newConfigFilePath
    )

    assert(checkDockerImageExists(dockerTag))
    assert(executableRequirementsFile.exists)
    assert(executableRequirementsFile.canExecute)

    val output = Exec.runCatch(
      Seq(
        executableRequirementsFile.toString,
        "--file", "/usr/local/lib/R/site-library/BiocGenerics/R/BiocGenerics"
      )
    )

    assert(output.output.contains("/usr/local/lib/R/site-library/BiocGenerics/R/BiocGenerics doesn't exist."))
  }
}


class MainBuildAuxiliaryDockerRequirementsApkTest extends AbstractMainBuildAuxiliaryDockerRequirements {
  override val dockerTag = "viash_requirements_testbench_apktest"
  override val image = "bash:3.2"

  test("test_setup; check the fortune package isn't added for the build option", DockerTest) { f =>
    val newConfigFilePath = deriveEngineConfig(None, Some("""[{ "type": "apk", "packages": ["fortune"] }]"""), "apk_test_fortune_build")

    TestHelper.testMain(
      "build",
      "-o", tempFolStr,
      "--setup", "build",
      newConfigFilePath
    )

    assert(executableRequirementsFile.exists)
    assert(executableRequirementsFile.canExecute)

    val output = Exec.runCatch(
      Seq(
        executableRequirementsFile.toString,
        "--which", "fortune"
      )
    )

    assert(output.output == "")
  }

  test("test_setup; check the fortune package is added for the test option", DockerTest) { f =>
    val newConfigFilePath = deriveEngineConfig(None, Some("""[{ "type": "apk", "packages": ["fortune"] }]"""), "apk_test_fortune_test")

    val testText = TestHelper.testMain(
      "test",
      newConfigFilePath
    )

    assert(testText.contains("Running tests in temporary directory: "))
    assert(testText.contains("SUCCESS! All 1 out of 1 test scripts succeeded!"))
    assert(testText.contains("Cleaning up temporary directory"))
  }

  test("test_setup; check the fortune package is not added for the test option when not specified", DockerTest) { f =>
    val newConfigFilePath = deriveEngineConfig(None, None, "apk_base_test")

    val testOutput = TestHelper.testMainException2[RuntimeException](
      "test",
      "-k", "false",
      newConfigFilePath
    )

    assert(testOutput.exceptionText == "Only 0 out of 1 test scripts succeeded!")

    assert(testOutput.output.contains("Running tests in temporary directory: "))
    assert(testOutput.output.contains("ERROR! Only 0 out of 1 test scripts succeeded!"))
    assert(testOutput.output.contains("Cleaning up temporary directory"))
  }
}
