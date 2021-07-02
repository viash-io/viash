#!/bin/bash

set -ex

./ns_add --input1 10 --input2 15 --output output_add.txt

[[ ! -f output_add.txt ]] && echo "Output file could not be found!" && exit 1
grep -q 'input1: 10' output_add.txt
grep -q 'input2: 15' output_add.txt
grep -q 'result: 25' output_add.txt

echo ">>> Test finished successfully"
exit 0
