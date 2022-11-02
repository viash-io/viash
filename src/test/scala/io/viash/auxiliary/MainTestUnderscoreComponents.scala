package io.viash.auxiliary

import org.scalatest.{BeforeAndAfterAll, FunSuite}
import java.nio.file.Paths

import io.viash.config.Config

import scala.io.Source
import io.viash.helpers.{IO, Exec}
import io.viash.TestHelper
import scala.sys.process.{Process, ProcessLogger}
class MainTestUnderscoreComponents extends FunSuite with BeforeAndAfterAll {
  private val nsPath = System.getProperty("user.dir") + "/src/viash"

  private val temporaryFolder = IO.makeTemp("viash_tester")
  private val tempFolStr = temporaryFolder.toString

  private val newNsPath = Paths.get(tempFolStr, "src").toString

  println(s"nsPath: $nsPath")
  println(s"tempFolStr: $tempFolStr")
  
  test("Copy underscore components into temporary folder") {
    TestHelper.copyFolder(nsPath, newNsPath)
    Process(Seq("tree", tempFolStr)).!(ProcessLogger(println, println))
    Process(Seq("ls", "-R", tempFolStr)).!(ProcessLogger(println, println))
  }

  test("Test running viash ns test on the underscore components") {
    val (stdout, stderr, exitCode) = TestHelper.testMainWithStdErr(
      "ns", "test",
      "--src", newNsPath,
    )

    assert(!stdout.contains("ERROR"), stdout)
    assert(!stderr.contains("tests failed"), stderr)
    assert(exitCode == 0, s"stdout: $stdout\nstderr: $stderr")
  }

  override def afterAll() {
    IO.deleteRecursively(temporaryFolder)
  }
}