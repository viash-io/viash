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
  private val funcNoTestFile = getClass.getResource("/testbash/functionality_no_tests.yaml").getPath
  private val funcFailedTestFile = getClass.getResource("/testbash/functionality_failed_test.yaml").getPath
  private val platFile = getClass.getResource("/testbash/platform_docker.yaml").getPath

  private val temporaryFolder = IO.makeTemp("viash_tester")
  private val tempFolStr = temporaryFolder.toString

  // parse functionality from file
  private val functionality = Functionality.parse(IO.uri(funcFile))

  Console.println(s"tempFolStr: $tempFolStr")


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


  test("Check test output when no tests are specified in the functionality file") {
    val testText = executeMainAndCaptureStdOut(
      Array(
        "test",
        "-f", funcNoTestFile,
        "-p", platFile
      ))
    assert(testText.contains("Running tests in temporary directory: ") === true)
    assert(testText.contains("SUCCESS! All 1 out of 1 test scripts succeeded!") === true)
    assert(testText.contains("Cleaning up temporary directory") === true)
  }

  test("Check test output when a test fails") {

    // don't use executeMainAndCaptureStdOut here as the exception will prevent us from getting the stdout text
    val os = new ByteArrayOutputStream()

    assertThrows[RuntimeException] {
      Console.withOut(os) {
        Main.main(Array(
          "test",
          "-f", funcFailedTestFile,
          "-p", platFile
        ))
      }
    }

    val testText = os.toString()
    Console.print(testText)

    assert(testText.contains("Running tests in temporary directory: ") === true)
    assert(testText.contains("ERROR! Only 2 out of 3 test scripts succeeded!") === true)
    assert(testText.contains("Cleaning up temporary directory") === false)

    // TODO clean up folder of failed test
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
