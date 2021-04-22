#!/bin/bash

set -ex

./ns_divide --input1 8 --input2 0 --output output_div0.txt

[[ ! -f output_div0.txt ]] && echo "Output file could not be found!" && exit 1
grep -q 'input1: 8' output_div0.txt
grep -q 'input2: 0' output_div0.txt
grep -q 'result: ?' output_div0.txt

echo ">>> Test finished successfully, however we should not get here!"
exit 0
