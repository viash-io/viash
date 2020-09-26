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

  // convert testbash
  private val params = Array(
    "test",
    "-f", funcFile,
    "-p", platFile
  )

  private val os = new ByteArrayOutputStream()
  Console.withOut(os) {

    Main.main(params)

  }

  private val stdout = os.toString()
  Console.print("stdout: -->")
  Console.print(stdout)
  Console.println("<--")

  test("Check standard test output for typical outputs") {
    assert(stdout.contains("SUCCESS! All 3 out of 3 test scripts succeeded!"))
    assert(stdout.contains("Cleaning up temporary files"))
  }


  override def afterAll() {
    IO.deleteRecursively(temporaryFolder)
  }
}
