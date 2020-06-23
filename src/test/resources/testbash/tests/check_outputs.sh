#!/usr/bin/env bash
set -ex

echo ">>> Checking whether output is correct"
./testbash "resource1.txt" --real_number 10.5 --whole_number=10 -s "a string with spaces" --truth \
  --output ./output.txt --log ./log.txt \
  --optional foo --optional_with_default bar --passthrough='you shall#not$pass' --passthroughbool

[[ ! -f output.txt ]] && echo "Output file could not be found!" && exit 1
grep -q 'input: "..*/resource1.txt"' output.txt
grep -q 'real_number: "10.5"' output.txt
grep -q 'whole_number: "10"' output.txt
grep -q 's: "a string with spaces"' output.txt
grep -q 'truth: "true"' output.txt
grep -q 'output: ".*/output.txt"' output.txt
grep -q 'log: ".*/log.txt"' output.txt
grep -q 'optional: "foo"' output.txt
grep -q 'optional_with_default: "bar"' output.txt
grep -q 'passthrough: "you shall#not$pass"' output.txt
grep -q 'passthrough: ".*pass"' output.txt
grep -q 'passthroughbool: "true"' output.txt
grep -q 'resources_dir: "..*"' output.txt
grep -q 'head of input: "if you can read this,"' output.txt
grep -q 'head of resource1: "if you can read this,"' output.txt

[[ ! -f log.txt ]] && echo "Log file could not be found!" && exit 1
grep -q 'Parsed input arguments.' log.txt

echo ">>> Checking whether output is correct with minimal parameters"
./testbash "resource2.txt" --real_number 123.456 --whole_number=789 -s 'my$weird#string' \
  > output2.txt

[[ ! -f output2.txt ]] && echo "Output file could not be found!" && exit 1
grep -q 'input: "..*/resource2.txt"' output2.txt
grep -q 'real_number: "123.456"' output2.txt
grep -q 'whole_number: "789"' output2.txt
grep -q 's: "my$weird#string"' output2.txt
grep -q 'truth: "false"' output2.txt
grep -q 'output: ""' output2.txt
grep -q 'log: ""' output2.txt
grep -q 'optional: ""' output2.txt
grep -q 'optional_with_default: "The default value."' output2.txt
grep -q 'passthrough: ""' output2.txt
grep -q 'passthroughbool: "false"' output2.txt
grep -q 'resources_dir: "..*"' output2.txt
grep -q 'head of input: "this file is only for testing"' output2.txt
grep -q 'head of resource1: "if you can read this,"' output2.txt

echo ">>> Test finished successfully"
