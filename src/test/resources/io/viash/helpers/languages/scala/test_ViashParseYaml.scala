#!/usr/bin/env scala

/*
 * Unit test for ViashYamlParser.parseYaml function
 * This test verifies that the Scala YAML parser correctly parses YAML content
 * and returns appropriate Scala data structures.
 */

import scala.io.Source
import scala.util.{Try, Success, Failure}
import java.io.File

// Load the YAML parser
val scriptDir = new File(sys.props("user.dir")).toPath.resolve("src/test/resources/io/viash/helpers/languages/scala").toString
val yamlParserPath = new File(scriptDir).toPath.resolve("../../../../main/resources/io/viash/languages/scala/ViashParseYaml.scala").toString
val yamlParserCode = Source.fromFile(yamlParserPath).mkString

// Evaluate the YAML parser code to get the ViashYamlParser object
val interpreter = new scala.tools.nsc.interpreter.IMain()
interpreter.interpret(yamlParserCode)

// Colors for test output
object Colors {
  val RED = "\u001b[0;31m"
  val GREEN = "\u001b[0;32m"
  val YELLOW = "\u001b[1;33m"
  val NC = "\u001b[0m" // No Color
}

// Test counters
var testsPass = 0
var testsFail = 0

def runTest(testName: String, expected: Any, actual: Any): Unit = {
  print(s"Testing $testName... ")
  
  if (actual == expected) {
    println(s"${Colors.GREEN}PASS${Colors.NC}")
    testsPass += 1
  } else {
    println(s"${Colors.RED}FAIL${Colors.NC}")
    println(s"  Expected: $expected")
    println(s"  Actual:   $actual")
    testsFail += 1
  }
}

def testArray(testName: String, expectedArray: List[Any], actualArray: List[Any]): Unit = {
  print(s"Testing $testName... ")
  
  if (actualArray == expectedArray) {
    println(s"${Colors.GREEN}PASS${Colors.NC}")
    testsPass += 1
  } else {
    println(s"${Colors.RED}FAIL${Colors.NC}")
    println(s"  Expected array: $expectedArray")
    println(s"  Actual array:   $actualArray")
    testsFail += 1
  }
}

def main(): Unit = {
  println(s"${Colors.YELLOW}Running ViashYamlParser Scala unit tests...${Colors.NC}")
  println()

  // Test 1: Basic key-value pairs in sections
  println("=== Test 1: Basic key-value pairs ===")
  val yamlContent1 = """
par:
  input: "/path/to/input.txt"
  number: 42
  flag: true
  empty_value: null
meta:
  name: "test_component"
  version: "1.0.0"
"""

  val result1 = ViashYamlParser.parseYaml(Some(yamlContent1))
  
  // Test the results
  val par1 = result1.get("par").asInstanceOf[Option[Map[String, Any]]].getOrElse(Map())
  val meta1 = result1.get("meta").asInstanceOf[Option[Map[String, Any]]].getOrElse(Map())
  
  runTest("par.input", "/path/to/input.txt", par1.get("input"))
  runTest("par.number", 42, par1.get("number"))
  runTest("par.flag", true, par1.get("flag"))
  runTest("par.empty_value", None, par1.get("empty_value"))
  runTest("meta.name", "test_component", meta1.get("name"))
  runTest("meta.version", "1.0.0", meta1.get("version"))
  println()

  // Test 2: Arrays in sections
  println("=== Test 2: Arrays ===")
  val yamlContent2 = """
par:
  files:
    - "file1.txt"
    - "file2.txt" 
    - "file3.txt"
  numbers:
    - 1
    - 2
    - 3
"""

  val result2 = ViashYamlParser.parseYaml(Some(yamlContent2))
  val par2 = result2.get("par").asInstanceOf[Option[Map[String, Any]]].getOrElse(Map())
  
  testArray("par.files", List("file1.txt", "file2.txt", "file3.txt"), 
             par2.get("files").asInstanceOf[Option[List[Any]]].getOrElse(List()))
  testArray("par.numbers", List(1, 2, 3), 
             par2.get("numbers").asInstanceOf[Option[List[Any]]].getOrElse(List()))
  println()

  // Test 3: Quoted strings and special characters  
  println("=== Test 3: Quoted strings ===")
  val yamlContent3 = """
par:
  quoted_string: "Hello \"World\""
  single_quoted: "Single quotes"
  with_newline: "Line 1\nLine 2"
"""

  val result3 = ViashYamlParser.parseYaml(Some(yamlContent3))
  val par3 = result3.get("par").asInstanceOf[Option[Map[String, Any]]].getOrElse(Map())
  
  runTest("par.quoted_string", "Hello \"World\"", par3.get("quoted_string"))
  runTest("par.single_quoted", "Single quotes", par3.get("single_quoted"))
  runTest("par.with_newline", "Line 1\nLine 2", par3.get("with_newline"))
  println()

  // Test 4: Boolean and numeric types
  println("=== Test 4: Boolean and numeric types ===")
  val yamlContent4 = """
par:
  bool_true: true
  bool_false: false
  integer: 123
  negative: -456
  float_val: 3.14
  negative_float: -2.5
"""

  val result4 = ViashYamlParser.parseYaml(Some(yamlContent4))
  val par4 = result4.get("par").asInstanceOf[Option[Map[String, Any]]].getOrElse(Map())
  
  runTest("par.bool_true", true, par4.get("bool_true"))
  runTest("par.bool_false", false, par4.get("bool_false"))
  runTest("par.integer", 123, par4.get("integer"))
  runTest("par.negative", -456, par4.get("negative"))
  runTest("par.float_val", 3.14, par4.get("float_val"))
  runTest("par.negative_float", -2.5, par4.get("negative_float"))
  println()

  // Test 5: Root level parsing (no sections)
  println("=== Test 5: Root level parsing ===")
  val yamlContent5 = """
simple_key: simple_value
number_key: 789
bool_key: true
"""

  val result5 = ViashYamlParser.parseYaml(Some(yamlContent5))
  
  runTest("simple_key", "simple_value", result5.get("simple_key"))
  runTest("number_key", 789, result5.get("number_key"))
  runTest("bool_key", true, result5.get("bool_key"))
  println()

  // Test 6: Edge cases
  println("=== Test 6: Edge cases ===")
  val yamlContent6 = """
par:
  empty_string: ""
  zero: 0
  empty_array:
meta:
  empty_section:
"""

  val result6 = ViashYamlParser.parseYaml(Some(yamlContent6))
  val par6 = result6.get("par").asInstanceOf[Option[Map[String, Any]]].getOrElse(Map())
  val meta6 = result6.get("meta").asInstanceOf[Option[Map[String, Any]]].getOrElse(Map())
  
  runTest("par.empty_string", "", par6.get("empty_string"))
  runTest("par.zero", 0, par6.get("zero"))
  testArray("par.empty_array", List(), par6.get("empty_array").asInstanceOf[Option[List[Any]]].getOrElse(List()))
  runTest("meta.empty_section", None, meta6.get("empty_section"))
  println()

  // Print summary
  val totalTests = testsPass + testsFail
  println("=" * 50)
  println(s"Tests completed: $totalTests")
  println(s"${Colors.GREEN}Tests passed: $testsPass${Colors.NC}")
  if (testsFail > 0) {
    println(s"${Colors.RED}Tests failed: $testsFail${Colors.NC}")
    sys.exit(1)
  } else {
    println(s"${Colors.GREEN}All tests passed!${Colors.NC}")
  }
}

main()
