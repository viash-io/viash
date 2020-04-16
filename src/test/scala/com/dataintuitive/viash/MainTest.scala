package com.dataintuitive.viash

import org.scalatest.FunSuite
import java.nio.file.{Path, Paths}
import java.io.File
import sys.process.Process
import com.dataintuitive.viash.functionality.Functionality
import scala.io.Source

class MainTest extends FunSuite {
  // which platform to test
  val testName = "testpython"
  val funcFile = getClass.getResource(s"/$testName/functionality.yaml").getPath
  val platFile = getClass.getResource(s"/$testName/platform_native.yaml").getPath
  
  // create a new temporary directory to store files
  def createTempDir(tmpName: String): File = {
  	val tmpDir = Paths.get(System.getProperty("java.io.tmpdir"))
  	val name: Path = tmpDir.getFileSystem.getPath(tmpName)
  	if (name.getParent != null) throw new IllegalArgumentException("Invalid prefix or suffix")
  	tmpDir.resolve(name).toFile()
  }
  
  val temporaryFolder = createTempDir("output_testpython")
  val tempFolStr = temporaryFolder.toString()
  
  // parse functionality from file
  val functionality = Functionality.parse(new File(funcFile))

  // convert testpython 
  val params = Array(
    "-f", funcFile, 
    "-p", platFile, 
    "export",
    "-o", tempFolStr
   )
  Main.main(params)
  
  // check whether executable was created
  val testpython = Paths.get(tempFolStr, "testpython").toFile()

  test("Viash should have created an executable named testpython") {
    assert(testpython.exists())
    assert(testpython.canExecute())
  }
  
  test("Check whether the executable can run") {
    Process(
      Seq(testpython.toString(), "--help"), 
      temporaryFolder
    ).!!
  }
  
  test("Check whether particular keywords can be found in the usage") {
    val stdout = 
      Process(
        Seq(testpython.toString(), "--help"), 
        temporaryFolder
      ).!!
    
    functionality.arguments.foreach(arg => {
      assert(stdout.contains(arg.name))
      for (opt <- arg.alternatives; value <- opt) 
        assert(stdout.contains(value))
      for (opt <- arg.description; value <- opt) 
        assert(stdout.contains(value))
    })
    
  }
  
  test("Check whether output is correctly created") {
    val output = Paths.get(tempFolStr, "output.txt").toFile()
    val log = Paths.get(tempFolStr, "log.txt").toFile()
    
    val inputStr = Paths.get(tempFolStr, "testpython").toString()
    val stdout = 
      Process(
        Seq(
          testpython.toString(), 
          inputStr,
          "--real_number", "10.5",
          "--whole_number", "10",
//          "-s", "a string with a few spaces",
          "-s", "help",
          "--truth",
          "--output", output.toString(),
          "--log", log.toString(),
          "--optional", "foo"
        ), 
        temporaryFolder
      ).!!
    
    assert(output.exists())
    assert(log.exists())
    
    val outputLines = Source.fromFile(output).mkString
    assert(outputLines.contains(s""""input": "$inputStr""""))
    assert(outputLines.contains(""""real_number": 10.5"""))
    assert(outputLines.contains(""""whole_number": 10"""))
//    assert(outputLines.contains(""""s": "a string with a few spaces""""))
    assert(outputLines.contains(""""truth": true"""))
    assert(outputLines.contains(s""""output": "${output.toString()}""""))
    assert(outputLines.contains(s""""log": "${log.toString()}""""))
    
    val logLines = Source.fromFile(log).mkString
    assert(logLines.contains("INFO:root:Parsed input arguments"))
  }

}