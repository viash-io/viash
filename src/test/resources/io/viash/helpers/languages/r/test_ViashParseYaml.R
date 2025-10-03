#!/usr/bin/env Rscript

# Unit test for viash_parse_yaml function
# This test verifies that the R YAML parser correctly parses YAML content
# and returns appropriate R data structures.

# Source the YAML parser function
yaml_parser_path <- "src/main/resources/io/viash/languages/r/ViashParseYaml.R"
source(yaml_parser_path)

# Colors for test output (using cat with ANSI codes)
RED <- '\033[0;31m'
GREEN <- '\033[0;32m'
YELLOW <- '\033[1;33m'
NC <- '\033[0m' # No Color

# Test counters
tests_passed <- 0
tests_failed <- 0

# Function to run a test
run_test <- function(test_name, expected, actual) {
  cat(sprintf("Testing %s... ", test_name))
  
  if (identical(actual, expected)) {
    cat(sprintf("%sPASS%s\n", GREEN, NC))
    tests_passed <<- tests_passed + 1
  } else {
    cat(sprintf("%sFAIL%s\n", RED, NC))
    cat(sprintf("  Expected: %s\n", deparse(expected)))
    cat(sprintf("  Actual:   %s\n", deparse(actual)))
    tests_failed <<- tests_failed + 1
  }
}

# Function to test array
test_array <- function(test_name, expected_array, actual_array) {
  cat(sprintf("Testing %s... ", test_name))
  
  if (identical(actual_array, expected_array)) {
    cat(sprintf("%sPASS%s\n", GREEN, NC))
    tests_passed <<- tests_passed + 1
  } else {
    cat(sprintf("%sFAIL%s\n", RED, NC))
    cat(sprintf("  Expected array: %s\n", deparse(expected_array)))
    cat(sprintf("  Actual array:   %s\n", deparse(actual_array)))
    tests_failed <<- tests_failed + 1
  }
}

# Main test function
main <- function() {
  cat(sprintf("%sRunning viash_parse_yaml R unit tests...%s\n", YELLOW, NC))
  cat("\n")

  # Test 1: Basic key-value pairs in sections
  cat("=== Test 1: Basic key-value pairs ===\n")
  yaml_content <- "
par:
  input: \"/path/to/input.txt\"
  number: 42
  flag: true
  empty_value: null
meta:
  name: \"test_component\"
  version: \"1.0.0\"
"

  result <- viash_parse_yaml(yaml_content)
  
  # Test the results
  run_test("par$input", "/path/to/input.txt", result$par$input)
  run_test("par$number", 42L, result$par$number)
  run_test("par$flag", TRUE, result$par$flag)
  run_test("par$empty_value", NULL, result$par$empty_value)
  run_test("meta$name", "test_component", result$meta$name)
  run_test("meta$version", "1.0.0", result$meta$version)
  cat("\n")

  # Test 2: Arrays in sections
  cat("=== Test 2: Arrays ===\n")
  yaml_content <- "
par:
  files:
    - \"file1.txt\"
    - \"file2.txt\" 
    - \"file3.txt\"
  numbers:
    - 1
    - 2
    - 3
"

  result <- viash_parse_yaml(yaml_content)
  
  test_array("par$files", c("file1.txt", "file2.txt", "file3.txt"), result$par$files)
  test_array("par$numbers", c(1L, 2L, 3L), result$par$numbers)
  cat("\n")

  # Test 3: Quoted strings and special characters  
  cat("=== Test 3: Quoted strings ===\n")
  yaml_content <- '
par:
  quoted_string: "Hello \"World\""
  single_quoted: "Single quotes"
  with_newline: "Line 1\\nLine 2"
'

  result <- viash_parse_yaml(yaml_content)
  
  run_test("par$quoted_string", 'Hello "World"', result$par$quoted_string)
  run_test("par$single_quoted", "Single quotes", result$par$single_quoted)
  run_test("par$with_newline", "Line 1\nLine 2", result$par$with_newline)
  cat("\n")

  # Test 4: Boolean and numeric types
  cat("=== Test 4: Boolean and numeric types ===\n")
  yaml_content <- "
par:
  bool_true: true
  bool_false: false
  integer: 123
  negative: -456
  float_val: 3.14
  negative_float: -2.5
"

  result <- viash_parse_yaml(yaml_content)
  
  run_test("par$bool_true", TRUE, result$par$bool_true)
  run_test("par$bool_false", FALSE, result$par$bool_false)
  run_test("par$integer", 123L, result$par$integer)
  run_test("par$negative", -456L, result$par$negative)
  run_test("par$float_val", 3.14, result$par$float_val)
  run_test("par$negative_float", -2.5, result$par$negative_float)
  cat("\n")

  # Test 5: Root level parsing (no sections)
  cat("=== Test 5: Root level parsing ===\n")
  yaml_content <- "
simple_key: simple_value
number_key: 789
bool_key: true
"

  result <- viash_parse_yaml(yaml_content)
  
  run_test("simple_key", "simple_value", result$simple_key)
  run_test("number_key", 789L, result$number_key)
  run_test("bool_key", TRUE, result$bool_key)
  cat("\n")

  # Test 6: Edge cases
  cat("=== Test 6: Edge cases ===\n")
  yaml_content <- "
par:
  empty_string: \"\"
  zero: 0
  empty_array:
meta:
  empty_section:
"

  result <- viash_parse_yaml(yaml_content)
  
  run_test("par$empty_string", "", result$par$empty_string)
  run_test("par$zero", 0L, result$par$zero)
  run_test("par$empty_array (NULL)", NULL, result$par$empty_array)
  run_test("meta$empty_section (NULL)", NULL, result$meta$empty_section)
  cat("\n")

  # Print summary
  total_tests <- tests_passed + tests_failed
  cat(paste(rep("=", 50), collapse=""), "\n")
  cat(sprintf("Tests completed: %d\n", total_tests))
  cat(sprintf("%sTests passed: %d%s\n", GREEN, tests_passed, NC))
  if (tests_failed > 0) {
    cat(sprintf("%sTests failed: %d%s\n", RED, tests_failed, NC))
    quit(status = 1)
  } else {
    cat(sprintf("%sAll tests passed!%s\n", GREEN, NC))
  }
}

# Run the tests
main()
