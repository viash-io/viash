#!/bin/bash

# load helper functions
source src/main/resources/io/viash/helpers/bashutils/ViashAbsolutePath.sh
source src/main/resources/io/viash/helpers/bashutils/ViashLogging.sh
source src/main/resources/io/viash/helpers/bashutils/ViashDockerAutodetectMount.sh
source src/test/resources/io/viash/helpers/bashutils/helpers.sh

# Set up test environment
TEST_DIR=$(mktemp -d)
cleanup() {
  rm -rf "$TEST_DIR"
}
trap cleanup EXIT

# Create test directory structure
mkdir -p "$TEST_DIR/subdir"
touch "$TEST_DIR/file.txt"
touch "$TEST_DIR/subdir/nested.txt"


## TEST1: test ViashDockerAutodetectMount with default prefix

# Set default automount prefix
VIASH_DOCKER_AUTOMOUNT_PREFIX="/viash_automount"

# TEST1a: File path
output=$(ViashDockerAutodetectMount "$TEST_DIR/file.txt")
assert_value_equal "test1a_output" "/viash_automount$TEST_DIR/file.txt" "$output"

# TEST1b: Directory path
output=$(ViashDockerAutodetectMount "$TEST_DIR/subdir")
assert_value_equal "test1b_output" "/viash_automount$TEST_DIR/subdir" "$output"

# TEST1c: Nested file path
output=$(ViashDockerAutodetectMount "$TEST_DIR/subdir/nested.txt")
assert_value_equal "test1c_output" "/viash_automount$TEST_DIR/subdir/nested.txt" "$output"


## TEST2: test ViashDockerAutodetectMount with custom prefix

VIASH_DOCKER_AUTOMOUNT_PREFIX="/custom_mount"

# TEST2a: File with custom prefix
output=$(ViashDockerAutodetectMount "$TEST_DIR/file.txt")
assert_value_equal "test2a_output" "/custom_mount$TEST_DIR/file.txt" "$output"

# TEST2b: Directory with custom prefix
output=$(ViashDockerAutodetectMount "$TEST_DIR/subdir")
assert_value_equal "test2b_output" "/custom_mount$TEST_DIR/subdir" "$output"


## TEST3: test ViashDockerAutodetectMount with empty prefix

VIASH_DOCKER_AUTOMOUNT_PREFIX=""

# TEST3a: File with empty prefix (path should be unchanged)
output=$(ViashDockerAutodetectMount "$TEST_DIR/file.txt")
assert_value_equal "test3a_output" "$TEST_DIR/file.txt" "$output"


## TEST4: test ViashDockerAutodetectMountArg

VIASH_DOCKER_AUTOMOUNT_PREFIX="/viash_automount"

# TEST4a: Volume mount for file
output=$(ViashDockerAutodetectMountArg "$TEST_DIR/file.txt")
assert_value_equal "test4a_output" "--volume=\"$TEST_DIR:/viash_automount$TEST_DIR\"" "$output"

# TEST4b: Volume mount for directory
output=$(ViashDockerAutodetectMountArg "$TEST_DIR/subdir")
assert_value_equal "test4b_output" "--volume=\"$TEST_DIR/subdir:/viash_automount$TEST_DIR/subdir\"" "$output"


## TEST5: test ViashDockerStripAutomount

VIASH_DOCKER_AUTOMOUNT_PREFIX="/viash_automount"

# TEST5a: Strip prefix from automounted path
output=$(ViashDockerStripAutomount "/viash_automount$TEST_DIR/file.txt")
assert_value_equal "test5a_output" "$TEST_DIR/file.txt" "$output"

# TEST5b: Path without prefix should remain unchanged
output=$(ViashDockerStripAutomount "$TEST_DIR/file.txt")
assert_value_equal "test5b_output" "$TEST_DIR/file.txt" "$output"

# TEST5c: Path with different prefix
output=$(ViashDockerStripAutomount "/other_prefix$TEST_DIR/file.txt")
assert_value_equal "test5c_output" "/other_prefix$TEST_DIR/file.txt" "$output"


## TEST6: test with relative paths (should be converted to absolute)

VIASH_DOCKER_AUTOMOUNT_PREFIX="/viash_automount"

# TEST6a: Relative path
cd "$TEST_DIR"
touch "relative_file.txt"
output=$(ViashDockerAutodetectMount "relative_file.txt")
assert_value_equal "test6a_output" "/viash_automount$TEST_DIR/relative_file.txt" "$output"

# Go back to original directory
cd - > /dev/null

print_test_summary
