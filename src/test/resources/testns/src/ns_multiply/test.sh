#!/bin/bash

set -ex

./ns_multiply --input1 6 --input2 3 --output output_mult.txt

[[ ! -f output_mult.txt ]] && echo "Output file could not be found!" && exit 1
grep -q 'input1: 6' output_mult.txt
grep -q 'input2: 3' output_mult.txt
grep -q 'result: 18' output_mult.txt

echo ">>> Test finished successfully"
exit 0
