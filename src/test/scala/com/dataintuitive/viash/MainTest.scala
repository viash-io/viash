package com.dataintuitive.viash

import org.scalatest.FunSuite
import java.nio.file.{Path, Paths}

class MainTest extends FunSuite {
  
  def createTempDir(tmpName: String): String = {
  	val tmpDir = Paths.get(System.getProperty("java.io.tmpdir"))
  	val name: Path = tmpDir.getFileSystem.getPath(tmpName)
  	if (name.getParent != null) throw new IllegalArgumentException("Invalid prefix or suffix")
  	tmpDir.resolve(name).toString
  }
  
  val temporaryFolder = createTempDir("output_testpython")

  val params = Array(
      "-f", getClass.getResource("/testpython/functionality.yaml").getPath, 
      "-p", getClass.getResource("/testpython/platform_native.yaml").getPath, 
      "export",
      "-o", temporaryFolder
     )
  //println(params.mkString(" "))
  Main.main(params)
  
  
  val testpython = Paths.get(temporaryFolder, "testpython").toFile()

  test("Viash should have created an executable named testpython") {
    assert(testpython.exists())
    assert(testpython.canExecute())
  }

}