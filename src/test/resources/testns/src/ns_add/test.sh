#!/bin/bash

set -ex

./ns_add --input1 10 --input2 15 --output output.txt

[[ ! -f output.txt ]] && echo "Output file could not be found!" && exit 1
grep -q 'input1: 10' output.txt
grep -q 'input2: 15' output.txt
grep -q 'result: 25' output.txt

echo ">>> Test finished successfully"
exit 0
