#!/usr/bin/env bash
set -ex

echo ">>> Checking whether expected resources exist"
[[ ! -f "$meta_executable" ]] && echo "executable could not be found!" && exit 1
[[ ! -f "$meta_resources_dir/.config.vsh.yaml" ]] && echo ".config.vsh.yaml could not be found!" && exit 1
[[ ! -f "$meta_config" ]] && echo ".config.vsh.yaml could not be found!" && exit 1

echo ">>> Checking whether output is correct"
"$meta_executable" "NOTICE" --real_number 10.5 --whole_number=10 -s "a string with spaces" \
  --truth --falsehood --reality true \
  --optional foo --optional_with_default bar \
  a b c d \
  --output ./output.txt --log ./log.txt \
  --multiple one --multiple=two \
  e f \
  --long_number 112589990684262400

[[ ! -f output.txt ]] && echo "Output file could not be found!" && exit 1
grep -q 'input: |NOTICE|' output.txt
grep -q 'real_number: |10.5|' output.txt
grep -q 'whole_number: |10|' output.txt
grep -q 'long_number: |112589990684262400|' output.txt
grep -q 's: |a string with spaces|' output.txt
grep -q 'truth: |true|' output.txt
grep -q 'falsehood: |false|' output.txt
grep -q 'reality: |true|' output.txt
grep -q 'output: |.*/output.txt|' output.txt
grep -q 'log: |.*/log.txt|' output.txt
grep -q 'optional: |foo|' output.txt
grep -q 'optional_with_default: |bar|' output.txt
grep -q 'multiple: |one:two|' output.txt
grep -q 'multiple_pos: |a:b:c:d:e:f|' output.txt

echo ">>> Test finished successfully"
