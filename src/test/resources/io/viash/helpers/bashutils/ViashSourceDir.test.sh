#!/bin/bash

# load helper functions
source src/main/resources/io/viash/helpers/bashutils/ViashSourceDir.sh
source src/main/resources/io/viash/helpers/bashutils/ViashFindTargetDir.sh
source src/test/resources/io/viash/helpers/bashutils/helpers.sh

# Create a temporary directory structure for testing
TEST_DIR=$(mktemp -d)
cleanup() {
  rm -rf "$TEST_DIR"
}
trap cleanup EXIT


## TEST1: test ViashSourceDir

# TEST1a: Get directory of this script
output=$(ViashSourceDir "${BASH_SOURCE[0]}")
expected=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
assert_value_equal "test1a_output" "$expected" "$output"


## TEST2: test ViashSourceDir with symlinks

# TEST2a: Create a symlink to a script and resolve it
mkdir -p "$TEST_DIR/real_dir"
echo '#!/bin/bash' > "$TEST_DIR/real_dir/script.sh"
chmod +x "$TEST_DIR/real_dir/script.sh"

mkdir -p "$TEST_DIR/link_dir"
ln -s "$TEST_DIR/real_dir/script.sh" "$TEST_DIR/link_dir/script_link.sh"

# Source the function and test it
output=$(ViashSourceDir "$TEST_DIR/link_dir/script_link.sh")
assert_value_equal "test2a_output" "$TEST_DIR/real_dir" "$output"


## TEST3: test ViashFindTargetDir

# TEST3a: Create a directory structure with .build.yaml
mkdir -p "$TEST_DIR/project/target/namespace/component"
touch "$TEST_DIR/project/target/.build.yaml"

output=$(ViashFindTargetDir "$TEST_DIR/project/target/namespace/component")
assert_value_equal "test3a_output" "$TEST_DIR/project/target" "$output"

# TEST3b: Find .build.yaml from direct parent
output=$(ViashFindTargetDir "$TEST_DIR/project/target/namespace")
assert_value_equal "test3b_output" "$TEST_DIR/project/target" "$output"

# TEST3c: Find .build.yaml from the target dir itself
output=$(ViashFindTargetDir "$TEST_DIR/project/target")
assert_value_equal "test3c_output" "$TEST_DIR/project/target" "$output"


## TEST4: test ViashFindTargetDir when no .build.yaml exists

# TEST4a: No .build.yaml found
mkdir -p "$TEST_DIR/no_build/subdir"
output=$(ViashFindTargetDir "$TEST_DIR/no_build/subdir")
assert_value_equal "test4a_output" "" "$output"


## TEST5: test ViashFindTargetDir with multiple .build.yaml files

# TEST5a: Should find the closest (lowest) .build.yaml
mkdir -p "$TEST_DIR/multi/a/b/c"
touch "$TEST_DIR/multi/.build.yaml"
touch "$TEST_DIR/multi/a/b/.build.yaml"

output=$(ViashFindTargetDir "$TEST_DIR/multi/a/b/c")
assert_value_equal "test5a_output" "$TEST_DIR/multi/a/b" "$output"

# TEST5b: Starting from a location with .build.yaml
output=$(ViashFindTargetDir "$TEST_DIR/multi/a/b")
assert_value_equal "test5b_output" "$TEST_DIR/multi/a/b" "$output"

# TEST5c: Going above the nested .build.yaml should find the parent one
output=$(ViashFindTargetDir "$TEST_DIR/multi/a")
assert_value_equal "test5c_output" "$TEST_DIR/multi" "$output"

print_test_summary
