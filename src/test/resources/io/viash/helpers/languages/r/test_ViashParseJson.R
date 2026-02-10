#!/usr/bin/env Rscript

# Test suite for viash_parse_json R function
cat("Running viash_parse_json R unit tests...\n\n")

# Source the parser
source("src/main/resources/io/viash/languages/r/ViashParseJson.R")

# Test helper functions
RED <- "\033[31m"
GREEN <- "\033[32m"
RESET <- "\033[0m"

test_passed <- 0
test_failed <- 0

test_equal <- function(description, actual, expected) {
  if (identical(actual, expected)) {
    cat(sprintf("%sPASS%s: %s\n", GREEN, RESET, description))
    test_passed <<- test_passed + 1
    return(TRUE)
  } else {
    cat(sprintf("%sFAIL%s: %s\n", RED, RESET, description))
    cat("  Expected:", deparse(expected), "\n")
    cat("  Got:", deparse(actual), "\n")
    test_failed <<- test_failed + 1
    return(FALSE)
  }
}

# Create test JSON file
test_json <- tempfile(fileext = ".json")
writeLines('{
  "par": {
    "input": "file.txt",
    "number": 42,
    "flag": true,
    "empty_value": "",
    "array_simple": ["a", "b", "c"],
    "array_numbers": [1, 2, 3],
    "array_mixed": ["text", 123, true, null],
    "nested": {
      "level1": {
        "level2": "deep_value"
      }
    },
    "path_with_spaces": "/path/with spaces/file.txt",
    "quotes": "She said \\"hello\\"",
    "newlines": "line1\\nline2",
    "tabs": "col1\\tcol2",
    "string": "text",
    "integer": 123,
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
    "version": "1.0"
  },
  "simple_key": "value",
  "number_key": 99,
  "bool_key": false
}', test_json)

# Set environment variable
Sys.setenv(VIASH_WORK_PARAMS = test_json)

# Parse the JSON
data <- viash_parse_json()

cat("=== Test 1: Basic key-value pairs ===\n")
test_equal("par$input", data$par$input, "file.txt")
test_equal("par$number", data$par$number, 42L)
test_equal("par$flag", data$par$flag, TRUE)
test_equal("par$empty_value", data$par$empty_value, "")
test_equal("meta$name", data$meta$name, "test_component")
test_equal("meta$version", data$meta$version, "1.0")

cat("\n=== Test 2: Arrays ===\n")
test_equal("par$array_simple", data$par$array_simple, c("a", "b", "c"))
test_equal("par$array_numbers", data$par$array_numbers, c(1L, 2L, 3L))
test_equal("par$array_mixed length", length(data$par$array_mixed), 4L)

cat("\n=== Test 3: Nested structures ===\n")
test_equal("par$nested$level1$level2", data$par$nested$level1$level2, "deep_value")

cat("\n=== Test 4: Quoted strings ===\n")
test_equal("par$path_with_spaces", data$par$path_with_spaces, "/path/with spaces/file.txt")
test_equal("par$quotes", data$par$quotes, "She said \"hello\"")
test_equal("par$newlines", data$par$newlines, "line1\nline2")
test_equal("par$tabs", data$par$tabs, "col1\tcol2")

cat("\n=== Test 5: Type conversions ===\n")
test_equal("par$string type", is.character(data$par$string), TRUE)
test_equal("par$integer type", is.numeric(data$par$integer), TRUE)
test_equal("par$float type", is.numeric(data$par$float), TRUE)
test_equal("par$bool_true type", is.logical(data$par$bool_true), TRUE)
test_equal("par$bool_false", data$par$bool_false, FALSE)
test_equal("par$null_value", data$par$null_value, NULL)

cat("\n=== Test 6: Root-level values ===\n")
test_equal("simple_key", data$simple_key, "value")
test_equal("number_key", data$number_key, 99L)
test_equal("bool_key", data$bool_key, FALSE)

cat("\n=== Test 7: Edge cases ===\n")
test_equal("par$empty_string", data$par$empty_string, "")
test_equal("par$zero", data$par$zero, 0L)
test_equal("par$empty_array length", length(data$par$empty_array), 0L)
test_equal("par$empty_object type", is.list(data$par$empty_object), TRUE)

# Clean up
unlink(test_json)

# Print summary
cat("\n==================================================\n")
cat(sprintf("Tests completed: %d\n", test_passed + test_failed))
cat(sprintf("Tests passed: %d\n", test_passed))
if (test_failed > 0) {
  cat(sprintf("%sTests failed: %d%s\n", RED, test_failed, RESET))
  quit(status = 1)
} else {
  cat(sprintf("%sAll tests passed!%s\n", GREEN, RESET))
}
