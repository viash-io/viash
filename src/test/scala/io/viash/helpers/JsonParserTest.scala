package io.viash.helpers

import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import java.nio.file.{Files, Paths, Path, StandardCopyOption}

/**
 * Test suite for language-specific JSON parsers.
 * Tests the ViashParseJson functions for Bash, Python, R, JavaScript, C#, and Scala.
 */
class JsonParserTest extends AnyFunSuite with BeforeAndAfterAll {
  Logger.UseColorOverride.value = Some(false)
  
  private val projectRoot = Paths.get(System.getProperty("user.dir"))
  
  // Helper to set up temp dir with correct structure for all language parsers
  private def setupTempDirWithParser(
    language: String,
    parserFileName: String
  ): (Path, Path) = {
    setupTempDirWithParserFiles(
      language,
      s"test_ViashParseJson.$parserFileName",
      List(s"ViashParseJson.$parserFileName")
    )
  }

  // Helper to set up temp dir with explicit test and parser file names
  private def setupTempDirWithParserFiles(
    language: String,
    testFileName: String,
    parserFileNames: List[String]
  ): (Path, Path) = {
    val tempDir = Files.createTempDirectory("viash_json_parser_test_")
    
    val testScriptSrc = projectRoot.resolve(s"src/test/resources/io/viash/helpers/languages/$language/$testFileName")
    val testSubPath = s"src/test/resources/io/viash/helpers/languages/$language/$testFileName"
    
    // Create directory structure for test file
    val testDir = tempDir.resolve(testSubPath).getParent
    Files.createDirectories(testDir)
    val testScript = tempDir.resolve(testSubPath)
    Files.copy(testScriptSrc, testScript, StandardCopyOption.REPLACE_EXISTING)
    
    // Create directory structure for parser file(s)
    for (parserFileName <- parserFileNames) {
      val parserScriptSrc = projectRoot.resolve(s"src/main/resources/io/viash/languages/$language/$parserFileName")
      val parserSubPath = s"src/main/resources/io/viash/languages/$language/$parserFileName"
      val parserDir = tempDir.resolve(parserSubPath).getParent
      Files.createDirectories(parserDir)
      val parserScript = tempDir.resolve(parserSubPath)
      Files.copy(parserScriptSrc, parserScript, StandardCopyOption.REPLACE_EXISTING)
    }
    
    (tempDir, testScript)
  }
  
  private def cleanupTempDir(tempDir: Path): Unit = {
    import scala.jdk.CollectionConverters._
    Files.walk(tempDir).iterator().asScala.toList.reverse.foreach(Files.delete)
  }

  test("Bash JSON parser") {
    val (tempDir, testScript) = setupTempDirWithParser("bash", "sh")
    
    try {
      val result = Exec.runCatchPath(
        List("bash", testScript.toString),
        cwd = Some(tempDir)
      )
      
      assert(result.exitValue == 0, s"Bash JSON parser test failed:\n${result.output}")
    } finally {
      cleanupTempDir(tempDir)
    }
  }

  test("Python JSON parser") {
    val (tempDir, testScript) = setupTempDirWithParser("python", "py")
    
    try {
      val result = Exec.runCatchPath(
        List("python3", testScript.toString),
        cwd = Some(tempDir)
      )
      
      assert(result.exitValue == 0, s"Python JSON parser test failed:\n${result.output}")
    } finally {
      cleanupTempDir(tempDir)
    }
  }

  test("R JSON parser (hybrid)") {
    val (tempDir, testScript) = setupTempDirWithParserFiles(
      "r",
      "test_ViashParseJsonHybrid.R",
      List("ViashParseJsonHybrid.R")
    )
    
    try {
      val result = Exec.runCatchPath(
        List("Rscript", testScript.toString),
        cwd = Some(tempDir)
      )
      
      assert(result.exitValue == 0, s"R JSON parser (hybrid) test failed:\n${result.output}")
    } finally {
      cleanupTempDir(tempDir)
    }
  }

  test("R JSON parser (jsonlite)") {
    // Check if jsonlite is available
    val jsonliteCheck = Exec.runCatch(List("Rscript", "-e", "library(jsonlite)"))
    assume(jsonliteCheck.exitValue == 0, "jsonlite not available, skipping jsonlite-only R test")

    val (tempDir, testScript) = setupTempDirWithParserFiles(
      "r",
      "test_ViashParseJson.R",
      List("ViashParseJson.R")
    )
    
    try {
      val result = Exec.runCatchPath(
        List("Rscript", testScript.toString),
        cwd = Some(tempDir)
      )
      
      assert(result.exitValue == 0, s"R JSON parser (jsonlite) test failed:\n${result.output}")
    } finally {
      cleanupTempDir(tempDir)
    }
  }

  test("JavaScript JSON parser") {
    val (tempDir, testScript) = setupTempDirWithParser("javascript", "js")
    
    try {
      val result = Exec.runCatchPath(
        List("node", testScript.toString),
        cwd = Some(tempDir)
      )
      
      assert(result.exitValue == 0, s"JavaScript JSON parser test failed:\n${result.output}")
    } finally {
      cleanupTempDir(tempDir)
    }
  }

  test("C# JSON parser") {
    // Check if dotnet-script is available (either as 'dotnet script' or 'dotnet-script')
    val dotnetScriptCheck = Exec.runCatch(List("dotnet-script", "--version"))
    val dotnetCheck = Exec.runCatch(List("dotnet", "script", "--version"))
    val useDotnetScript = dotnetScriptCheck.exitValue == 0
    val useDotnetCmd = dotnetCheck.exitValue == 0
    assume(useDotnetScript || useDotnetCmd, "dotnet script not available, skipping C# test")
    
    val (tempDir, testScript) = setupTempDirWithParser("csharp", "csx")
    
    try {
      val cmd = if (useDotnetScript) List("dotnet-script", testScript.toString) else List("dotnet", "script", testScript.toString)
      val result = Exec.runCatchPath(
        cmd,
        cwd = Some(tempDir)
      )
      
      assert(result.exitValue == 0, s"C# JSON parser test failed:\n${result.output}")
    } finally {
      cleanupTempDir(tempDir)
    }
  }

  test("Scala JSON parser") {
    // Check if scala is available and can actually compile
    // Scala 2.x has known issues with Java 17+ due to missing javax.tools classes
    val scalaCheck = Exec.runCatch(List("scala", "-e", "println(\"hello\")"))
    assume(scalaCheck.exitValue == 0, s"scala not available or incompatible with current JDK, skipping Scala test. Output: ${scalaCheck.output}")
    
    val (tempDir, testScript) = setupTempDirWithParser("scala", "scala")
    
    try {
      val result = Exec.runCatchPath(
        List("scala", testScript.toString),
        cwd = Some(tempDir)
      )
      
      assert(result.exitValue == 0, s"Scala JSON parser test failed:\n${result.output}")
    } finally {
      cleanupTempDir(tempDir)
    }
  }
}
