package com.dataintuitive.viash

import com.dataintuitive.viash.config.Config
import com.dataintuitive.viash.helpers.{Exec, IO}
import org.scalatest.{BeforeAndAfterAll, FunSuite}

import java.nio.file.Paths
import scala.io.Source

class MainNSBuildNativeSuite extends FunSuite with BeforeAndAfterAll{
  // path to namespace components
  private val nsPath = getClass.getResource("/testns/").getPath

  private val temporaryFolder = IO.makeTemp("viash_ns_build")
  private val tempFolStr = temporaryFolder.toString

  private val nsFolder = Paths.get(tempFolStr, "native/testns/").toFile

  private val components = List(
    ("ns_add",      1, 2, 3),
    ("ns_subtract", 7, 2, 5),
    ("ns_multiply", 4, 3, 12),
    ("ns_divide",   10, 2, 5),
    ("ns_power",    3, 4, 81),
  )


  // convert testbash
  test("viash ns can build") {
    val out = TestHelper.testMain(
    "ns", "build",
      "-s", nsPath,
      "-t", tempFolStr
    )

    assert(nsFolder.exists)
    assert(nsFolder.isDirectory)

    for ((component, _, _, _) ← components) {
      val executable = Paths.get(nsFolder.toString, s"$component/$component").toFile
      assert(executable.exists)
      assert(executable.canExecute)
    }

  }


  test("Check whether the executable can run") {

    for ((component, _, _, _) ← components) {
      val executable = Paths.get(nsFolder.toString, s"$component/$component").toFile
      Exec.run(
        Seq(executable.toString, "--help")
      )
    }

  }

  test("Check whether particular keywords can be found in the usage") {

    for ((component, _, _, _) ← components) {
      val executable = Paths.get(nsFolder.toString, s"$component/$component").toFile
      val configFile = getClass.getResource(s"/testns/src/$component/config.vsh.yaml").getPath
      val functionality = Config.read(configFile, modifyFun = false).functionality

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

  }

  test("Check whether output is correctly created") {
    for ((component, input1, input2, expectedOutput) ← components) {
      val executable = Paths.get(nsFolder.toString, s"$component/$component").toFile
      val output = Paths.get(tempFolStr, s"output_$component.txt").toFile

      Exec.run(
        Seq(
          executable.toString,
          "--input1", input1.toString,
          "--input2", input2.toString,
          "--output", output.toString,
        )
      )

      assert(output.exists())

      val outputSrc = Source.fromFile(output)
      try {
        val outputLines = outputSrc.mkString
        assert(outputLines.contains(s"input1: $input1 input2: $input2 result: $expectedOutput"))
      } finally {
        outputSrc.close()
      }

    }
  }

  override def afterAll() {
    IO.deleteRecursively(temporaryFolder)
  }
}
