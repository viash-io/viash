package com.dataintuitive.viash

import org.scalatest.{FunSuite, BeforeAndAfterAll}
import java.nio.file.{Path, Paths, Files}
import java.io.File
import sys.process.Process
import com.dataintuitive.viash.functionality.Functionality
import scala.io.Source
import scala.reflect.io.Directory
import com.dataintuitive.viash.helpers._

class E2EBashDocker extends FunSuite with BeforeAndAfterAll {
  // which platform to test
  val testName = "testbash"
  val funcFile = getClass.getResource(s"/$testName/functionality.yaml").getPath
  val platFile = getClass.getResource(s"/$testName/platform_docker.yaml").getPath

  val temporaryFolder = IOHelper.makeTemp("viash_tester")
  val tempFolStr = temporaryFolder.toString()

  // parse functionality from file
  val functionality = Functionality.parse(new File(funcFile))

  // convert testpython
  val params = Array(
    "export",
    "-f", funcFile,
    "-p", platFile,
    "-o", tempFolStr
   )
  println(params.mkString(" "))
  Main.main(params)

  // check whether executable was created
  val executable = Paths.get(tempFolStr, functionality.name).toFile()
  val execPathInDocker = Paths.get("/data/", functionality.name).toFile().toString()

  test("Viash should have created an executable") {
    assert(executable.exists())
    assert(executable.canExecute())
  }

  test("Check whether the executable can build the image", DockerTest) {
    val out = Exec.run2(
      Seq(executable.toString(), "---setup")
    )
    assert(out.exitValue == 0)
  }

  test("Check whether the executable can run", DockerTest) {
    Exec.run(
      Seq(executable.toString(), "--help")
    )
  }

  test("Check whether particular keywords can be found in the usage", DockerTest) {
    val stdout =
      Exec.run(
        Seq(executable.toString(), "--help")
      )

    functionality.arguments.foreach(arg => {
      assert(stdout.contains(arg.name))
      for (opt <- arg.alternatives; value <- opt)
        assert(stdout.contains(value))
      for (opt <- arg.description; value <- opt)
        assert(stdout.contains(value))
    })

  }

  test("Check whether output is correctly created", DockerTest) {
    val output = Paths.get(tempFolStr, "output.txt").toFile()
    val log = Paths.get(tempFolStr, "log.txt").toFile()

    val stdout =
      Exec.run(
        Seq(
          executable.toString(),
          execPathInDocker,
          "--real_number", "10.5",
          "--whole_number=10",
          "-s", "a string with a few spaces",
          "--truth",
          "--output", "/data/output.txt",
          "--log", "/data/log.txt",
          "--optional", "foo",
          "--optional_with_default", "bar",
          "--passthrough=you shall$not#pass",
          "--passthroughbool",
          "--data", tempFolStr
        )
      )

    assert(output.exists())
    assert(log.exists())

    val outputLines = Source.fromFile(output).mkString
    assert(outputLines.contains(s"""input: "$execPathInDocker""""))
    assert(outputLines.contains("""real_number: "10.5""""))
    assert(outputLines.contains("""whole_number: "10""""))
    assert(outputLines.contains("""s: "a string with a few spaces""""))
    assert(outputLines.contains("""truth: "true""""))
    assert(outputLines.contains("""output: "/data/output.txt""""))
    assert(outputLines.contains("""log: "/data/log.txt""""))
    assert(outputLines.contains("""optional: "foo""""))
    assert(outputLines.contains("""optional_with_default: "bar""""))
    assert(outputLines.contains("""passthrough: "you shall$not#pass""""))
    assert(outputLines.contains("""passthroughbool: "true""""))
    assert(outputLines.contains("""PASSTHROUGH: " --passthrough='you shall$not#pass' --passthroughbool""""))
    assert(outputLines.contains(s"""data: "${tempFolStr}""""))
    assert(outputLines.contains("""resources_dir: "/resources""""))

    val logLines = Source.fromFile(log).mkString
    assert(logLines.contains("INFO: Parsed input arguments"))
  }

  test("Alternative params", DockerTest) {
    val stdout =
      Exec.run(
        Seq(
          executable.toString(),
          execPathInDocker,
          "--real_number", "123.456",
          "--whole_number", "789",
          "-s", "my$weird#string",
          "--data", tempFolStr
        )
      )

    assert(stdout.contains(s"""input: "$execPathInDocker""""))
    assert(stdout.contains("""real_number: "123.456""""))
    assert(stdout.contains("""whole_number: "789""""))
    assert(stdout.contains("""s: "my$weird#string""""))
    assert(stdout.contains("""truth: "false""""))
    assert(stdout.contains("""optional: """""))
    assert(stdout.contains("""optional_with_default: "The default value.""""))
    assert(stdout.contains("""passthrough: """""))
    assert(stdout.contains(s"""PASSTHROUGH: """""))
    assert(stdout.contains(s"""data: "${tempFolStr}""""))
    assert(stdout.contains("""resources_dir: "/resources""""))

    assert(stdout.contains("INFO: Parsed input arguments"))
  }

  override def afterAll() {
    IOHelper.deleteRecursively(temporaryFolder)
  }
}