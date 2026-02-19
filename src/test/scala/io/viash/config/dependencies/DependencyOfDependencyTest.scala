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

  // Build a package with the given configs and return the working directory
  def buildPackage(
    packageName: String,
    configs: List[(String, Config)]
  ): Path = {
    val workingDir = createViashSubFolder(temporaryFolder, packageName)

    // Write all configs
    configs.foreach { case (componentName, config) =>
      writeTestConfig(workingDir.resolve(s"src/$componentName/config.vsh.yaml"), config)
    }

    // Build the namespace
    val testOutput = TestHelper.testMain(
      workingDir = Some(workingDir),
      "ns", "build",
      "-s", workingDir.resolve("src").toString(),
      "-t", workingDir.resolve("target").toString()
    )

    assert(testOutput.stderr.strip == s"All ${configs.size} configs built successfully", "check build was successful")

    // Verify executables
    configs.foreach { case (componentName, config) =>
      val executable = workingDir.resolve(s"target/executable/${config.name}/${config.name}").toFile()
      assert(executable.exists, s"executable for ${config.name} should exist")
      assert(executable.canExecute, s"executable for ${config.name} should be executable")
    }

    workingDir
  }

  // Test an executable's output, automatically building expected output based on component name
  // comp1 -> "Hello from comp1\n"
  // comp1b -> "Hello from comp1\nHello from comp1b\n"
  // comp2 -> "Hello from comp1\nHello from comp1b\nHello from comp2\n"
  def assertExecutableOutput(workingDir: Path, componentName: String): Unit = {
    val numberPattern = "comp(\\d+)([a-z]*)".r
    val expectedOutput = componentName match {
      case numberPattern(num, suffix) =>
        val n = num.toInt
        val components = (1 to n).flatMap { i =>
          if ((i < n) || (suffix == "b")) List(s"comp$i", s"comp${i}b")
          else List(s"comp$i")
        }
        components.map(c => s"Hello from $c").mkString("", "\n", "\n")
      case _ => ""
    }
    
    val executable = workingDir.resolve(s"target/executable/$componentName/$componentName").toFile()
    val result = Exec.runCatch(Seq(executable.toString))
    assert(result.output == expectedOutput, s"output for $componentName should match")
    assert(result.exitValue == 0, s"exit value for $componentName should be 0")
  }

  test("Prepare package 1") {
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

    val workingDir = buildPackage("pack1", List(
      ("comp1", conf1),
      ("comp1b", conf1b)
    ))

    // Test outputs
    assertExecutableOutput(workingDir, "comp1")
    assertExecutableOutput(workingDir, "comp1b")
  }

  test("Prepare package 2") {
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

    val workingDir = buildPackage("pack2", List(
      ("comp2", conf2),
      ("comp2b", conf2b)
    ))

    // Test outputs
    assertExecutableOutput(workingDir, "comp2")
    assertExecutableOutput(workingDir, "comp2b")
  }

  test("Prepare package 3") {
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

    val workingDir = buildPackage("pack3", List(
      ("comp3", conf3),
      ("comp3b", conf3b)
    ))

    // Test outputs
    assertExecutableOutput(workingDir, "comp3")
    assertExecutableOutput(workingDir, "comp3b")
  }

  test("Prepare package 4") {
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

    val workingDir = buildPackage("pack4", List(
      ("comp4", conf4),
      ("comp4b", conf4b)
    ))

    // Test outputs
    assertExecutableOutput(workingDir, "comp4")
    assertExecutableOutput(workingDir, "comp4b")
  }

  override def afterAll(): Unit = {
    IO.deleteRecursively(temporaryFolder)
  }
}
