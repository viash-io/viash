package com.dataintuitive.viash.escaping

import com.dataintuitive.viash.TestHelper
import com.dataintuitive.viash.config.Config
import com.dataintuitive.viash.helpers._
import org.scalatest.{BeforeAndAfterAll, FunSuite}

import java.io.{IOException, UncheckedIOException}
import java.nio.file.{Files, Path, Paths}
import scala.io.Source

class EscapingNativeTest extends FunSuite with BeforeAndAfterAll {
  // which platform to test
  private val rootPath = getClass.getResource(s"/test_escaping/").getPath
  private val configFile = getClass.getResource(s"/test_escaping/config.vsh.yaml").getPath

  private val temporaryFolder = IO.makeTemp("viash_escaping_test")
  private val tempFolStr = temporaryFolder.toString

  // parse functionality from file
  private val functionality = Config.read(configFile, modifyFun = false).functionality

  // check whether executable was created
  private val executable = Paths.get(tempFolStr, functionality.name).toFile

  // convert testbash
  test("viash can build the config file without special 'to-escape-characters'") {
    TestHelper.testMain(
      "build",
      "-p", "native",
      "-o", tempFolStr,
      configFile
    )

    assert(executable.exists)
    assert(executable.canExecute)
  }

  test("Check whether the executable can run") {
    val stdout =
    Exec.run(
      Seq(executable.toString, "--help")
    )
  }

  // define character sequences like it would be regex'ed in sed
  val escapeCharacters = List("$", raw"\\", "\"", "`", "'", raw"\n")

  var i = 0
  for (chars <- escapeCharacters) {
    i += 1

    // make a subfolder for each character to test
    val tempSubFolder = Paths.get(tempFolStr, s"test_$i")
    tempSubFolder.toFile.mkdir
    TestHelper.copyFolder(rootPath, tempSubFolder.toString)

    val configSubFile = Paths.get(tempSubFolder.toString, s"config.vsh.yaml")

    // replace placeholder with character sequence
    Exec.run(
      Seq("sed", "-i", s"s/{test_detect}/$chars/g", configSubFile.toString)
    )

    test(s"Check whether $chars get escaped properly") {

      val functionalitySub = Config.read(configSubFile.toString, modifyFun = false).functionality
      val executableSub = Paths.get(tempSubFolder.toString, "output", functionalitySub.name).toFile

      // build the script
      TestHelper.testMain(
        "build",
        "-p", "native",
        "-o", Paths.get(tempSubFolder.toString, "output").toString,
        configSubFile.toString
      )

      assert(executableSub.exists)
      assert(executableSub.canExecute)


      val stdout =
        Exec.run(
          Seq(executableSub.toString, "--help")
        )

      val stripAll = (s: String) => s.replaceAll(raw"\s+", " ").strip

      // test if descriptions match
      functionalitySub.arguments.foreach(arg => {
        for (opt <- arg.alternatives; value <- opt)
          assert(stdout.contains(value))
        for (description <- arg.description)
          assert(stripAll(stdout).contains(stripAll(description)))
      })
    }
  }

  override def afterAll() {
    IO.deleteRecursively(temporaryFolder)
  }
}