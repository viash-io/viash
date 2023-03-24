package io.viash.escaping

import io.viash.TestHelper
import io.viash.config.Config
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite

import java.io.{IOException, UncheckedIOException}
import java.nio.file.{Files, Path, Paths}
import scala.io.Source
import io.viash.helpers.{IO, Exec}

class EscapingNativeTest extends AnyFunSuite with BeforeAndAfterAll {
  // which platform to test
  private val rootPath = getClass.getResource(s"/test_escaping/").getPath
  private val configFile = getClass.getResource(s"/test_escaping/config.vsh.yaml").getPath

  private val temporaryFolder = IO.makeTemp("viash_escaping_test")
  private val tempFolStr = temporaryFolder.toString

  // parse functionality from file
  private val functionality = Config.read(configFile, addOptMainScript = false).functionality

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

  val singleChars = Seq("$", raw"\n")
  val repeatingChars = for (c <- Seq("\\", "\"", "'", "`"); r <- 1 to 4) yield c * r
  
  for ((chars, i) <- (singleChars ++ repeatingChars).zipWithIndex) {

    // make a subfolder for each character to test
    val tempSubFolder = Paths.get(tempFolStr, s"test_$i")
    tempSubFolder.toFile.mkdir
    IO.copyFolder(rootPath, tempSubFolder.toString)

    val configSubFile = Paths.get(tempSubFolder.toString, s"config.vsh.yaml")
    val sedEscaped = chars.replaceAll(raw"\\", raw"\\\\")

    // replace placeholder with character sequence
    Exec.run(
      Seq("sed", "-i'.original'", s"s/{test_detect}/$sedEscaped/g", configSubFile.toString)
    )

    test(s"Check whether $chars gets escaped properly") {

      val functionalitySub = Config.read(configSubFile.toString, addOptMainScript = false).functionality
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

      val stripAll = (s: String) => s.replaceAll(raw"\s+", " ").trim

      // test if descriptions match
      functionalitySub.arguments.foreach(arg => {
        for (opt <- arg.alternatives; value <- opt)
          assert(stdout.contains(value))
        for (description <- arg.description)
          assert(stripAll(stdout).contains(stripAll(description)))
      })
    }
  }

  override def afterAll(): Unit = {
    IO.deleteRecursively(temporaryFolder)
  }
}