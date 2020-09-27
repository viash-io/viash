package com.dataintuitive.viash

import java.io.ByteArrayOutputStream
import java.nio.file.Paths

import com.dataintuitive.viash.functionality.Functionality
import com.dataintuitive.viash.helpers.IO
import org.scalatest.{BeforeAndAfterAll, FunSuite}

import scala.reflect.io.Directory


class E2EMainTest extends FunSuite with BeforeAndAfterAll {
  // which platform to test
  private val funcFile = getClass.getResource("/testbash/functionality.yaml").getPath
  private val platFile = getClass.getResource("/testbash/platform_docker.yaml").getPath

  private val temporaryFolder = IO.makeTemp("viash_tester")
  private val tempFolStr = temporaryFolder.toString

  // parse functionality from file
  private val functionality = Functionality.parse(IO.uri(funcFile))


  test("Check standard test output for typical outputs") {
    val testText = executeMainAndCaptureStdOut(
      Array(
        "test",
        "-f", funcFile,
        "-p", platFile
      ))
    assert(testText.contains("Running tests in temporary directory: ") === true)
    assert(testText.contains("SUCCESS! All 3 out of 3 test scripts succeeded!") === true)
    assert(testText.contains("Cleaning up temporary directory") === true)
  }



  test("Check output in case --keep is specified") {
    val testText = executeMainAndCaptureStdOut(
      Array(
        "test",
        "-f", funcFile,
        "-p", platFile,
        "--keep"
      ))
    assert(testText.contains("Running tests in temporary directory: ") === true)
    assert(testText.contains("SUCCESS! All 3 out of 3 test scripts succeeded!") === true)
    assert(testText.contains("Cleaning up temporary directory") === false)

    // Get temporary directory
    val Regex = ".*Running tests in temporary directory: '([^']*)'.*".r

    var tempPath = ""
    testText.replaceAll("\n", "") match {
      case Regex(path) => tempPath = path
      case _ => {}
    }

    assert(tempPath.contains("/tmp/viash_test_testbash") === true)

    // Check temporary directory is still present
    val tempFolder = new Directory(Paths.get(tempPath).toFile)
    assert(tempFolder.exists === true)
    assert(tempFolder.isDirectory === true)

    // Check a file in the directory
    val tempFile = Paths.get(tempPath, "build_executable/NOTICE").toFile
    assert(tempFile.exists === true)
    assert(tempFile.isFile === true)
    assert(tempFile.canRead === true)

    // Remove the temporary directory
    tempFolder.deleteRecursively()
    assert(tempFolder.exists === false)
  }



  def executeMainAndCaptureStdOut(args: Array[String]) : String = {
    val os = new ByteArrayOutputStream()
    Console.withOut(os) {

      Main.main(args)

    }

    val stdout = os.toString()
    //Console.print("stdout: -->")
    Console.print(stdout)
    //Console.println("<--")
    return stdout
  }

  override def afterAll() {
    IO.deleteRecursively(temporaryFolder)
  }
}
