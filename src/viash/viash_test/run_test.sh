#!/usr/bin/env bash

set -ex

defaults_output="defaults_test_output.txt"

alt_output=alt_test_output.txt
alt_src="alt_src"
log=build_log.txt
tsv=output.tsv

# 1. Run component with default arguments
# Run component
set +e
$meta_executable \
    --verbose \
    >$defaults_output

exit_code="$?"
set -e

[ $exit_code -ne 1 ] && echo "Expected exit code 1 but received $exit_code" && exit 1

# Check if defaults output exists
[[ ! -f $defaults_output ]] && echo "Default: Test output file could not be found!" && exit 1

# # Check if default arguments are as expected
grep -q "viash ns test --src src --parallel --config_mod .functionality.version := 'dev' --config_mod .platforms\[.type == 'docker'\].setup_strategy := 'cachedbuild' --platform docker" $defaults_output

# 2. Run component with custom arguments
# Copy src dir
cp -r src $alt_src

# Run component
unset exit_code
set +e
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
    --log $log \
    --tsv $tsv \
    >$alt_output

exit_code="$?"
set -e
[ $exit_code -ne 1 ] && echo "Expected exit code 0 but received $exit_code" && exit 1

# Check if alt output exists
[[ ! -f $alt_output ]] && echo "Alt: Test output file could not be found!" && exit 1

# The build log should contain an error as ns_error can't be built
grep -q "Reading file 'alt_src/ns_error/config.vsh.yaml' failed" $log

# Check if tsv exists
[[ ! -f $tsv ]] && echo "TSV not found!" && exit 1

# Check if ns_add was tested
grep -q "ns_add" $tsv

# Check if the test was succesful was tested
grep -q "SUCCESS" $tsv

# Check if the namespace of ns_add is testns
grep -q "testns" $tsv

echo ">>> Test finished successfully"
