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
cat > "$test_json" << 'EOF'
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
ViashParseJsonBash < "$test_json"

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
