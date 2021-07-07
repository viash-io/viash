package com.dataintuitive.viash.platforms

import com.dataintuitive.viash.helpers.IO
import com.dataintuitive.viash.{DockerTest, NextFlowTest, TestHelper}
import org.scalatest.{BeforeAndAfterAll, FunSuite}

import java.io.File
import java.nio.file.{Files, Path, Paths}
import scala.io.Source

import java.io.IOException
import java.io.UncheckedIOException

class NextFlowPlatformTest extends FunSuite with BeforeAndAfterAll {
  // temporary folder to work in
  private val temporaryFolder = IO.makeTemp("viash_tester_nextflow")
  private val tempFolStr = temporaryFolder.toString

  // path to namespace components
  private val rootPath = getClass.getResource("/testnextflow/").getPath
  private val srcPath = Paths.get(tempFolStr, "src").toFile.toString
  private val targetPath = Paths.get(tempFolStr, "target").toFile.toString

  // convert testbash
  test("build pipeline components", DockerTest, NextFlowTest) {

    // copy resources to temporary folder so we can build in a clean environment
    for (resource <- List("src", "workflows", "resources"))
      copyFolder(Paths.get(rootPath, resource).toString, Paths.get(tempFolStr, resource).toString)

    // build the nextflow containers
    val (_, _) = TestHelper.testMainWithStdErr(
      "ns", "build",
      "-s", srcPath,
      "-t", targetPath,
      "--setup", "cb"
    )

    import sys.process._
    val output = Process(
      Seq("nextflow", "run", ".", "-main-script", "workflows/main.nf", "--input", "resources/*", "--publishDir", "output"),
      new File(tempFolStr),
      "NXF_VER" -> "21.04.1"
    ).!!
    val lines = output.split("\n").find(_.contains("DEBUG7"))

    assert(lines.isDefined)
    val DebugRegex = "\\[DEBUG7, foo, (.*)\\]".r
    val DebugRegex(path) = lines.get

    val src = Source.fromFile(path)
    try {
      val step3Out = src.getLines.mkString
      assert(step3Out.matches("^11 .*$"))
    } finally {
      src.close()
    }
    // TODO: check other debug flags as well.
    // TODO: change step3 into something more interesting.
  }



  // code based on https://stackoverflow.com/questions/29076439/java-8-copy-directory-recursively/34254130#34254130
  def copyFolder(src: String, dest: String) {
    val stream = Files.walk(Paths.get(src))

    try {
      stream.forEachOrdered((sourcePath: Path) => {

        try {
          val newPath = Paths.get(dest).resolve(Paths.get(src).relativize(sourcePath))
          if (sourcePath.toFile.isFile) {
            Files.copy(sourcePath, newPath)
          } else if (sourcePath.toFile.isDirectory) {
            newPath.toFile.mkdir()
          }

        } catch {
          case e: IOException =>
            throw new UncheckedIOException(e)
        }

      })

    } finally {
      stream.close()
    }
  }


  override def afterAll() {
    IO.deleteRecursively(temporaryFolder)
  }
}
