#!/usr/bin/env bash
set -ex # exit the script when one of the checks fail.

# check 1
echo ">>> Checking whether output is correct"
./hello_world I am viash! > output.txt

[[ ! -f output.txt ]] && echo "Output file could not be found!" && exit 1
grep -q 'Hello world! I am viash!' output.txt

# check 2
echo ">>> Checking whether output is correct when no parameters are given"
./hello_world > output2.txt

[[ ! -f output2.txt ]] && echo "Output file could not be found!" && exit 1
grep -q 'Hello world!' output2.txt

echo ">>> Test finished successfully!"

# check 3
echo ">>> Checking whether output is correct when more parameters are given"
./hello_world General Kenobi. --greeter="Hello there." > output3.txt

[[ ! -f output3.txt ]] && echo "Output file could not be found!" && exit 1
grep -q 'Hello there. General Kenobi.' output3.txt

echo ">>> Test finished successfully!"


