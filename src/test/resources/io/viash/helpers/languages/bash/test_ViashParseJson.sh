#!/usr/bin/env bash

# Test suite for viash_parse_json Bash function
echo "Running viash_parse_json Bash unit tests..."

# Load the parser
source src/main/resources/io/viash/languages/bash/ViashParseJson.sh

# Colors for test output
RED='\033[0;31m'
GREEN='\033[0;32m'
RESET='\033[0m'

test_passed=0
test_failed=0

# Test helper function
test_equal() {
  local description="$1"
  local actual="$2"
  local expected="$3"

  if [ "$actual" = "$expected" ]; then
    echo -e "${GREEN}PASS${RESET}: $description"
    ((test_passed++))
    return 0
  else
    echo -e "${RED}FAIL${RESET}: $description"
    echo "  Expected: $expected"
    echo "  Got: $actual"
    ((test_failed++))
    return 1
  fi
}

# Create test JSON file
test_json=$(mktemp)
cat >"$test_json" <<'EOF'
{
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
        "nasty_val": "{nasty}",
        "level2": "deep_value"
      }
    },
    "path_with_spaces": "/path/with spaces/file.txt",
    "quotes": "She said \"hello\"",
    "newlines": "line1\nline2",
    "tabs": "col1\tcol2",
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
}
EOF

# Parse the JSON
ViashParseJsonBash <"$test_json"

echo "=== Test 1: Basic key-value pairs ==="
test_equal "par_input" "$par_input" "file.txt"
test_equal "par_number" "$par_number" "42"
test_equal "par_flag" "$par_flag" "true"
test_equal "par_empty_value" "$par_empty_value" ""
test_equal "meta_name" "$meta_name" "test_component"
test_equal "meta_version" "$meta_version" "1.0"

echo
echo "=== Test 2: Arrays ==="
test_equal "par_array_simple[0]" "${par_array_simple[0]}" "a"
test_equal "par_array_simple[1]" "${par_array_simple[1]}" "b"
test_equal "par_array_simple[2]" "${par_array_simple[2]}" "c"
test_equal "par_array_numbers[0]" "${par_array_numbers[0]}" "1"
test_equal "par_array_numbers[1]" "${par_array_numbers[1]}" "2"
test_equal "par_array_numbers[2]" "${par_array_numbers[2]}" "3"

echo
echo "=== Test 3: Nested objects (stored as JSON strings) ==="
# Nested objects should be stored as JSON strings
test_equal "par_nested exists" "${par_nested:+set}" "set"
# Simple check that it looks like JSON
if [[ "$par_nested" =~ "level1" ]] && [[ "$par_nested" =~ "level2" ]]; then
  echo -e "${GREEN}PASS${RESET}: par_nested contains expected nested structure"
  ((test_passed++))
else
  echo -e "${RED}FAIL${RESET}: par_nested contains expected nested structure"
  echo "  Got: $par_nested"
  ((test_failed++))
fi

echo
echo "=== Test 4: Quoted strings ==="
test_equal "par_path_with_spaces" "$par_path_with_spaces" "/path/with spaces/file.txt"
test_equal "par_quotes" "$par_quotes" "She said \"hello\""
# Note: In bash, \n and \t are literal strings, not escape sequences
test_equal "par_newlines" "$par_newlines" "line1\nline2"
test_equal "par_tabs" "$par_tabs" "col1\tcol2"

echo
echo "=== Test 5: Type conversions ==="
test_equal "par_string" "$par_string" "text"
test_equal "par_integer" "$par_integer" "123"
test_equal "par_float" "$par_float" "3.14"
test_equal "par_bool_true" "$par_bool_true" "true"
test_equal "par_bool_false" "$par_bool_false" "false"
# null values should leave the variable unset
test_equal "par_null_value (unset)" "${par_null_value-UNSET}" "UNSET"

echo
echo "=== Test 6: Root-level values ==="
test_equal "simple_key" "$simple_key" "value"
test_equal "number_key" "$number_key" "99"
test_equal "bool_key" "$bool_key" "false"

echo
echo "=== Test 7: Edge cases ==="
test_equal "par_empty_string" "$par_empty_string" ""
test_equal "par_zero" "$par_zero" "0"
test_equal "par_empty_array length" "${#par_empty_array[@]}" "0"

# Clean up
rm -f "$test_json"

## ===================================================================
## TEST 8: Compact / minified JSON (no whitespace)
## ===================================================================
echo
echo "=== Test 8: Compact JSON (minified) ==="

ViashParseJsonBash <<< '{"par":{"x":"hello","y":42,"z":true,"arr":["a","b"],"nested":{"k":"v"}},"meta":{"name":"comp"}}'

test_equal "compact: par_x" "$par_x" "hello"
test_equal "compact: par_y" "$par_y" "42"
test_equal "compact: par_z" "$par_z" "true"
test_equal "compact: par_arr[0]" "${par_arr[0]}" "a"
test_equal "compact: par_arr[1]" "${par_arr[1]}" "b"
test_equal "compact: par_nested contains k" "$(echo "$par_nested" | grep -c '"k"')" "1"
test_equal "compact: meta_name" "$meta_name" "comp"

## ===================================================================
## TEST 9: Special characters in strings
## ===================================================================
echo
echo "=== Test 9: Special characters ==="

test_special=$(mktemp)
cat >"$test_special" <<'EOF'
{
  "par": {
    "backtick": "run `id`",
    "dollar": "path is $PATH",
    "subst": "time $(date)",
    "backslash": "C:\\Users\\test",
    "quotes_in_str": "She said \"hi\"",
    "slash": "a/b/c",
    "mixed": "a\\b\"c$d`e"
  }
}
EOF
ViashParseJsonBash <"$test_special"
rm -f "$test_special"

test_equal "special: backtick" "$par_backtick" 'run `id`'
test_equal "special: dollar" "$par_dollar" 'path is $PATH'
test_equal "special: subst" "$par_subst" 'time $(date)'
test_equal "special: backslash" "$par_backslash" 'C:\Users\test'
test_equal "special: quotes" "$par_quotes_in_str" 'She said "hi"'
test_equal "special: slash" "$par_slash" "a/b/c"
test_equal "special: mixed" "$par_mixed" 'a\b"c$d`e'

## ===================================================================
## TEST 10: Negative numbers, scientific notation
## ===================================================================
echo
echo "=== Test 10: Numeric edge cases ==="

ViashParseJsonBash <<< '{"par":{"neg":-7,"sci":1.5e10,"neg_sci":-2.5E-3,"big":999999999}}'

test_equal "numeric: neg" "$par_neg" "-7"
test_equal "numeric: sci" "$par_sci" "1.5e10"
test_equal "numeric: neg_sci" "$par_neg_sci" "-2.5E-3"
test_equal "numeric: big" "$par_big" "999999999"

## ===================================================================
## TEST 11: Mixed-type arrays
## ===================================================================
echo
echo "=== Test 11: Mixed-type arrays ==="

ViashParseJsonBash <<< '{"par":{"mix":["text",123,true,false,null,-5]}}'

test_equal "mixed array[0]" "${par_mix[0]}" "text"
test_equal "mixed array[1]" "${par_mix[1]}" "123"
test_equal "mixed array[2]" "${par_mix[2]}" "true"
test_equal "mixed array[3]" "${par_mix[3]}" "false"
test_equal "mixed array[4]" "${par_mix[4]}" "null"
test_equal "mixed array[5]" "${par_mix[5]}" "-5"
test_equal "mixed array length" "${#par_mix[@]}" "6"

## ===================================================================
## TEST 12: Empty and minimal JSON
## ===================================================================
echo
echo "=== Test 12: Empty/minimal JSON ==="

ViashParseJsonBash <<< '{}'
# Should not crash - that's the test

ViashParseJsonBash <<< '{"par":{}}'
# Should also not crash

ViashParseJsonBash <<< '{"par":{"only":true}}'
test_equal "minimal: par_only" "$par_only" "true"

## ===================================================================
## TEST 13: Deeply nested objects stored as JSON
## ===================================================================
echo
echo "=== Test 13: Deeply nested objects ==="

test_deep=$(mktemp)
cat >"$test_deep" <<'EOF'
{
  "par": {
    "config": {
      "db": {
        "host": "localhost",
        "port": 5432
      },
      "cache": true
    }
  }
}
EOF
ViashParseJsonBash <"$test_deep"
rm -f "$test_deep"

test_equal "deep: par_config exists" "${par_config:+set}" "set"
# Should contain the nested JSON including db and cache
if [[ "$par_config" =~ "host" ]] && [[ "$par_config" =~ "localhost" ]] && [[ "$par_config" =~ "cache" ]]; then
  echo -e "${GREEN}PASS${RESET}: deep: par_config contains expected structure"
  ((test_passed++))
else
  echo -e "${RED}FAIL${RESET}: deep: par_config contains expected structure"
  echo "  Got: $par_config"
  ((test_failed++))
fi

## ===================================================================
## TEST 14: Error handling - invalid JSON should fail
## ===================================================================
echo
echo "=== Test 14: Error handling ==="

# Invalid JSON: missing closing brace
set +e
stderr=$(ViashParseJsonBash <<< '{"par":{"x": "hello"' 2>&1 >/dev/null)
exit_code=$?
set -e
if [ $exit_code -ne 0 ]; then
  echo -e "${GREEN}PASS${RESET}: invalid JSON exits with error"
  ((test_passed++))
else
  echo -e "${RED}FAIL${RESET}: invalid JSON exits with error (got exit code 0)"
  ((test_failed++))
fi

# Invalid JSON: trailing garbage
set +e
stderr=$(ViashParseJsonBash <<< '{"x": }' 2>&1 >/dev/null)
exit_code=$?
set -e
if [ $exit_code -ne 0 ]; then
  echo -e "${GREEN}PASS${RESET}: malformed value exits with error"
  ((test_passed++))
else
  echo -e "${RED}FAIL${RESET}: malformed value exits with error (got exit code 0)"
  ((test_failed++))
fi

## ===================================================================
## TEST 15: Strings with commas, colons, braces (tricky for line parsers)
## ===================================================================
echo
echo "=== Test 15: Tricky string content ==="

test_tricky=$(mktemp)
cat >"$test_tricky" <<'EOF'
{
  "par": {
    "with_comma": "a, b, c",
    "with_colon": "key: value",
    "with_braces": "obj = {x: 1}",
    "with_brackets": "arr = [1, 2]"
  }
}
EOF
ViashParseJsonBash <"$test_tricky"
rm -f "$test_tricky"

test_equal "tricky: comma" "$par_with_comma" "a, b, c"
test_equal "tricky: colon" "$par_with_colon" "key: value"
test_equal "tricky: braces" "$par_with_braces" "obj = {x: 1}"
test_equal "tricky: brackets" "$par_with_brackets" "arr = [1, 2]"

# Print summary
echo
echo "=================================================="
echo "Tests completed: $((test_passed + test_failed))"
echo "Tests passed: $test_passed"
if [ $test_failed -gt 0 ]; then
  echo -e "${RED}Tests failed: $test_failed${RESET}"
  exit 1
else
  echo -e "${GREEN}All tests passed!${RESET}"
fi
