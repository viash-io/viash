#!/bin/bash

# Test counter for tracking test results
TESTS_PASSED=0
TESTS_FAILED=0

# assert_value_equal: Assert that a value equals the expected value
# $1: name of the test
# $2: expected value
# $3+: actual value(s)
assert_value_equal() {
  local name="$1"
  local expected="$2"
  shift 2
  local values="$*"
  if [ "$expected" != "$values" ]; then
    echo "FAILED: $name"
    echo "  Expected: '$expected'"
    echo "  Got:      '$values'"
    TESTS_FAILED=$((TESTS_FAILED + 1))
    return 1
  else
    TESTS_PASSED=$((TESTS_PASSED + 1))
    return 0
  fi
}

# assert_array_equal: Assert that two arrays are equal
# $1: name of the test
# $2: name of expected array variable
# $3: name of actual array variable
assert_array_equal() {
  local name="$1"
  local expected_name="$2"
  local actual_name="$3"
  
  # Get array values using eval for bash 3.2 compatibility
  eval "local expected_values=(\"\${${expected_name}[@]}\")"
  eval "local actual_values=(\"\${${actual_name}[@]}\")"
  
  # Check lengths
  if [ ${#expected_values[@]} -ne ${#actual_values[@]} ]; then
    echo "FAILED: $name (array length mismatch)"
    echo "  Expected length: ${#expected_values[@]}"
    echo "  Got length:      ${#actual_values[@]}"
    TESTS_FAILED=$((TESTS_FAILED + 1))
    return 1
  fi
  
  # Check each element
  for i in "${!expected_values[@]}"; do
    if [ "${expected_values[$i]}" != "${actual_values[$i]}" ]; then
      echo "FAILED: $name (element $i mismatch)"
      echo "  Expected[$i]: '${expected_values[$i]}'"
      echo "  Got[$i]:      '${actual_values[$i]}'"
      TESTS_FAILED=$((TESTS_FAILED + 1))
      return 1
    fi
  done
  
  TESTS_PASSED=$((TESTS_PASSED + 1))
  return 0
}

# assert_contains: Assert that a string contains a substring
# $1: name of the test
# $2: substring to find
# $3: string to search in
assert_contains() {
  local name="$1"
  local substring="$2"
  local string="$3"
  if [[ "$string" != *"$substring"* ]]; then
    echo "FAILED: $name"
    echo "  Expected to contain: '$substring'"
    echo "  In string:           '$string'"
    TESTS_FAILED=$((TESTS_FAILED + 1))
    return 1
  else
    TESTS_PASSED=$((TESTS_PASSED + 1))
    return 0
  fi
}

# assert_not_contains: Assert that a string does not contain a substring
# $1: name of the test
# $2: substring that should not be found
# $3: string to search in
assert_not_contains() {
  local name="$1"
  local substring="$2"
  local string="$3"
  if [[ "$string" == *"$substring"* ]]; then
    echo "FAILED: $name"
    echo "  Expected NOT to contain: '$substring'"
    echo "  In string:               '$string'"
    TESTS_FAILED=$((TESTS_FAILED + 1))
    return 1
  else
    TESTS_PASSED=$((TESTS_PASSED + 1))
    return 0
  fi
}

# assert_exit_code: Assert that the last command had a specific exit code
# $1: name of the test
# $2: expected exit code
# $3: actual exit code
assert_exit_code() {
  local name="$1"
  local expected="$2"
  local actual="$3"
  if [ "$expected" -ne "$actual" ]; then
    echo "FAILED: $name"
    echo "  Expected exit code: $expected"
    echo "  Got exit code:      $actual"
    TESTS_FAILED=$((TESTS_FAILED + 1))
    return 1
  else
    TESTS_PASSED=$((TESTS_PASSED + 1))
    return 0
  fi
}

# print_test_summary: Print a summary of test results and exit with proper code
print_test_summary() {
  local total=$((TESTS_PASSED + TESTS_FAILED))
  echo ""
  echo "=================================="
  echo "Test Summary: $TESTS_PASSED/$total passed"
  if [ $TESTS_FAILED -gt 0 ]; then
    echo "FAILURES: $TESTS_FAILED"
    exit 1
  else
    echo "All tests passed!"
    exit 0
  fi
}
