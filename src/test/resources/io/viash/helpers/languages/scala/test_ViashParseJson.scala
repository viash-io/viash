#!/usr/bin/env scala

//> using file ../../../../../../../main/resources/io/viash/languages/scala/ViashParseJson.scala

/*
 * Unit test for ViashJsonParser.parseJson function
 * This test verifies that the Scala JSON parser correctly parses JSON content
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
    case (e: Double, a: Int) => e == a.toDouble
    case (e: Int, a: Double) => e.toDouble == a
    case (e, a) => e == a
  }
  
  if (isEqual) {
    println(s"${Colors.GREEN}PASS${Colors.NC}")
    testsPass += 1
  } else {
    println(s"${Colors.RED}FAIL${Colors.NC}")
    println(s"  Expected: $expected (${expected.getClass.getSimpleName})")
    println(s"  Actual:   $actual (${actual.getClass.getSimpleName})")
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

def testType(testName: String, expectedType: String, actual: Any): Unit = {
  print(s"Testing $testName type... ")
  
  val actualType = actual match {
    case _: String => "String"
    case _: Int => "Int"
    case _: Double => "Double"
    case _: Boolean => "Boolean"
    case _: List[_] => "List"
    case _: Map[_, _] => "Map"
    case null => "Null"
    case _ => actual.getClass.getSimpleName
  }
  
  val isEqual = expectedType == actualType
  
  if (isEqual) {
    println(s"${Colors.GREEN}PASS${Colors.NC}")
    testsPass += 1
  } else {
    println(s"${Colors.RED}FAIL${Colors.NC}")
    println(s"  Expected type: $expectedType")
    println(s"  Actual type:   $actualType")
    testsFail += 1
  }
}

@main def main(): Unit = {
  println("Running ViashJsonParser Scala unit tests...")
  println()

  // Test JSON content
  val jsonContent = """{
  "par": {
    "input": "file.txt",
    "number": 42,
    "flag": true,
    "empty_value": "",
    "array_simple": ["a", "b", "c"],
    "array_numbers": [1, 2, 3],
    "array_mixed": ["text", 123, true],
    "nested": {
      "level1": {
        "level2": "deep_value"
      }
    },
    "path_with_spaces": "/path/with spaces/file.txt",
    "quotes": "He said \"hello\"",
    "newlines": "line1\nline2",
    "tabs": "col1\tcol2",
    "string": "text",
    "integer": 42,
    "float": 3.14,
    "bool_true": true,
    "bool_false": false,
    "null_value": null,
    "empty_string": "",
    "zero": 0,
    "empty_array": [],
    "empty_object": {}
  },
  "meta": {
    "name": "test_component",
    "version": "1.0.0"
  },
  "simple_key": "simple_value",
  "number_key": 123,
  "bool_key": true
}"""

  // Parse the JSON
  val tempFile = java.nio.file.Files.createTempFile("viash-test", ".json")
  try {
    java.nio.file.Files.write(tempFile, jsonContent.getBytes("UTF-8"))
    val parsed = ViashJsonParser.parseJson(Some(tempFile.toString))
    val par = parsed("par").asInstanceOf[Map[String, Any]]
    val meta = parsed("meta").asInstanceOf[Map[String, Any]]

    // Test 1: Basic key-value pairs
    println("=== Test 1: Basic key-value pairs ===")
    runTest("par.input", "file.txt", par("input"))
    runTest("par.number", 42, par("number"))
    runTest("par.flag", true, par("flag"))
    runTest("par.empty_value", "", par("empty_value"))
    runTest("meta.name", "test_component", meta("name"))
    runTest("meta.version", "1.0.0", meta("version"))
    println()
  
    // Test 2: Arrays
    println("=== Test 2: Arrays ===")
    val arraySimple = par("array_simple").asInstanceOf[List[Any]]
    runTest("par.array_simple length", 3, arraySimple.length)
    runTest("par.array_simple[0]", "a", arraySimple(0))
    runTest("par.array_simple[1]", "b", arraySimple(1))
    runTest("par.array_simple[2]", "c", arraySimple(2))
    
    val arrayNumbers = par("array_numbers").asInstanceOf[List[Any]]
    runTest("par.array_numbers length", 3, arrayNumbers.length)
    runTest("par.array_numbers[0]", 1, arrayNumbers(0))
    runTest("par.array_numbers[1]", 2, arrayNumbers(1))
    runTest("par.array_numbers[2]", 3, arrayNumbers(2))
    
    val arrayMixed = par("array_mixed").asInstanceOf[List[Any]]
    runTest("par.array_mixed length", 3, arrayMixed.length)
    println()
  
    // Test 3: Nested structures
    println("=== Test 3: Nested structures ===")
    val nested = par("nested").asInstanceOf[Map[String, Any]]
    val level1 = nested("level1").asInstanceOf[Map[String, Any]]
    runTest("par.nested.level1.level2", "deep_value", level1("level2"))
    println()
  
    // Test 4: Quoted strings
    println("=== Test 4: Quoted strings ===")
    runTest("par.path_with_spaces", "/path/with spaces/file.txt", par("path_with_spaces"))
    runTest("par.quotes", "He said \"hello\"", par("quotes"))
    runTest("par.newlines", "line1\nline2", par("newlines"))
    runTest("par.tabs", "col1\tcol2", par("tabs"))
    println()
  
    // Test 5: Type conversions
    println("=== Test 5: Type conversions ===")
    testType("par.string", "String", par("string"))
    testType("par.integer", "Int", par("integer"))
    testType("par.float", "Double", par("float"))
    testType("par.bool_true", "Boolean", par("bool_true"))
    runTest("par.bool_false", false, par("bool_false"))
    runTest("par.null_value", null, par("null_value"))
    println()
  
    // Test 6: Root-level values
    println("=== Test 6: Root-level values ===")
    runTest("simple_key", "simple_value", parsed("simple_key"))
    runTest("number_key", 123, parsed("number_key"))
    runTest("bool_key", true, parsed("bool_key"))
    println()
  
    // Test 7: Edge cases
    println("=== Test 7: Edge cases ===")
    runTest("par.empty_string", "", par("empty_string"))
    runTest("par.zero", 0, par("zero"))
    val emptyArray = par("empty_array").asInstanceOf[List[Any]]
    runTest("par.empty_array length", 0, emptyArray.length)
    testType("par.empty_object", "Map", par("empty_object"))
    println()
    
    // Summary
    println("==================================================")
    println(s"Tests completed: ${testsPass + testsFail}")
    println(s"Tests passed: $testsPass")
    if (testsFail > 0) {
      println(s"${Colors.RED}Tests failed: $testsFail${Colors.NC}")
      sys.exit(1)
    } else {
      println(s"${Colors.GREEN}All tests passed!${Colors.NC}")
      sys.exit(0)
    }
  } finally {
    java.nio.file.Files.deleteIfExists(tempFile)
  }
}
