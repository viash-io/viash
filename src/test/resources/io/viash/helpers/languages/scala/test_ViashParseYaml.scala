#!/usr/bin/env scala

//> using file ../../../../../../../main/resources/io/viash/languages/scala/ViashParseYaml.scala

/*
 * Unit test for ViashYamlParser.parseYaml function
 * This test verifies that the Scala YAML parser correctly parses YAML content
 * and returns appropriate Scala data structures.
 */

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
  
  val isEqual = (expected, actual) match {
    case (None, None) => true
    case (Some(e), Some(a)) => e == a
    case (e, a) => e == a
  }
  
  if (isEqual) {
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
  
  val isEqual = expectedArray == actualArray
  
  if (isEqual) {
    println(s"${Colors.GREEN}PASS${Colors.NC}")
    testsPass += 1
  } else {
    println(s"${Colors.RED}FAIL${Colors.NC}")
    println(s"  Expected array: [${expectedArray.mkString(", ")}]")
    println(s"  Actual array:   [${actualArray.mkString(", ")}]")
    testsFail += 1
  }
}

@main def main(): Unit = {
  println("Running ViashYamlParser Scala unit tests...")
  println()
  
  // Test 1: Basic key-value pairs
  println("=== Test 1: Basic key-value pairs ===")
  val yamlContent1 = """
par:
  input: test.txt
  number: 42
  flag: true
  empty_value:
meta:
  name: test_script
  version: 1.0
"""
  
  val result1 = ViashYamlParser.parseYaml(Some(yamlContent1))
  val par1 = result1.get("par").map(_.asInstanceOf[Map[String, Any]]).getOrElse(Map.empty)
  val meta1 = result1.get("meta").map(_.asInstanceOf[Map[String, Any]]).getOrElse(Map.empty)
  
  runTest("par.input", "test.txt", par1.getOrElse("input", None))
  runTest("par.number", 42, par1.getOrElse("number", None))
  runTest("par.flag", true, par1.getOrElse("flag", None))
  runTest("par.empty_value", None, par1.getOrElse("empty_value", None))
  runTest("meta.name", "test_script", meta1.getOrElse("name", None))
  runTest("meta.version", 1.0, meta1.getOrElse("version", None))
  println()

  // Test 2: Arrays
  println("=== Test 2: Arrays ===")
  val yamlContent2 = """
par:
  files:
    - file1.txt
    - file2.txt
    - file3.txt
  numbers:
    - 1
    - 2
    - 3
"""
  
  val result2 = ViashYamlParser.parseYaml(Some(yamlContent2))
  val par2 = result2.get("par").map(_.asInstanceOf[Map[String, Any]]).getOrElse(Map.empty)
  
  testArray("par.files", List("file1.txt", "file2.txt", "file3.txt"), par2.get("files").map(_.asInstanceOf[List[Any]]).getOrElse(List.empty))
  testArray("par.numbers", List(1, 2, 3), par2.get("numbers").map(_.asInstanceOf[List[Any]]).getOrElse(List.empty))
  println()

  // Test 3: Quoted strings
  println("=== Test 3: Quoted strings ===")
  val yamlContent3 = """
par:
  quoted_string: "This is a quoted string"
  single_quoted: 'Single quoted string'
  with_newline: "Line 1\nLine 2"
"""
  
  val result3 = ViashYamlParser.parseYaml(Some(yamlContent3))
  val par3 = result3.get("par").map(_.asInstanceOf[Map[String, Any]]).getOrElse(Map.empty)
  
  runTest("par.quoted_string", "This is a quoted string", par3.getOrElse("quoted_string", None))
  runTest("par.single_quoted", "Single quoted string", par3.getOrElse("single_quoted", None))
  runTest("par.with_newline", "Line 1\nLine 2", par3.getOrElse("with_newline", None))
  println()

  // Test 4: Boolean and numeric types
  println("=== Test 4: Boolean and numeric types ===")
  val yamlContent4 = """
par:
  bool_true: true
  bool_false: false
  integer: 123
  negative: -45
  float_val: 3.14
  negative_float: -2.71
"""
  
  val result4 = ViashYamlParser.parseYaml(Some(yamlContent4))
  val par4 = result4.get("par").map(_.asInstanceOf[Map[String, Any]]).getOrElse(Map.empty)
  
  runTest("par.bool_true", true, par4.getOrElse("bool_true", None))
  runTest("par.bool_false", false, par4.getOrElse("bool_false", None))
  runTest("par.integer", 123, par4.getOrElse("integer", None))
  runTest("par.negative", -45, par4.getOrElse("negative", None))
  runTest("par.float_val", 3.14, par4.getOrElse("float_val", None))
  runTest("par.negative_float", -2.71, par4.getOrElse("negative_float", None))
  println()

  // Test 5: Root level parsing
  println("=== Test 5: Root level parsing ===")
  val yamlContent5 = """
simple_key: simple_value
number_key: 999
bool_key: false
"""
  
  val result5 = ViashYamlParser.parseYaml(Some(yamlContent5))
  
  runTest("simple_key", "simple_value", result5.getOrElse("simple_key", None))
  runTest("number_key", 999, result5.getOrElse("number_key", None))
  runTest("bool_key", false, result5.getOrElse("bool_key", None))
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
  val par6 = result6.get("par").map(_.asInstanceOf[Map[String, Any]]).getOrElse(Map.empty)
  val meta6 = result6.get("meta").map(_.asInstanceOf[Map[String, Any]]).getOrElse(Map.empty)
  
  runTest("par.empty_string", "", par6.getOrElse("empty_string", None))
  runTest("par.zero", 0, par6.getOrElse("zero", None))
  runTest("par.empty_array", None, par6.getOrElse("empty_array", None))
  runTest("meta.empty_section", None, meta6.getOrElse("empty_section", None))
  println()

  // Print results
  println("=" * 50)
  println(s"Tests completed: ${testsPass + testsFail}")
  println(s"Tests passed: $testsPass")
  if (testsFail > 0) {
    println(s"Tests failed: $testsFail")
  } else {
    println("All tests passed!")
  }
  
  if (testsFail > 0) {
    sys.exit(1)
  }
}
