#!/usr/bin/env bash
set -ex

# Helper function to check output with verbose error messages
check_output() {
  local pattern="$1"
  local file="$2"
  if ! grep -q "$pattern" "$file"; then
    echo "FAILED: Pattern not found: $pattern"
    echo "Actual content of relevant lines:"
    # Try to show relevant lines by extracting the key name
    local key=$(echo "$pattern" | sed 's/[:|].*//; s/.*|//')
    grep "$key" "$file" || echo "(no lines matching '$key' found)"
    echo "---"
    return 1
  fi
}

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
  --long_number 112589990684262400 \
  --multiple_output './output_*.txt'

[[ ! -f output.txt ]] && echo "Output file could not be found!" && exit 1
check_output 'input: |NOTICE|' output.txt
check_output 'real_number: |10.5|' output.txt
check_output 'whole_number: |10|' output.txt
check_output 'long_number: |112589990684262400|' output.txt
check_output 's: |a string with spaces|' output.txt
check_output 'truth: |true|' output.txt
check_output 'falsehood: |false|' output.txt
check_output 'reality: |true|' output.txt
check_output 'output: |.*/output.txt|' output.txt
check_output 'log: |.*/log.txt|' output.txt
check_output 'optional: |foo|' output.txt
check_output 'optional_with_default: |bar|' output.txt
check_output 'multiple: |one;two|' output.txt
check_output 'multiple_pos: |a;b;c;d;e;f|' output.txt
check_output 'multiple_output: |.*/output_\*.txt|' output.txt

echo ">>> Test finished successfully"
