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
    println(s"stdout: $stdout")
  }

  val escapeCharacters = List(/*'`', */"$", "\\\\", "\"")

  var i = 0
  for (char <- escapeCharacters) {
    i += 1

    // make a subfolder for each character to test
    val tempSubFolder = Paths.get(tempFolStr, s"test_$i")
    tempSubFolder.toFile.mkdir
    copyFolder(rootPath.toString, tempSubFolder.toString)

    val configSubFile = Paths.get(tempSubFolder.toString, s"config.vsh.yaml")
    val executableSub = Paths.get(tempSubFolder.toString, "output", functionality.name).toFile

    println(s"temp Folder and file: ${tempSubFolder.toString} ${configSubFile.toString} ${executableSub.toString}")

    //sed 's/Athens/Rome/;s/Greece/Italy/' message.txt
    Exec.run(
      Seq("sed", "-i", s"s/{test_detect}/$char/g", configSubFile.toString)
    )

    // TODO replace '{test_detect}' with character



    TestHelper.testMain(
      "build",
      "-p", "native",
      "-o", Paths.get(tempSubFolder.toString, "output").toString,
      configSubFile.toString
    )

    assert(executableSub.exists)
    assert(executableSub.canExecute)
  }

  test("Check whether particular keywords can be found in the usage") {
    val stdout =
      Exec.run(
        Seq(executable.toString, "--help")
      )

    val stripAll = (s : String) => s.replaceAll(raw"\s+", " ").strip

    functionality.arguments.foreach(arg => {
      for (opt <- arg.alternatives; value <- opt)
        assert(stdout.contains(value))
      for (description <- arg.description) {
        assert(stripAll(stdout).contains(stripAll(description)))
      }
    })
  }

  // code based on https://stackoverflow.com/questions/29076439/java-8-copy-directory-recursively/34254130#34254130
  def copyFolder(src: String, dest: String): Unit = {
    try {
      val stream = Files.walk(Paths.get(src))
      try stream.forEachOrdered((sourcePath: Path) => {

        def foo(sourcePath: Path) =
          try {
            val newPath = Paths.get(dest).resolve(Paths.get(src).relativize(sourcePath))
            if (sourcePath.toFile.isFile) {
              Files.copy(sourcePath, newPath)
            }
            else if (sourcePath.toFile.isDirectory) {
              newPath.toFile.mkdir()
            }

          } catch {
            case e: IOException =>
              throw new UncheckedIOException(e)
          }

        foo(sourcePath)
      })
      finally if (stream != null) stream.close()
    }
  }


  override def afterAll() {
    //IO.deleteRecursively(temporaryFolder)
  }
}