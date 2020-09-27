package com.dataintuitive.viash

import java.io.ByteArrayOutputStream

import com.dataintuitive.viash.functionality.Functionality
import com.dataintuitive.viash.helpers.IO
import org.scalatest.{BeforeAndAfterAll, FunSuite}


class E2EMainBashDocker extends FunSuite with BeforeAndAfterAll {
  // which platform to test
  private val testName = "testbash"
  private val funcFile = getClass.getResource(s"/$testName/functionality.yaml").getPath
  private val platFile = getClass.getResource(s"/$testName/platform_docker.yaml").getPath

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
    assert(testText.contains("Running tests in temporary directory: "))
    assert(testText.contains("SUCCESS! All 3 out of 3 test scripts succeeded!"))
    assert(testText.contains("Cleaning up temporary directory"))
  }



  test("Check output in case of --keep is specified") {
    val testText = executeMainAndCaptureStdOut(
      Array(
        "test",
        "-f", funcFile,
        "-p", platFile,
        "--keep"
      ))
    assert(testText.contains("Running tests in temporary directory: "))
    assert(testText.contains("SUCCESS! All 3 out of 3 test scripts succeeded!"))
    assert(!testText.contains("Cleaning up temporary directory"))

    // Running tests in temporary directory: '/tmp/viash_test_testbash9467890914539760538'
    // Check folder exists
    // Check file in folder
    // remove temporary directory
  }



  def executeMainAndCaptureStdOut(args: Array[String]) : String = {
    val os = new ByteArrayOutputStream()
    Console.withOut(os) {

      Main.main(args)

    }

    val stdout = os.toString()
    //Console.print("stdout: -->")
    //Console.print(stdout)
    //Console.println("<--")
    return stdout
  }

  override def afterAll() {
    IO.deleteRecursively(temporaryFolder)
  }
}
