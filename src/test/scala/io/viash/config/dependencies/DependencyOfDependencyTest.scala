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
// Additionally we're testing handof between local dependencies too as they result in different resolution logic and can cause issues after a few steps (ie. 3 levels).
// comb4b -> comp4 -> comp3b -> comp3 -> comp2b -> comp2 -> comp1b -> comp1

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

    val conf1b = Config(
      name = "comp1b",
      dependencies = List(
        Dependency(
          name = "comp1",
        )
      ),
      resources = textBashScript("$dep_comp1\necho 'Hello from comp1b'"),
    )

    writeTestConfig(workingDir.resolve("src/comp1/config.vsh.yaml"), conf1)
    writeTestConfig(workingDir.resolve("src/comp1b/config.vsh.yaml"), conf1b)

    val testOutput = TestHelper.testMain(
      workingDir = Some(workingDir),
      "ns", "build",
      "-s", workingDir.resolve("src").toString(),
      "-t", workingDir.resolve("target").toString()
    )

    assert(testOutput.stderr.strip == "All 2 configs built successfully", "check build was successful")

    val executable1 = workingDir.resolve("target/executable/comp1/comp1").toFile()
    val executable1b = workingDir.resolve("target/executable/comp1b/comp1b").toFile()
    assert(executable1.exists)
    assert(executable1.canExecute)
    assert(executable1b.exists)
    assert(executable1b.canExecute)

    // check output when running
    val out1 = Exec.runCatch(
      Seq(executable1.toString)
    )

    assert(out1.output == "Hello from comp1\n")
    assert(out1.exitValue == 0)

    val out1b = Exec.runCatch(
      Seq(executable1b.toString)
    )

    assert(out1b.output == "Hello from comp1\nHello from comp1b\n")
    assert(out1b.exitValue == 0)
  }

  test("Prepare package 2") {
    val workingDir = createViashSubFolder(temporaryFolder, "pack2")

    val conf2 = Config(
      name = "comp2",
      dependencies = List(
        Dependency(
          name = "comp1b",
          repository = Right(LocalRepository(path = Some("/../pack1/")))
        )
      ),
      resources = textBashScript("$dep_comp1b\necho 'Hello from comp2'"),
    )

    val conf2b = Config(
      name = "comp2b",
      dependencies = List(
        Dependency(
          name = "comp2",
        )
      ),
      resources = textBashScript("$dep_comp2\necho 'Hello from comp2b'"),
    )

    writeTestConfig(workingDir.resolve("src/comp2/config.vsh.yaml"), conf2)
    writeTestConfig(workingDir.resolve("src/comp2b/config.vsh.yaml"), conf2b)

    val testOutput = TestHelper.testMain(
      workingDir = Some(workingDir),
      "ns", "build",
      "-s", workingDir.resolve("src").toString(),
      "-t", workingDir.resolve("target").toString()
    )

    assert(testOutput.stderr.strip == "All 2 configs built successfully", "check build was successful")

    val executable2 = workingDir.resolve("target/executable/comp2/comp2").toFile()
    val executable2b = workingDir.resolve("target/executable/comp2b/comp2b").toFile()
    assert(executable2.exists)
    assert(executable2.canExecute)
    assert(executable2b.exists)
    assert(executable2b.canExecute)

    // check output when running
    val out2 = Exec.runCatch(
      Seq(executable2.toString)
    )
    assert(out2.output == "Hello from comp1\nHello from comp1b\nHello from comp2\n")
    assert(out2.exitValue == 0)

    val out2b = Exec.runCatch(
      Seq(executable2b.toString)
    )

    assert(out2b.output == "Hello from comp1\nHello from comp1b\nHello from comp2\nHello from comp2b\n")
    assert(out2b.exitValue == 0)
  }

  test("Prepare package 3") {
    val workingDir = createViashSubFolder(temporaryFolder, "pack3")

    val conf3 = Config(
      name = "comp3",
      dependencies = List(
        Dependency(
          name = "comp2b",
          repository = Right(LocalRepository(path = Some("/../pack2/")))
        )
      ),
      resources = textBashScript("$dep_comp2b\necho 'Hello from comp3'"),
    )
    val conf3b = Config(
      name = "comp3b",
      dependencies = List(
        Dependency(
          name = "comp3",
        )
      ),
      resources = textBashScript("$dep_comp3\necho 'Hello from comp3b'"),
    )

    writeTestConfig(workingDir.resolve("src/comp3/config.vsh.yaml"), conf3)
    writeTestConfig(workingDir.resolve("src/comp3b/config.vsh.yaml"), conf3b)

    val testOutput = TestHelper.testMain(
      workingDir = Some(workingDir),
      "ns", "build",
      "-s", workingDir.resolve("src").toString(),
      "-t", workingDir.resolve("target").toString()
    )

    assert(testOutput.stderr.strip == "All 2 configs built successfully", "check build was successful")

    
    val executable3 = workingDir.resolve("target/executable/comp3/comp3").toFile()
    val executable3b = workingDir.resolve("target/executable/comp3b/comp3b").toFile()
    assert(executable3.exists)
    assert(executable3.canExecute)
    assert(executable3b.exists)
    assert(executable3b.canExecute)

    // check output when running
    val out3 = Exec.runCatch(
      Seq(executable3.toString)
    )

    assert(out3.output == "Hello from comp1\nHello from comp1b\nHello from comp2\nHello from comp2b\nHello from comp3\n")
    assert(out3.exitValue == 0)

    val out3b = Exec.runCatch(
      Seq(executable3b.toString)
    )

    assert(out3b.output == "Hello from comp1\nHello from comp1b\nHello from comp2\nHello from comp2b\nHello from comp3\nHello from comp3b\n")
    assert(out3b.exitValue == 0)
  }

  test("Prepare package 4") {
    val workingDir = createViashSubFolder(temporaryFolder, "pack4")

    val conf4 = Config(
      name = "comp4",
      dependencies = List(
        Dependency(
          name = "comp3b",
          repository = Right(LocalRepository(path = Some("/../pack3/")))
        )
      ),
      resources = textBashScript("$dep_comp3b\necho 'Hello from comp4'"),
    )
    val conf4b = Config(
      name = "comp4b",
      dependencies = List(
        Dependency(
          name = "comp4",
        )
      ),
      resources = textBashScript("$dep_comp4\necho 'Hello from comp4b'"),
    )

    writeTestConfig(workingDir.resolve("src/comp4/config.vsh.yaml"), conf4)
    writeTestConfig(workingDir.resolve("src/comp4b/config.vsh.yaml"), conf4b)

    val testOutput = TestHelper.testMain(
      workingDir = Some(workingDir),
      "ns", "build",
      "-s", workingDir.resolve("src").toString(),
      "-t", workingDir.resolve("target").toString()
    )

    assert(testOutput.stderr.strip == "All 2 configs built successfully", "check build was successful")

    val executable4 = workingDir.resolve("target/executable/comp4/comp4").toFile()
    val executable4b = workingDir.resolve("target/executable/comp4b/comp4b").toFile()
    assert(executable4.exists)
    assert(executable4.canExecute)
    assert(executable4b.exists)
    assert(executable4b.canExecute)

    // check output when running
    val out4 = Exec.runCatch(
      Seq(executable4.toString)
    )

    assert(out4.output == "Hello from comp1\nHello from comp1b\nHello from comp2\nHello from comp2b\nHello from comp3\nHello from comp3b\nHello from comp4\n")
    assert(out4.exitValue == 0)

    val out4b = Exec.runCatch(
      Seq(executable4b.toString)
    )

    assert(out4b.output == "Hello from comp1\nHello from comp1b\nHello from comp2\nHello from comp2b\nHello from comp3\nHello from comp3b\nHello from comp4\nHello from comp4b\n")
    assert(out4b.exitValue == 0)
  }

  override def afterAll(): Unit = {
    IO.copyFolder(temporaryFolder, Path.of("/home/hendrik/DI/temp/dep_tests2/"))
    IO.deleteRecursively(temporaryFolder)
  }
}
