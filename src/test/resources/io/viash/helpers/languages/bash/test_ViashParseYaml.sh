#!/bin/bash

# Unit test for ViashParseYamlBash function
# This test verifies that the bash YAML parser correctly parses YAML content
# and sets environment variables with appropriate prefixes.

# Colors for test output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Test counters
TESTS_PASSED=0
TESTS_FAILED=0

# Function to run a test
run_test() {
  local test_name="$1"
  local expected="$2"
  local actual="$3"
  
  echo -n "Testing $test_name... "
  
  if [[ "$actual" == "$expected" ]]; then
    echo -e "${GREEN}PASS${NC}"
    ((TESTS_PASSED++))
  else
    echo -e "${RED}FAIL${NC}"
    echo "  Expected: '$expected'"
    echo "  Actual:   '$actual'"
    ((TESTS_FAILED++))
  fi
}

# Function to test array
test_array() {
  local test_name="$1"
  local var_name="$2"
  shift 2
  local expected_array=("$@")
  
  echo -n "Testing $test_name... "
  
  # Get the actual array using nameref
  local -n actual_array_ref="$var_name"
  
  if [[ "${#actual_array_ref[@]}" -eq "${#expected_array[@]}" ]]; then
    local arrays_match=true
    for ((i=0; i<${#expected_array[@]}; i++)); do
      if [[ "${actual_array_ref[i]}" != "${expected_array[i]}" ]]; then
        arrays_match=false
        break
      fi
    done
    
    if [[ "$arrays_match" == true ]]; then
      echo -e "${GREEN}PASS${NC}"
      ((TESTS_PASSED++))
      return
    fi
  fi
  
  echo -e "${RED}FAIL${NC}"
  echo "  Expected array: (${expected_array[*]})"
  echo "  Actual array:   (${actual_array_ref[*]})"
  ((TESTS_FAILED++))
}

# Source the YAML parser function
source "src/main/resources/io/viash/languages/bash/ViashParseYaml.sh"

echo -e "${YELLOW}Running ViashParseYamlBash unit tests...${NC}"
echo

# Test 1: Basic key-value pairs in sections
echo "=== Test 1: Basic key-value pairs ==="
yaml_content="
par:
input: \"/path/to/input.txt\"
number: 42
flag: true
empty_value: null
meta:
name: \"test_component\"
version: \"1.0.0\"
"

# Clear any existing variables
unset par_input par_number par_flag par_empty_value
unset meta_name meta_version

# Parse the YAML
ViashParseYamlBash <<< "$yaml_content"

# Test the results
run_test "par_input" "/path/to/input.txt" "$par_input"
run_test "par_number" "42" "$par_number"
run_test "par_flag" "true" "$par_flag"
run_test "par_empty_value" "" "$par_empty_value"
run_test "meta_name" "test_component" "$meta_name"
run_test "meta_version" "1.0.0" "$meta_version"

echo

# Test 2: Arrays in sections
echo "=== Test 2: Arrays ==="
yaml_content="
par:
  files:
  - \"file1.txt\"
  - \"file2.txt\"
  - \"file3.txt\"
  numbers:
  - 1
  - 2
  - 3
meta:
  tags:
  - production
  - stable
"

# Clear any existing variables
unset par_files par_numbers meta_tags

# Parse the YAML
ViashParseYamlBash <<< "$yaml_content"

# Test array results
if [[ -v par_files ]]; then
  test_array "par_files array" "par_files" "file1.txt" "file2.txt" "file3.txt"
else
  echo -e "Testing par_files array... ${RED}FAIL${NC} (variable not set)"
  ((TESTS_FAILED++))
fi

if [[ -v par_numbers ]]; then
  test_array "par_numbers array" "par_numbers" "1" "2" "3"
else
  echo -e "Testing par_numbers array... ${RED}FAIL${NC} (variable not set)"
  ((TESTS_FAILED++))
fi

if [[ -v meta_tags ]]; then
  test_array "meta_tags array" "meta_tags" "production" "stable"
else
  echo -e "Testing meta_tags array... ${RED}FAIL${NC} (variable not set)"
  ((TESTS_FAILED++))
fi

echo

# Test 3: Quoted strings with special characters
echo "=== Test 3: Quoted strings with escapes ==="
yaml_content="
par:
  message: \"Hello \\\"world\\\"!\"
  multiline: \"Line 1\\nLine 2\"
  backslash: \"Path\\\\to\\\\file\"
"

# Clear any existing variables
unset par_message par_multiline par_backslash

# Parse the YAML
ViashParseYamlBash <<< "$yaml_content"

# Test the results
run_test "par_message (quotes)" "Hello \"world\"!" "$par_message"
run_test "par_multiline (newline)" $'Line 1\nLine 2' "$par_multiline"
run_test "par_backslash" "Path\\to\\file" "$par_backslash"

echo

# Test 4: Boolean and special values
echo "=== Test 4: Boolean and special values ==="
yaml_content="
config:
  enabled: true
  disabled: false
  empty: null
  zero: 0
  negative: -1
"

# Clear any existing variables
unset config_enabled config_disabled config_empty config_zero config_negative

# Parse the YAML
ViashParseYamlBash <<< "$yaml_content"

# Test the results
run_test "config_enabled" "true" "$config_enabled"
run_test "config_disabled" "false" "$config_disabled"
run_test "config_empty" "" "$config_empty"
run_test "config_zero" "0" "$config_zero"
run_test "config_negative" "-1" "$config_negative"

echo

# Test 5: Custom prefix (only applies when no sections are present)
echo "=== Test 5: Custom prefix ==="
yaml_content="
key1: value1
key2: value2
"

# Clear any existing variables
unset CUSTOM_key1 CUSTOM_key2

# Parse the YAML with custom prefix (no sections)
ViashParseYamlBash "CUSTOM_" <<< "$yaml_content"

# Test the results
run_test "CUSTOM_key1" "value1" "$CUSTOM_key1"
run_test "CUSTOM_key2" "value2" "$CUSTOM_key2"

echo

# Test 6: Empty sections and edge cases
echo "=== Test 6: Edge cases ==="
yaml_content="
empty_section:
section_with_content:
  key: value

# Comment line should be ignored
another_section:
  # Another comment
  final_key: final_value
"

# Clear any existing variables
unset section_with_content_key another_section_final_key

# Parse the YAML
ViashParseYamlBash <<< "$yaml_content"

# Test the results
run_test "section_with_content_key" "value" "$section_with_content_key"
run_test "another_section_final_key" "final_value" "$another_section_final_key"

echo

# Summary
echo "=== Test Summary ==="
echo -e "Tests passed: ${GREEN}$TESTS_PASSED${NC}"
echo -e "Tests failed: ${RED}$TESTS_FAILED${NC}"
echo -e "Total tests:  $((TESTS_PASSED + TESTS_FAILED))"

if [[ $TESTS_FAILED -eq 0 ]]; then
  echo -e "\n${GREEN}All tests passed!${NC}"
  exit 0
else
  echo -e "\n${RED}Some tests failed!${NC}"
  exit 1
fi
