#!/bin/bash

set -ex

./ns_multiply --input1 6 --input2 3 --output output.txt

[[ ! -f output.txt ]] && echo "Output file could not be found!" && exit 1
grep -q 'input1: 6' output.txt
grep -q 'input2: 3' output.txt
grep -q 'result: 18' output.txt

echo ">>> Test finished successfully"
exit 0
