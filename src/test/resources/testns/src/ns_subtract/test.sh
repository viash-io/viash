#!/bin/bash

set -ex

./ns_subtract --input1 42 --input2 5 --output output_sub.txt

[[ ! -f output_sub.txt ]] && echo "Output file could not be found!" && exit 1
grep -q 'input1: 42' output_sub.txt
grep -q 'input2: 5' output_sub.txt
grep -q 'result: 37' output_sub.txt

echo ">>> Test finished successfully"
exit 0
