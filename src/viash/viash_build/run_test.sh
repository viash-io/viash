#!/usr/bin/env bash

set -ex

defaults_output="defaults_test_output.txt"
expected_target_dir="target/native/testns"

alt_output=alt_test_output.txt
alt_src="alt_src"
log=build_log.txt
target_ns_add="target/native/testns/ns_add/ns_add"
target_ns_add_yaml="target/native/testns/ns_add/viash.yaml"

# 1. Run component with default arguments
# Run component
$meta_executable \
    --verbose \
    >$defaults_output

# Check if defaults output exists
[[ ! -f $defaults_output ]] && echo "Default: Test output file could not be found!" && exit 1

# Check if default arguments are as expected
grep -q "viash ns build --src src --parallel --write_meta --config_mod .functionality.version := 'dev' --setup cachedbuild" $defaults_output

# Check if target dir hierarchy exists
[[ ! -d $expected_target_dir ]] && echo "Default: target directory hierarchy could not be found!" && exit 1

# Remove target dir
rm -r target

# 2. Run component with custom arguments
# Copy src dir
cp -r src $alt_src

# Run component
$meta_executable \
    --verbose \
    --src $alt_src \
    --platform native \
    --mode release \
    --tag rc-1 \
    --query_namespace 'testns' \
    --query_name 'ns_a' \
    --registry 'my_registry' \
    --organization 'my_organization' \
    --target_image_source 'https://github.com/viash-io/viash' \
    --namespace_separator '*~*' \
    --max_threads 8 \
    --config_mod '.functionality.version := "5.0"' \
    --log $log

>$alt_output

# Check if alt output exists
[[ ! -f $alt_output ]] && echo "Alt: Test output file could not be found!" && exit 1

# Only ns_add should be included because of the query mame
[[ -d "$expected_target_dir/ns_divide" ]] && echo "The ns_divide component shouldn't have been built!" && exit 1

# The build log should contain an error as ns_error can't be built
grep -q "Reading file 'alt_src/ns_error/config.vsh.yaml' failed" $log

# Check if target dir hierarchy exists
[[ ! -d $expected_target_dir ]] && echo "Alt: target directory hierarchy could not be found!" && exit 1

# Check if ns_add exists in target dir
[[ ! -f $target_ns_add ]] && echo "Alt: ns_add couldn't be found in target directory!" && exit 1

# Check if the version of ns_add is changed to 5.0
grep -q "ns_add 5.0" $target_ns_add

# Check if the namespace of ns_add is testns
grep -q 'namespace: "testns"' $target_ns_add_yaml

echo ">>> Test finished successfully"
