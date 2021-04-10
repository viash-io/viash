#!/bin/bash

set -ex

./ns_divide --input1 8 --input2 2 --output output.txt

[[ ! -f output.txt ]] && echo "Output file could not be found!" && exit 1
grep -q 'input1: 8' output.txt
grep -q 'input2: 2' output.txt
grep -q 'result: 4' output.txt

echo ">>> Test finished successfully"
exit 0
