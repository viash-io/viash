#!/bin/bash

set -ex

./ns_subtract --input1 42 --input2 5 --output output.txt

[[ ! -f output.txt ]] && echo "Output file could not be found!" && exit 1
grep -q 'input1: 42' output.txt
grep -q 'input2: 5' output.txt
grep -q 'result: 37' output.txt

echo ">>> Test finished successfully"
exit 0
