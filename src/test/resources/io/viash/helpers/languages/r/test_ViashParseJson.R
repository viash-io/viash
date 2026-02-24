#!/usr/bin/env Rscript

# Test suite for viash_parse_json R function using jsonlite
cat("Running viash_parse_json jsonlite-only unit tests...\n\n")

# Source the jsonlite-only parser
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

# Check that jsonlite is available
if (!requireNamespace("jsonlite", quietly = TRUE)) {
  cat("SKIP: jsonlite is not installed, skipping jsonlite-only tests.\n")
  quit(status = 0)
}

## ===================================================================
## TEST 1: Basic key-value pairs
## ===================================================================
cat("=== Test 1: Basic key-value pairs ===\n")

test_json <- tempfile(fileext = ".json")
writeLines('{
  "par": {
    "input": "file.txt",
    "number": 42,
    "flag": true,
    "empty_value": "",
    "array_simple": ["a", "b", "c"],
    "array_numbers": [1, 2, 3],
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

Sys.setenv(VIASH_WORK_PARAMS = test_json)
data <- viash_parse_json()
unlink(test_json)

test_equal("par$input", data$par$input, "file.txt")
test_equal("par$number", data$par$number, 42L)
test_equal("par$flag", data$par$flag, TRUE)
test_equal("par$empty_value", data$par$empty_value, "")
test_equal("meta$name", data$meta$name, "test_component")
test_equal("meta$version", data$meta$version, "1.0")

## ===================================================================
## TEST 2: Arrays
## ===================================================================
cat("\n=== Test 2: Arrays ===\n")
test_equal("par$array_simple", data$par$array_simple, c("a", "b", "c"))
test_equal("par$array_numbers", data$par$array_numbers, c(1L, 2L, 3L))

## ===================================================================
## TEST 3: Nested structures
## ===================================================================
cat("\n=== Test 3: Nested structures ===\n")
test_equal("par$nested$level1$level2", data$par$nested$level1$level2, "deep_value")

## ===================================================================
## TEST 4: Quoted strings
## ===================================================================
cat("\n=== Test 4: Quoted strings ===\n")
test_equal("par$path_with_spaces", data$par$path_with_spaces, "/path/with spaces/file.txt")
test_equal("par$quotes", data$par$quotes, "She said \"hello\"")
test_equal("par$newlines", data$par$newlines, "line1\nline2")
test_equal("par$tabs", data$par$tabs, "col1\tcol2")

## ===================================================================
## TEST 5: Type conversions
## ===================================================================
cat("\n=== Test 5: Type conversions ===\n")
test_equal("par$string type", is.character(data$par$string), TRUE)
test_equal("par$integer type", is.numeric(data$par$integer), TRUE)
test_equal("par$float type", is.numeric(data$par$float), TRUE)
test_equal("par$bool_true type", is.logical(data$par$bool_true), TRUE)
test_equal("par$bool_false", data$par$bool_false, FALSE)
test_equal("par$null_value", data$par$null_value, NULL)

## ===================================================================
## TEST 6: Root-level values
## ===================================================================
cat("\n=== Test 6: Root-level values ===\n")
test_equal("simple_key", data$simple_key, "value")
test_equal("number_key", data$number_key, 99L)
test_equal("bool_key", data$bool_key, FALSE)

## ===================================================================
## TEST 7: Edge cases
## ===================================================================
cat("\n=== Test 7: Edge cases ===\n")
test_equal("par$empty_string", data$par$empty_string, "")
test_equal("par$zero", data$par$zero, 0L)
test_equal("par$empty_array length", length(data$par$empty_array), 0L)
test_equal("par$empty_object type", is.list(data$par$empty_object), TRUE)

## ===================================================================
## TEST 8: Special characters
## ===================================================================
cat("\n=== Test 8: Special characters ===\n")

special_json <- tempfile(fileext = ".json")
writeLines('{
  "par": {
    "backtick": "run `id`",
    "dollar": "path is $PATH",
    "subst": "time $(date)",
    "backslash": "C:\\\\Users\\\\test",
    "quotes_in_str": "She said \\"hi\\"",
    "slash": "a/b/c",
    "mixed": "a\\\\b\\"c$d`e"
  }
}', special_json)
Sys.setenv(VIASH_WORK_PARAMS = special_json)
data <- viash_parse_json()
unlink(special_json)

test_equal("special: backtick", data$par$backtick, "run `id`")
test_equal("special: dollar", data$par$dollar, "path is $PATH")
test_equal("special: subst", data$par$subst, "time $(date)")
test_equal("special: backslash", data$par$backslash, "C:\\Users\\test")
test_equal("special: quotes", data$par$quotes_in_str, 'She said "hi"')
test_equal("special: slash", data$par$slash, "a/b/c")
test_equal("special: mixed", data$par$mixed, 'a\\b"c$d`e')

## ===================================================================
## TEST 9: Numeric edge cases
## ===================================================================
cat("\n=== Test 9: Numeric edge cases ===\n")

num_json <- tempfile(fileext = ".json")
writeLines('{"par":{"neg":-7,"sci":1.5e10,"neg_sci":-2.5E-3,"big":999999999}}',
           num_json)
Sys.setenv(VIASH_WORK_PARAMS = num_json)
data <- viash_parse_json()
unlink(num_json)

test_equal("numeric: neg", data$par$neg, -7L)
test_equal("numeric: sci", data$par$sci, 1.5e10)
test_equal("numeric: neg_sci", data$par$neg_sci, -2.5E-3)
test_equal("numeric: big", data$par$big, 999999999L)

## ===================================================================
## TEST 10: Error handling - missing file
## ===================================================================
cat("\n=== Test 10: Error handling ===\n")

Sys.setenv(VIASH_WORK_PARAMS = "/nonexistent/path.json")
result <- tryCatch(viash_parse_json(), error = function(e) "ERROR")
test_equal("missing file errors", result, "ERROR")

## ===================================================================
## TEST 11: Big integer handling (bigint_as_char = TRUE)
## ===================================================================
cat("\n=== Test 11: Big integer handling ===\n")

bigint_json <- tempfile(fileext = ".json")
writeLines('{
  "par": {
    "small_int": 42,
    "big_int": 9007199254740993,
    "negative_big": -9007199254740993
  }
}', bigint_json)
Sys.setenv(VIASH_WORK_PARAMS = bigint_json)
data <- viash_parse_json()
unlink(bigint_json)

test_equal("small_int is integer", data$par$small_int, 42L)
# Big integers (> 2^53) should be preserved as character strings
test_equal("big_int is character", is.character(data$par$big_int), TRUE)
test_equal("big_int value preserved", data$par$big_int, "9007199254740993")
test_equal("negative_big is character", is.character(data$par$negative_big), TRUE)
test_equal("negative_big value preserved", data$par$negative_big, "-9007199254740993")

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
