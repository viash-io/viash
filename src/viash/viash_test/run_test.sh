#!/usr/bin/env bash

set -ex

defaults_output=test_output.txt

# Check defaults
./$meta_functionality_name \
    --verbose \
    >$defaults_output

# Check if defaults output exists
[[ ! -f $defaults_output ]] && echo "Test output file could not be found!" && exit 1

echo "Command output: "
cat $defaults_output

# Check if default arguments are as expected
#grep -q "viash ns build --src src --parallel --write_meta --config_mod .functionality.version := 'dev' --setup cachedbuild" $defaults_output

echo ">>> Test finished successfully"
