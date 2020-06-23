#!/usr/bin/env bash
set -ex

echo ">>> Checking whether output is correct"
./testexecutable ls . > output.txt 2>&1

[[ ! -f output.txt ]] && echo "Output file could not be found!" && exit 1
grep -q 'testexecutable' output.txt
grep -q 'resource1.xt' output.txt

echo ">>> Test finished successfully"
