#!/bin/bash

set -ex

./ns_add --input1 10 --input2 15 --output output_dis.txt

[[ ! -f output_dis.txt ]] && echo "Output file could not be found!" && exit 1
grep -q 'input1: 10' output_dis.txt
grep -q 'input2: 15' output_dis.txt
grep -q 'result: 38' output_dis.txt

echo ">>> Test finished successfully"
exit 0
