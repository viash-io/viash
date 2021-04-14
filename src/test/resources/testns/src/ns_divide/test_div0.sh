#!/bin/bash

set -ex

./ns_divide --input1 8 --input2 0 --output output.txt

[[ ! -f output.txt ]] && echo "Output file could not be found!" && exit 1
grep -q 'input1: 8' output.txt
grep -q 'input2: 0' output.txt
grep -q 'result: ?' output.txt

echo ">>> Test finished successfully, however we should not get here!"
exit 0
