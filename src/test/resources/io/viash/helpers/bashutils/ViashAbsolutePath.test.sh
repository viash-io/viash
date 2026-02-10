#!/bin/bash

# load helper functions
source src/main/resources/io/viash/helpers/bashutils/ViashAbsolutePath.sh
source src/test/resources/io/viash/helpers/bashutils/helpers.sh

# Store current directory for reference
ORIG_PWD="$PWD"


## TEST1: test relative paths

# TEST1a: Simple relative file
output=$(ViashAbsolutePath "file.txt")
assert_value_equal "test1a_output" "$ORIG_PWD/file.txt" "$output"

# TEST1b: Relative path with subdirectory
output=$(ViashAbsolutePath "subdir/file.txt")
assert_value_equal "test1b_output" "$ORIG_PWD/subdir/file.txt" "$output"

# TEST1c: Relative path with multiple subdirectories
output=$(ViashAbsolutePath "a/b/c/file.txt")
assert_value_equal "test1c_output" "$ORIG_PWD/a/b/c/file.txt" "$output"


## TEST2: test absolute paths

# TEST2a: Simple absolute path
output=$(ViashAbsolutePath "/foo/bar/file.txt")
assert_value_equal "test2a_output" "/foo/bar/file.txt" "$output"

# TEST2b: Root path
output=$(ViashAbsolutePath "/")
assert_value_equal "test2b_output" "/" "$output"

# TEST2c: Single directory absolute path
output=$(ViashAbsolutePath "/foo")
assert_value_equal "test2c_output" "/foo" "$output"


## TEST3: test path normalization with ..

# TEST3a: Path with parent directory reference
output=$(ViashAbsolutePath "/foo/bar/..")
assert_value_equal "test3a_output" "/foo" "$output"

# TEST3b: Path with multiple parent references
output=$(ViashAbsolutePath "/foo/bar/baz/../..")
assert_value_equal "test3b_output" "/foo" "$output"

# TEST3c: Path with parent reference in the middle
output=$(ViashAbsolutePath "/foo/bar/../baz/file.txt")
assert_value_equal "test3c_output" "/foo/baz/file.txt" "$output"

# TEST3d: Relative path with parent reference
output=$(ViashAbsolutePath "foo/bar/../file.txt")
assert_value_equal "test3d_output" "$ORIG_PWD/foo/file.txt" "$output"

# TEST3e: Too many parent references (should stay at root)
output=$(ViashAbsolutePath "/foo/../../../bar")
assert_value_equal "test3e_output" "/bar" "$output"


## TEST4: test path normalization with .

# TEST4a: Path with current directory reference
output=$(ViashAbsolutePath "/foo/./bar")
assert_value_equal "test4a_output" "/foo/bar" "$output"

# TEST4b: Path with multiple current directory references
output=$(ViashAbsolutePath "/foo/././bar/./baz")
assert_value_equal "test4b_output" "/foo/bar/baz" "$output"

# TEST4c: Path with mixed . and ..
output=$(ViashAbsolutePath "/foo/./bar/../baz/./file.txt")
assert_value_equal "test4c_output" "/foo/baz/file.txt" "$output"


## TEST5: test edge cases

# TEST5a: Path with trailing slash
output=$(ViashAbsolutePath "/foo/bar/")
assert_value_equal "test5a_output" "/foo/bar" "$output"

# TEST5b: Path with multiple consecutive slashes
output=$(ViashAbsolutePath "/foo//bar///baz")
assert_value_equal "test5b_output" "/foo/bar/baz" "$output"

# TEST5c: Just a dot (current directory)
output=$(ViashAbsolutePath ".")
assert_value_equal "test5c_output" "$ORIG_PWD" "$output"

# TEST5d: Just two dots (parent directory)
output=$(ViashAbsolutePath "..")
expected_parent=$(dirname "$ORIG_PWD")
assert_value_equal "test5d_output" "$expected_parent" "$output"


## TEST6: test relative paths starting with ./

# TEST6a: Relative path starting with ./
output=$(ViashAbsolutePath "./file.txt")
assert_value_equal "test6a_output" "$ORIG_PWD/file.txt" "$output"

# TEST6b: Relative path starting with ./subdir
output=$(ViashAbsolutePath "./subdir/file.txt")
assert_value_equal "test6b_output" "$ORIG_PWD/subdir/file.txt" "$output"

print_test_summary
