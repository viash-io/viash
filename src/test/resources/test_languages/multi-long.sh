#!/usr/bin/env bash
set -ex

echo ">>> Checking whether expected resources exist"
[[ ! -f "$meta_executable" ]] && echo "executable could not be found!" && exit 1
[[ ! -f "$meta_resources_dir/.config.vsh.yaml" ]] && echo ".config.vsh.yaml could not be found!" && exit 1
[[ ! -f "$meta_config" ]] && echo ".config.vsh.yaml could not be found!" && exit 1

echo ">>> Checking whether output is correct"
"$meta_executable" "resource1.txt" --real_number 10.5 --whole_number=10 -s "a string with spaces" \
  446913741939 338239080089 864531271886 126957339937 \
  --output ./output.txt --log ./log.txt \
  --multiple 806082089013 --multiple=360278033202 \
  829243285718 694515245636 \
  --long_number 112589990684262400

[[ ! -f output.txt ]] && echo "Output file could not be found!" && exit 1
grep -q 'input: |resource1.txt|' output.txt
grep -q 'real_number: |10.5|' output.txt
grep -q 'whole_number: |10|' output.txt
grep -q 'long_number: |112589990684262400|' output.txt
grep -q 's: |a string with spaces|' output.txt
grep -q 'multiple: |806082089013;360278033202|' output.txt
grep -q 'multiple_pos: |446913741939;338239080089;864531271886;126957339937;829243285718;694515245636|' output.txt
echo ">>> Test finished successfully"
