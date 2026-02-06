#!/bin/bash

# load helper functions
source src/main/resources/io/viash/helpers/bashutils/ViashRemoveFlags.sh
source src/test/resources/io/viash/helpers/bashutils/helpers.sh


## TEST1: test removing double-dash flags

# TEST1a: Simple flag=value
output=$(ViashRemoveFlags "--foo=bar")
assert_value_equal "test1a_output" "bar" "$output"

# TEST1b: Flag with underscore
output=$(ViashRemoveFlags "--my_flag=value")
assert_value_equal "test1b_output" "value" "$output"

# TEST1c: Flag with hyphen
output=$(ViashRemoveFlags "--my-flag=value")
assert_value_equal "test1c_output" "value" "$output"

# TEST1d: Flag with numbers
output=$(ViashRemoveFlags "--option123=value")
assert_value_equal "test1d_output" "value" "$output"


## TEST2: test removing single-dash flags

# TEST2a: Single letter flag
output=$(ViashRemoveFlags "-f=value")
assert_value_equal "test2a_output" "value" "$output"

# TEST2b: Multiple letter single-dash flag
output=$(ViashRemoveFlags "-abc=value")
assert_value_equal "test2b_output" "value" "$output"


## TEST3: test values containing special characters

# TEST3a: Value with spaces
output=$(ViashRemoveFlags "--message=hello world")
assert_value_equal "test3a_output" "hello world" "$output"

# TEST3b: Value with path
output=$(ViashRemoveFlags "--input=/path/to/file.txt")
assert_value_equal "test3b_output" "/path/to/file.txt" "$output"

# TEST3c: Value with equals sign
output=$(ViashRemoveFlags "--equation=a=b+c")
assert_value_equal "test3c_output" "a=b+c" "$output"

# TEST3d: Value with multiple equals signs
output=$(ViashRemoveFlags "--param=key=value=extra")
assert_value_equal "test3d_output" "key=value=extra" "$output"


## TEST4: test strings without flags (should pass through unchanged)

# TEST4a: Plain value
output=$(ViashRemoveFlags "just_a_value")
assert_value_equal "test4a_output" "just_a_value" "$output"

# TEST4b: Value starting with equals
output=$(ViashRemoveFlags "=value")
assert_value_equal "test4b_output" "=value" "$output"

# TEST4c: Value with dashes in middle (not a flag)
output=$(ViashRemoveFlags "not--a-flag")
assert_value_equal "test4c_output" "not--a-flag" "$output"


## TEST5: test edge cases

# TEST5a: Empty value after flag
output=$(ViashRemoveFlags "--flag=")
assert_value_equal "test5a_output" "" "$output"

# TEST5b: Flag only (no equals sign) - should pass through unchanged
output=$(ViashRemoveFlags "--flag")
assert_value_equal "test5b_output" "--flag" "$output"

# TEST5c: Numeric value
output=$(ViashRemoveFlags "--count=42")
assert_value_equal "test5c_output" "42" "$output"

# TEST5d: Quoted value
output=$(ViashRemoveFlags '--message="hello world"')
assert_value_equal "test5d_output" '"hello world"' "$output"

print_test_summary
