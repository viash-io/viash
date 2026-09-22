#!/bin/bash

# regression test for https://github.com/viash-io/viash/issues/908
# a line starting with '|' used to be dropped from the generated script.
false \
|| echo_exit_code="$?"
echo "exit code: $echo_exit_code" > "$par_output"
