package io.viash.e2e.ns_test

import io.viash._

import io.viash.helpers.{IO, Logger}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite

import java.nio.file.{Files, Path}

class MainNSTestExitCodeSuite extends AnyFunSuite with BeforeAndAfterAll {
  Logger.UseColorOverride.value = Some(false)

  private val temporaryFolder = IO.makeTemp("viash_ns_test_exit_code")

  /** Create a namespace with a single component and return its source directory */
  private def makeNamespace(name: String, config: String): Path = {
    val srcDir = temporaryFolder.resolve(name).resolve("src")
    val componentDir = srcDir.resolve(name)
    Files.createDirectories(componentDir)
    Files.writeString(componentDir.resolve("config.vsh.yaml"), config)
    srcDir
  }

  private def nsTest(srcDir: Path): TestHelper.TestMainOutput = {
    TestHelper.testMain(
      "ns", "test",
      "--src", srcDir.toString,
      "--keep", "false"
    )
  }

  test("ns test exits with code 0 when all tests succeed", NativeTest) {
    val srcDir = makeNamespace("passing_test",
      """name: passing_test
        |resources:
        |  - type: bash_script
        |    text: echo hello
        |test_resources:
        |  - type: bash_script
        |    text: exit 0
        |engines:
        |  - type: native
        |""".stripMargin)

    val testOutput = nsTest(srcDir)

    assert(testOutput.exitCode == Some(0), s"stdout:\n${testOutput.stdout}\nstderr:\n${testOutput.stderr}")
  }

  test("ns test exits with code 0 when a component has no tests", NativeTest) {
    val srcDir = makeNamespace("missing_test",
      """name: missing_test
        |resources:
        |  - type: bash_script
        |    text: echo hello
        |engines:
        |  - type: native
        |""".stripMargin)

    val testOutput = nsTest(srcDir)

    assert(testOutput.stderr.contains("tests missing"), s"stderr:\n${testOutput.stderr}")
    assert(testOutput.exitCode == Some(0), s"stdout:\n${testOutput.stdout}\nstderr:\n${testOutput.stderr}")
  }

  test("ns test exits with code 1 when a test fails", NativeTest) {
    val srcDir = makeNamespace("failing_test",
      """name: failing_test
        |resources:
        |  - type: bash_script
        |    text: echo hello
        |test_resources:
        |  - type: bash_script
        |    text: exit 1
        |engines:
        |  - type: native
        |""".stripMargin)

    val testOutput = nsTest(srcDir)

    assert(testOutput.stderr.contains("tests failed"), s"stderr:\n${testOutput.stderr}")
    assert(testOutput.exitCode == Some(1), s"stdout:\n${testOutput.stdout}\nstderr:\n${testOutput.stderr}")
  }

  test("ns test exits with code 1 when the docker image fails to build", DockerTest) {
    val srcDir = makeNamespace("failing_build",
      """name: failing_build
        |resources:
        |  - type: bash_script
        |    text: echo hello
        |test_resources:
        |  - type: bash_script
        |    text: exit 0
        |engines:
        |  - type: docker
        |    image: bash:5.2
        |    target_image: viash_ns_test_exit_code/failing_build
        |    setup:
        |      - type: docker
        |        run: exit 1
        |""".stripMargin)

    val testOutput = nsTest(srcDir)

    assert(testOutput.stderr.contains("configs built failed"), s"stderr:\n${testOutput.stderr}")
    assert(testOutput.exitCode == Some(1), s"stdout:\n${testOutput.stdout}\nstderr:\n${testOutput.stderr}")
  }

  override def afterAll(): Unit = {
    IO.deleteRecursively(temporaryFolder)
  }
}
