#!/usr/bin/env bash
set -x

echo ">>> This test should always fail but that is to be expected"
./testbash "missingresource" --real_number abc --whole_number abc -s "a string with spaces" --truth \
  --output ./output_fail.txt --log ./log.txt \
  --optional foo --optional_with_default bar

[ $? -eq 0 ] && echo "Command should have failed, but didn't." && exit 1

echo Done
