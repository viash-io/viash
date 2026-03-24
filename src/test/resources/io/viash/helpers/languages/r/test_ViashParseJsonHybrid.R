#!/usr/bin/env Rscript

# Test suite for viash_parse_json R function in hybrid mode
# (jsonlite preferred, custom parser fallback)
cat("Running viash_parse_json hybrid mode unit tests...\n\n")

# Source the hybrid parser (self-contained: includes both jsonlite and custom parser)
source("src/main/resources/io/viash/languages/r/ViashParseJsonHybrid.R")

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
test_equal("par$array_mixed length", length(data$par$array_mixed), 4L)

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
## TEST 8: Compact / minified JSON (no whitespace)
## ===================================================================
cat("\n=== Test 8: Compact JSON (minified) ===\n")

compact_json <- tempfile(fileext = ".json")
writeLines('{"par":{"x":"hello","y":42,"z":true,"arr":["a","b"],"nested":{"k":"v"}},"meta":{"name":"comp"}}',
           compact_json)
Sys.setenv(VIASH_WORK_PARAMS = compact_json)
data <- viash_parse_json()
unlink(compact_json)

test_equal("compact: par$x", data$par$x, "hello")
test_equal("compact: par$y", data$par$y, 42L)
test_equal("compact: par$z", data$par$z, TRUE)
test_equal("compact: par$arr", data$par$arr, c("a", "b"))
test_equal("compact: par$nested$k", data$par$nested$k, "v")
test_equal("compact: meta$name", data$meta$name, "comp")

## ===================================================================
## TEST 9: Special characters in strings
## ===================================================================
cat("\n=== Test 9: Special characters ===\n")

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
## TEST 10: Numeric edge cases
## ===================================================================
cat("\n=== Test 10: Numeric edge cases ===\n")

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
## TEST 11: Mixed-type arrays
## ===================================================================
cat("\n=== Test 11: Mixed-type arrays ===\n")

mix_json <- tempfile(fileext = ".json")
writeLines('{"par":{"mix":["text",123,true,false,null,-5]}}', mix_json)
Sys.setenv(VIASH_WORK_PARAMS = mix_json)
data <- viash_parse_json()
unlink(mix_json)

# jsonlite with simplifyVector converts mixed-type arrays to character vectors,
# while the custom parser preserves native types in a list.
# Both behaviors are acceptable; test for whichever parser was used.
if (is.character(data$par$mix)) {
  # jsonlite path: mixed arrays become character vectors (NULLs become NA)
  test_equal("mixed array length (jsonlite)", length(data$par$mix), 6L)
  test_equal("mixed array[1] (jsonlite)", data$par$mix[[1]], "text")
  test_equal("mixed array[2] (jsonlite)", data$par$mix[[2]], "123")
  test_equal("mixed array[3] (jsonlite)", data$par$mix[[3]], "TRUE")
  test_equal("mixed array[4] (jsonlite)", data$par$mix[[4]], "FALSE")
  test_equal("mixed array[5] is NA (jsonlite)", is.na(data$par$mix[[5]]), TRUE)
  test_equal("mixed array[6] (jsonlite)", data$par$mix[[6]], "-5")
} else {
  # Custom parser path: mixed arrays are kept as lists with native types
  test_equal("mixed array length (custom)", length(data$par$mix), 6L)
  test_equal("mixed array[[1]] (custom)", data$par$mix[[1]], "text")
  test_equal("mixed array[[2]] (custom)", data$par$mix[[2]], 123L)
  test_equal("mixed array[[3]] (custom)", data$par$mix[[3]], TRUE)
  test_equal("mixed array[[4]] (custom)", data$par$mix[[4]], FALSE)
  test_equal("mixed array[[5]] (custom)", is.null(data$par$mix[[5]]), TRUE)
  test_equal("mixed array[[6]] (custom)", data$par$mix[[6]], -5L)
}

## ===================================================================
## TEST 12: Empty and minimal JSON
## ===================================================================
cat("\n=== Test 12: Empty/minimal JSON ===\n")

empty_json <- tempfile(fileext = ".json")
writeLines('{}', empty_json)
Sys.setenv(VIASH_WORK_PARAMS = empty_json)
data <- viash_parse_json()
unlink(empty_json)
test_equal("empty object", is.list(data), TRUE)
test_equal("empty object length", length(data), 0L)

empty_par_json <- tempfile(fileext = ".json")
writeLines('{"par":{}}', empty_par_json)
Sys.setenv(VIASH_WORK_PARAMS = empty_par_json)
data <- viash_parse_json()
unlink(empty_par_json)
test_equal("empty par", length(data$par), 0L)

## ===================================================================
## TEST 13: Deeply nested objects
## ===================================================================
cat("\n=== Test 13: Deeply nested objects ===\n")

deep_json <- tempfile(fileext = ".json")
writeLines('{
  "par": {
    "config": {
      "db": {
        "host": "localhost",
        "port": 5432
      },
      "cache": true
    }
  }
}', deep_json)
Sys.setenv(VIASH_WORK_PARAMS = deep_json)
data <- viash_parse_json()
unlink(deep_json)

test_equal("deep: config$db$host", data$par$config$db$host, "localhost")
test_equal("deep: config$db$port", data$par$config$db$port, 5432L)
test_equal("deep: config$cache", data$par$config$cache, TRUE)

## ===================================================================
## TEST 14: Error handling
## ===================================================================
cat("\n=== Test 14: Error handling ===\n")

# Invalid JSON: missing closing brace
bad_json <- tempfile(fileext = ".json")
writeLines('{"par":{"x": "hello"', bad_json)
Sys.setenv(VIASH_WORK_PARAMS = bad_json)
result <- tryCatch(viash_parse_json(), error = function(e) "ERROR")
unlink(bad_json)
test_equal("invalid JSON errors", result, "ERROR")

# Invalid JSON: trailing garbage
bad_json2 <- tempfile(fileext = ".json")
writeLines('{"x": }', bad_json2)
Sys.setenv(VIASH_WORK_PARAMS = bad_json2)
result2 <- tryCatch(viash_parse_json(), error = function(e) "ERROR")
unlink(bad_json2)
test_equal("malformed value errors", result2, "ERROR")

## ===================================================================
## TEST 15: Tricky string content (commas, colons, braces)
## ===================================================================
cat("\n=== Test 15: Tricky string content ===\n")

tricky_json <- tempfile(fileext = ".json")
writeLines('{
  "par": {
    "with_comma": "a, b, c",
    "with_colon": "key: value",
    "with_braces": "obj = {x: 1}",
    "with_brackets": "arr = [1, 2]"
  }
}', tricky_json)
Sys.setenv(VIASH_WORK_PARAMS = tricky_json)
data <- viash_parse_json()
unlink(tricky_json)

test_equal("tricky: comma", data$par$with_comma, "a, b, c")
test_equal("tricky: colon", data$par$with_colon, "key: value")
test_equal("tricky: braces", data$par$with_braces, "obj = {x: 1}")
test_equal("tricky: brackets", data$par$with_brackets, "arr = [1, 2]")

## ===================================================================
## TEST 16: Custom parser fallback via .viash_json_parse directly
## ===================================================================
cat("\n=== Test 16: Custom parser fallback ===\n")

# Test the custom parser path by calling .viash_json_parse directly
test_json_fb <- tempfile(fileext = ".json")
writeLines('{"par":{"x":"hello","y":42,"flag":true}}', test_json_fb)

json_text <- paste(readLines(test_json_fb, warn = FALSE), collapse = "\n")
data2 <- .viash_json_parse(json_text)
unlink(test_json_fb)

test_equal("fallback: par$x", data2$par$x, "hello")
test_equal("fallback: par$y", data2$par$y, 42L)
test_equal("fallback: par$flag", data2$par$flag, TRUE)

## ===================================================================
## TEST 17: Fallback complex parsing
## ===================================================================
cat("\n=== Test 17: Fallback complex parsing ===\n")

complex_json <- tempfile(fileext = ".json")
writeLines('{
  "par": {
    "config": {
      "db": {
        "host": "localhost",
        "port": 5432
      },
      "cache": true
    },
    "array_numbers": [1, 2, 3],
    "empty_array": [],
    "null_value": null
  }
}', complex_json)

# Test the custom parser path directly
json_text <- paste(readLines(complex_json, warn = FALSE), collapse = "\n")
data <- .viash_json_parse(json_text)
unlink(complex_json)

test_equal("fallback: config$db$host", data$par$config$db$host, "localhost")
test_equal("fallback: config$db$port", data$par$config$db$port, 5432L)
test_equal("fallback: config$cache", data$par$config$cache, TRUE)
test_equal("fallback: array_numbers", data$par$array_numbers, c(1L, 2L, 3L))
test_equal("fallback: empty_array", length(data$par$empty_array), 0L)
test_equal("fallback: null_value", data$par$null_value, NULL)

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
