#!/usr/bin/env bash
set -ex

echo ">>> Checking whether expected resources exist"
[[ ! -f "$meta_executable" ]] && echo "executable could not be found!" && exit 1
[[ ! -f "$meta_resources_dir/.config.vsh.yaml" ]] && echo ".config.vsh.yaml could not be found!" && exit 1
[[ ! -f "$meta_config" ]] && echo ".config.vsh.yaml could not be found!" && exit 1

echo ">>> Checking whether output is correct"
"$meta_executable" "resource1.txt" --real_number 10.5 --whole_number=10 -s "a string with spaces" \
  1.1 2.2 3.3 4.4 \
  --output ./output.txt --log ./log.txt \
  --multiple 5.5 --multiple=38.1 \
  123.123 456.456 \
  --long_number 112589990684262400

[[ ! -f output.txt ]] && echo "Output file could not be found!" && exit 1
grep -q 'input: |resource1.txt|' output.txt
grep -q 'real_number: |10.5|' output.txt
grep -q 'whole_number: |10|' output.txt
grep -q 'long_number: |112589990684262400|' output.txt
grep -q 's: |a string with spaces|' output.txt
grep -q 'multiple: |5.5:38.1|' output.txt
grep -q 'multiple_pos: |1.1:2.2:3.3:4.4:123.123:456.456|' output.txt
echo ">>> Test finished successfully"
