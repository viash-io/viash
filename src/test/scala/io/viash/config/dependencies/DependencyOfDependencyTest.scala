package io.viash.config.dependencies

import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite

import io.viash.helpers.{Exec, IO, Logger}
import io.viash.config.{Config, ConfigMeta}
import io.viash.config.resources.BashScript
import io.viash.helpers.circe._
import io.viash.TestHelper

import java.nio.file.{Files, Path}

// Goal: Test a component that has a dependency that has a dependency
// However: Create the components from scratch so that we can pick up breaking changes faster than if we used pre-built components on a remote repository
// Reason: Dependencies of dependencies need to be resolved differently than dependencies of the component itself
// Comp 3 -> Comp 2 -> Comp 1

class DependencyOfDependencyTest extends AnyFunSuite with BeforeAndAfterAll {
  Logger.UseColorOverride.value = Some(false)

  private val temporaryFolder = IO.makeTemp(s"viash_${this.getClass.getName}_")

  // Create a subfolder and place a _viash.yaml file in it
  def createViashSubFolder(path: Path, name: String, text: Option[String] = None): Path = {
    val subFolder = path.resolve(name)
    subFolder.toFile().mkdir()
    val content = text.getOrElse("")
    Files.write(subFolder.resolve("_viash.yaml"), content.getBytes())
    subFolder
  }

  // Write config to a file
  def writeTestConfig(path: Path, config: Config): Unit = {
    val json = ConfigMeta.configToCleanJson(config)
    val yaml = json.toFormattedString("yaml")
    path.getParent().toFile().mkdirs()
    Files.write(path, yaml.getBytes())
  }

  // Short wrapper for creating a bash script containing some text and using it as a single resource
  def textBashScript(text: String): List[BashScript] = 
    List(BashScript(text = Some(text), dest = Some("./script.sh")))

  test("Prepare package 1") {
    val workingDir = createViashSubFolder(temporaryFolder, "pack1")

    val conf1 = Config(
      name = "comp1",
      resources = textBashScript("echo 'Hello from comp1'"),
    )

    writeTestConfig(workingDir.resolve("src/comp1/config.vsh.yaml"), conf1)

    val testOutput = TestHelper.testMain(
      workingDir = Some(workingDir),
      "ns", "build",
      "-s", workingDir.resolve("src").toString(),
      "-t", workingDir.resolve("target").toString()
    )

    assert(testOutput.stderr.strip == "All 1 configs built successfully", "check build was successful")

    val executable = workingDir.resolve("target/executable/comp1/comp1").toFile()
    assert(executable.exists)
    assert(executable.canExecute)

    // check output when running
    val out = Exec.runCatch(
      Seq(executable.toString)
    )

    assert(out.output == "Hello from comp1\n")
    assert(out.exitValue == 0)
  }

  test("Prepare package 2") {
    val workingDir = createViashSubFolder(temporaryFolder, "pack2")

    val conf1 = Config(
      name = "comp2",
      dependencies = List(
        Dependency(
          name = "comp1",
          repository = Right(LocalRepository(path = Some("/../pack1/")))
        )
      ),
      resources = textBashScript("$dep_comp1\necho 'Hello from comp2'"),
    )

    writeTestConfig(workingDir.resolve("src/comp2/config.vsh.yaml"), conf1)

    val testOutput = TestHelper.testMain(
      workingDir = Some(workingDir),
      "ns", "build",
      "-s", workingDir.resolve("src").toString(),
      "-t", workingDir.resolve("target").toString()
    )

    assert(testOutput.stderr.strip == "All 1 configs built successfully", "check build was successful")

    val executable = workingDir.resolve("target/executable/comp2/comp2").toFile()
    assert(executable.exists)
    assert(executable.canExecute)

    // check output when running
    val out = Exec.runCatch(
      Seq(executable.toString)
    )

    assert(out.output == "Hello from comp1\nHello from comp2\n")
    assert(out.exitValue == 0)
  }

  test("Prepare package 3") {
    val workingDir = createViashSubFolder(temporaryFolder, "pack3")

    val conf1 = Config(
      name = "comp3",
      dependencies = List(
        Dependency(
          name = "comp2",
          repository = Right(LocalRepository(path = Some("/../pack2/")))
        )
      ),
      resources = textBashScript("$dep_comp2\necho 'Hello from comp3'"),
    )

    writeTestConfig(workingDir.resolve("src/comp3/config.vsh.yaml"), conf1)

    val testOutput = TestHelper.testMain(
      workingDir = Some(workingDir),
      "ns", "build",
      "-s", workingDir.resolve("src").toString(),
      "-t", workingDir.resolve("target").toString()
    )

    assert(testOutput.stderr.strip == "All 1 configs built successfully", "check build was successful")

    val executable = workingDir.resolve("target/executable/comp3/comp3").toFile()
    assert(executable.exists)
    assert(executable.canExecute)

    // check output when running
    val out = Exec.runCatch(
      Seq(executable.toString)
    )

    assert(out.output == "Hello from comp1\nHello from comp2\nHello from comp3\n")
    assert(out.exitValue == 0)
  }

  override def afterAll(): Unit = {
    IO.deleteRecursively(temporaryFolder)
  }
}
