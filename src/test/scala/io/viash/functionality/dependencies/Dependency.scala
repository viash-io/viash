package io.viash.functionality.dependencies

import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite

import io.viash.config.{Config, ConfigMeta}
import io.viash.functionality.Config
import io.viash.config.resources.BashScript
import io.viash.helpers.{IO, Exec, Logger}
import io.viash.helpers.circe._

import java.nio.file.{Files, Path, Paths}
import io.viash.TestHelper
import io.viash.config.dependencies.{LocalRepository, ViashhubRepositoryWithName, Dependency}

class DependencyTest extends AnyFunSuite with BeforeAndAfterAll {
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

  // Write functionality as a config file
  def writeTestConfig(path: Path, fun: Config): Unit = {
    val config = Config(functionality = fun)
    val json = ConfigMeta.configToCleanJson(config)
    val yaml = json.toFormattedString("yaml")
    path.getParent().toFile().mkdirs()
    Files.write(path, yaml.getBytes())
  }

  // Short wrapper for creating a bash script containing some text and using it as a single resource
  def textBashScript(text: String): List[BashScript] = 
    List(BashScript(text = Some(text), dest = Some("./script.sh")))

  test("Use a local dependency") {
    val testFolder = createViashSubFolder(temporaryFolder, "local_test")
    
    // write test files
    val fun1 = Functionality(
      name = "dep1",
      resources = textBashScript("echo Hello from dep1"),
    )
    val fun2 = Functionality(
      name = "dep2",
      resources = textBashScript("$dep_dep1\necho Hello from dep2"),
      dependencies = List(Dependency("dep1"))
    )

    writeTestConfig(testFolder.resolve("src/dep1/config.vsh.yaml"), fun1)
    writeTestConfig(testFolder.resolve("src/dep2/config.vsh.yaml"), fun2)

    // build
    val testOutput = TestHelper.testMain(
        "ns", "build",
        "-s", testFolder.resolve("src").toString(),
        "-t", testFolder.resolve("target").toString()
      )

    assert(testOutput.stderr.strip == "All 2 configs built successfully", "check build was successful")

    // check file & file content
    val outputPath = testFolder.resolve("target/executable/dep2/dep2")
    val executable = outputPath.toFile
    assert(executable.exists)
    assert(executable.canExecute)

    val outputText = IO.read(outputPath.toUri())
    assert(outputText.contains("VIASH_DEP_DEP1="), "check the dependency is set in the output script")

    // check output when running
    val out = Exec.runCatch(
      Seq(executable.toString)
    )

    assert(out.output == "Hello from dep1\nHello from dep2\n")
    assert(out.exitValue == 0)
  }

  test("Use a local repository with an absolute path") {
    val testFolder = createViashSubFolder(temporaryFolder, "local_test_absolute_path")
    
    // write test files
    val fun1 = Functionality(
      name = "dep1",
      resources = textBashScript("echo Hello from dep1"),
    )
    val fun2 = Functionality(
      name = "dep2",
      resources = textBashScript("$dep_dep1\necho Hello from dep2"),
      dependencies = List(Dependency("dep1", repository = Right(LocalRepository(path = Some("/dependencies")))))
    )

    writeTestConfig(testFolder.resolve("dependencies/src/dep1/config.vsh.yaml"), fun1)
    writeTestConfig(testFolder.resolve("src/dep2/config.vsh.yaml"), fun2)

    // build our local repository
    val build1 = TestHelper.testMain(
        workingDir = Some(testFolder.resolve("dependencies")),
        "ns", "build",
        "-s", testFolder.resolve("dependencies/src").toString(),
        "-t", testFolder.resolve("dependencies/target").toString()
      )
    
    assert(build1.stderr.strip == "All 1 configs built successfully", "check dependency build was successful")

    // build
    val build2 = TestHelper.testMain(
        workingDir = Some(testFolder),
        "ns", "build",
        "-s", testFolder.resolve("src").toString(),
        "-t", testFolder.resolve("target").toString()
      )

    assert(build2.stderr.strip == "All 1 configs built successfully", "check build was successful")

    // check file & file content
    val outputPath = testFolder.resolve("target/executable/dep2/dep2")
    val executable = outputPath.toFile
    assert(executable.exists)
    assert(executable.canExecute)

    val outputText = IO.read(outputPath.toUri())
    assert(outputText.contains("VIASH_DEP_DEP1="), "check the dependency is set in the output script")

    // check output when running
    val out = Exec.runCatch(
      Seq(executable.toString)
    )

    assert(out.output == "Hello from dep1\nHello from dep2\n")
    assert(out.exitValue == 0)
  }

  test("Use a local repository with a relative path") {
    val testFolder = createViashSubFolder(temporaryFolder, "local_test_relative_path")
    
    // write test files
    val fun1 = Functionality(
      name = "dep1",
      resources = textBashScript("echo Hello from dep1"),
    )
    val fun2 = Functionality(
      name = "dep2",
      resources = textBashScript("$dep_dep1\necho Hello from dep2"),
      dependencies = List(Dependency("dep1", repository = Right(LocalRepository(path = Some("../../dependencies")))))
    )

    writeTestConfig(testFolder.resolve("dependencies/src/dep1/config.vsh.yaml"), fun1)
    writeTestConfig(testFolder.resolve("src/dep2/config.vsh.yaml"), fun2)

    // build our local repository
    val build1 = TestHelper.testMain(
        workingDir = Some(testFolder.resolve("dependencies")),
        "ns", "build",
        "-s", testFolder.resolve("dependencies/src").toString(),
        "-t", testFolder.resolve("dependencies/target").toString()
      )
    
    assert(build1.stderr.strip == "All 1 configs built successfully", "check dependency build was successful")

    // build
    val build2 = TestHelper.testMain(
        workingDir = Some(testFolder),
        "ns", "build",
        "-s", testFolder.resolve("src").toString(),
        "-t", testFolder.resolve("target").toString()
      )

    assert(build2.stderr.strip == "All 1 configs built successfully", "check build was successful")

    // check file & file content
    val outputPath = testFolder.resolve("target/executable/dep2/dep2")
    val executable = outputPath.toFile
    assert(executable.exists)
    assert(executable.canExecute)

    val outputText = IO.read(outputPath.toUri())
    assert(outputText.contains("VIASH_DEP_DEP1="), "check the dependency is set in the output script")

    // check output when running
    val out = Exec.runCatch(
      Seq(executable.toString)
    )

    assert(out.output == "Hello from dep1\nHello from dep2\n")
    assert(out.exitValue == 0)
  }

  test("Use a remote dependency") {
    val testFolder = createViashSubFolder(temporaryFolder, "remote_test")
    
    // write test files
    val fun = Functionality(
      name = "dep3",
      resources = textBashScript("$dep_viash_hub_dep\necho \"Hello from dep3\""),
      dependencies = List(Dependency("viash_hub/dep", repository = Left("vsh://hendrik/dependency_test@main_build")))
    )

    writeTestConfig(testFolder.resolve("src/dep3/config.vsh.yaml"), fun)

    // build
    val testOutput = TestHelper.testMain(
        "ns", "build",
        "-s", testFolder.resolve("src").toString(),
        "-t", testFolder.resolve("target").toString()
      )

    assert(testOutput.stderr.strip.contains("All 1 configs built successfully"), "check build was successful")

    // check file & file content
    val outputPath = testFolder.resolve("target/executable/dep3/dep3")
    val executable = outputPath.toFile
    assert(executable.exists)
    assert(executable.canExecute)

    val outputText = IO.read(outputPath.toUri())
    assert(outputText.contains("VIASH_DEP_VIASH_HUB_DEP="), "check the dependency is set in the output script")

    // check output when running
    val out = Exec.runCatch(
      Seq(executable.toString)
    )

    assert(out.output == "This is a component in the viash_hub repository.\nHello from dep3\n")
    assert(out.exitValue == 0)
  }

  test("Use a remote dependency, defined in .functionality.repositories") {
    val testFolder = createViashSubFolder(temporaryFolder, "remote_test_repositories")
    
    // write test files
    val fun = Functionality(
      name = "dep3",
      resources = textBashScript("$dep_viash_hub_dep\necho \"Hello from dep3\""),
      repositories = List(ViashhubRepositoryWithName("viash_hub", "vsh", "hendrik/dependency_test", Some("main_build"))),
      dependencies = List(Dependency("viash_hub/dep", repository = Left("viash_hub")))
    )

    writeTestConfig(testFolder.resolve("src/dep3/config.vsh.yaml"), fun)

    // build
    val testOutput = TestHelper.testMain(
        "ns", "build",
        "-s", testFolder.resolve("src").toString(),
        "-t", testFolder.resolve("target").toString()
      )

    assert(testOutput.stderr.strip.contains("All 1 configs built successfully"), "check build was successful")

    // check file & file content
    val outputPath = testFolder.resolve("target/executable/dep3/dep3")
    val executable = outputPath.toFile
    assert(executable.exists)
    assert(executable.canExecute)

    val outputText = IO.read(outputPath.toUri())
    assert(outputText.contains("VIASH_DEP_VIASH_HUB_DEP="), "check the dependency is set in the output script")

    // check output when running
    val out = Exec.runCatch(
      Seq(executable.toString)
    )

    assert(out.output == "This is a component in the viash_hub repository.\nHello from dep3\n")
    assert(out.exitValue == 0)
  }

  test("Use a remote dependency defined in the package config") {
    val packageConfig =
      """repositories:
        |  - name: viash_hub
        |    type: vsh
        |    repo: hendrik/dependency_test
        |    tag: main_build
        |""".stripMargin
    val testFolder = createViashSubFolder(temporaryFolder, "remote_test_package_config", Some(packageConfig))
    
    // write test files
    val fun = Functionality(
      name = "dep3",
      resources = textBashScript("$dep_viash_hub_dep\necho \"Hello from dep3\""),
      dependencies = List(Dependency("viash_hub/dep", repository = Left("viash_hub")))
    )

    writeTestConfig(testFolder.resolve("src/dep3/config.vsh.yaml"), fun)

    // build
    val testOutput = TestHelper.testMain(
        workingDir = Some(testFolder),
        "ns", "build",
        "-s", testFolder.resolve("src").toString(),
        "-t", testFolder.resolve("target").toString()
      )

    assert(testOutput.stderr.strip.contains("All 1 configs built successfully"), "check build was successful")

    // check file & file content
    val outputPath = testFolder.resolve("target/executable/dep3/dep3")
    val executable = outputPath.toFile
    assert(executable.exists)
    assert(executable.canExecute)

    val outputText = IO.read(outputPath.toUri())
    assert(outputText.contains("VIASH_DEP_VIASH_HUB_DEP="), "check the dependency is set in the output script")

    // check output when running
    val out = Exec.runCatch(
      Seq(executable.toString)
    )

    assert(out.output == "This is a component in the viash_hub repository.\nHello from dep3\n")
    assert(out.exitValue == 0)
  }

  test("Use a remote dependency with nested dependencies") {
    val testFolder = createViashSubFolder(temporaryFolder, "nested_remote_test")
    
    // write test files
    val fun = Functionality(
      name = "dep4",
      resources = textBashScript("$dep_viash_hub_test_tree\necho \"Hello from dep4\""),
      dependencies = List(Dependency("viash_hub_test/tree", repository = Left("vsh://hendrik/dependency_test2@main_build")))
    )

    writeTestConfig(testFolder.resolve("src/dep4/config.vsh.yaml"), fun)

    // build
    val testOutput = TestHelper.testMain(
        "ns", "build",
        "-s", testFolder.resolve("src").toString(),
        "-t", testFolder.resolve("target").toString()
      )

    assert(testOutput.stderr.strip.contains("All 1 configs built successfully"), "check build was successful")

    // check file & file content
    val outputPath = testFolder.resolve("target/executable/dep4/dep4")
    val executable = outputPath.toFile
    assert(executable.exists)
    assert(executable.canExecute)

    val outputText = IO.read(outputPath.toUri())
    assert(outputText.contains("VIASH_DEP_VIASH_HUB_TEST_TREE="), "check the dependency is set in the output script")

    // check output when running
    val out = Exec.runCatch(
      Seq(executable.toString)
    )

    assert(out.output == "This is tree.\nThis is bar 1.\nHello from dep4\n")
    assert(out.exitValue == 0)
  }

  override def afterAll(): Unit = {
    IO.deleteRecursively(temporaryFolder)
  }
}
