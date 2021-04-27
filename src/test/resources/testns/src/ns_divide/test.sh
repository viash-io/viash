#!/bin/bash

set -ex

./ns_divide --input1 8 --input2 2 --output output_div.txt

[[ ! -f output_div.txt ]] && echo "Output file could not be found!" && exit 1
grep -q 'input1: 8' output_div.txt
grep -q 'input2: 2' output_div.txt
grep -q 'result: 4' output_div.txt

echo ">>> Test finished successfully"
exit 0
