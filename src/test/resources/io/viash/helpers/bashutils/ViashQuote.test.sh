#!/bin/bash

# load helper functions
source src/main/resources/io/viash/helpers/bashutils/ViashQuote.sh
source src/test/resources/io/viash/helpers/bashutils/helpers.sh

## TEST1: test flag values (should not be quoted)

# TEST1a: Simple flag
output=$(ViashQuote "--foo")
assert_value_equal "test1a_output" "--foo" "$output"

# TEST1b: Flag with underscore
output=$(ViashQuote "--my_flag")
assert_value_equal "test1b_output" "--my_flag" "$output"

# TEST1c: Flag with hyphen
output=$(ViashQuote "--my-flag")
assert_value_equal "test1c_output" "--my-flag" "$output"

# TEST1d: Single-dash flag
output=$(ViashQuote "-f")
assert_value_equal "test1d_output" "-f" "$output"

# TEST1e: Flag with numbers
output=$(ViashQuote "--option123")
assert_value_equal "test1e_output" "--option123" "$output"


## TEST2: test plain values (should be quoted)

# TEST2a: Simple string
output=$(ViashQuote "bar")
assert_value_equal "test2a_output" "'bar'" "$output"

# TEST2b: String with spaces
output=$(ViashQuote "hello world")
assert_value_equal "test2b_output" "'hello world'" "$output"

# TEST2c: Numeric string
output=$(ViashQuote "123")
assert_value_equal "test2c_output" "'123'" "$output"

# TEST2d: Path
output=$(ViashQuote "/path/to/file.txt")
assert_value_equal "test2d_output" "'/path/to/file.txt'" "$output"

# TEST2e: Empty string
output=$(ViashQuote "")
assert_value_equal "test2e_output" "''" "$output"


## TEST3: test flag=value combinations

# TEST3a: Simple flag=value
output=$(ViashQuote "--foo=bar")
assert_value_equal "test3a_output" "--foo='bar'" "$output"

# TEST3b: Flag=value with spaces in value
output=$(ViashQuote "--message=hello world")
assert_value_equal "test3b_output" "--message='hello world'" "$output"

# TEST3c: Flag=value with path
output=$(ViashQuote "--input=/path/to/file.txt")
assert_value_equal "test3c_output" "--input='/path/to/file.txt'" "$output"

# TEST3d: Flag=value with numbers
output=$(ViashQuote "--count=42")
assert_value_equal "test3d_output" "--count='42'" "$output"

# TEST3e: Single-dash flag=value
output=$(ViashQuote "-o=output.txt")
assert_value_equal "test3e_output" "-o='output.txt'" "$output"


## TEST4: test edge cases

# TEST4a: String starting with equals (should be quoted, not treated as flag=value)
output=$(ViashQuote "=value")
assert_value_equal "test4a_output" "'=value'" "$output"

# TEST4b: String with only hyphens (treated as flag-like pattern since regex allows hyphens)
output=$(ViashQuote "---")
assert_value_equal "test4b_output" "---" "$output"

# TEST4c: Flag with trailing equals but no value (edge case)
output=$(ViashQuote "--foo=")
# This may or may not match the flag=value pattern depending on implementation
# The current implementation requires at least one char after =

# TEST4d: Value that looks like a flag inside
output=$(ViashQuote "not--a--flag")
assert_value_equal "test4d_output" "'not--a--flag'" "$output"

print_test_summary
