#!/usr/bin/env bash
set -ex

echo ">>> Checking whether output is correct"
"$meta_executable" . > output.txt 2>&1

[[ ! -f output.txt ]] && echo "Output file could not be found!" && exit 1
grep -q 'test_languages_scala' output.txt
grep -q 'resource1.txt' output.txt
grep -q 'resource2.txt' output.txt

echo ">>> Test finished successfully"
