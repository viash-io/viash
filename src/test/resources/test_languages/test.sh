#!/usr/bin/env bash
set -e

## VIASH START
## VIASH END

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

# Set up temporary directory and environment variables for the test
export VIASH_KEEP_WORK_DIR=silent
export VIASH_TEMP=$meta_temp_dir/temp
mkdir -p $VIASH_TEMP

echo ">>> Checking whether expected resources exist"
[[ ! -f "$meta_executable" ]] && echo "executable could not be found!" && exit 1
[[ ! -f "$meta_resources_dir/.config.vsh.yaml" ]] && echo ".config.vsh.yaml could not be found!" && exit 1
[[ ! -f "$meta_config" ]] && echo ".config.vsh.yaml could not be found!" && exit 1

echo ">>> Checking whether output is correct"
"$meta_executable" "resource1.txt" --real_number 10.5 --whole_number=10 -s "a string with spaces" \
  --truth --falsehood --reality true \
  --optional foo --optional_with_default bar \
  a b c d \
  --output ./output.txt --log ./log.txt \
  --multiple one --multiple=two \
  e f \
  --long_number 112589990684262400

[[ ! -f output.txt ]] && echo "Output file could not be found!" && exit 1
check_output 'input: |resource1.txt|' output.txt
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
check_output 'meta_name: |test_languages_.*|' output.txt
check_output 'meta_resources_dir: |..*|' output.txt
check_output 'meta_cpus: |2|' output.txt
check_output 'meta_memory_b: |2000000000|' output.txt
check_output 'meta_memory_kb: |2000000|' output.txt
check_output 'meta_memory_mb: |2000|' output.txt
check_output 'meta_memory_gb: |2|' output.txt
check_output 'meta_memory_tb: |1|' output.txt
check_output 'meta_memory_pb: |1|' output.txt
check_output 'meta_memory_kib: |1953125|' output.txt
check_output 'meta_memory_mib: |1908|' output.txt
check_output 'meta_memory_gib: |2|' output.txt
check_output 'meta_memory_tib: |1|' output.txt
check_output 'meta_memory_pib: |1|' output.txt

check_output 'head of input: |if you can read this,|' output.txt
check_output 'head of resource1: |if you can read this,|' output.txt

[[ ! -f log.txt ]] && echo "Log file could not be found!" && exit 1
grep -q 'Parsed input arguments.' log.txt

echo ">>> Checking whether output is correct with minimal parameters"
"$meta_executable" \
  "resource2.txt" \
  --real_number 123.456 \
  --whole_number=789 \
  -s "a \\ b \$ c \` d \" e ' f \n g # h @ i { j } k \"\"\" l ''' m todo_add_back_DOLLAR_VIASH_TEMP n : o ; p" \
  ---cpus 666 \
  ---memory 100PB \
  > output2.txt

[[ ! -f output2.txt ]] && echo "Output file could not be found!" && exit 1
check_output 'input: |resource2.txt|' output2.txt
check_output 'real_number: |123.456|' output2.txt
check_output 'whole_number: |789|' output2.txt
check_output 'long_number: ||' output2.txt
check_output "s: |a \\\\ b \\\$ c \` d \" e ' f \\\\n g # h @ i { j } k \"\"\" l ''' m todo_add_back_DOLLAR_VIASH_TEMP n : o ; p|" output2.txt
check_output 'truth: |false|' output2.txt
check_output 'falsehood: |true|' output2.txt
check_output 'reality: ||' output2.txt
check_output 'output: ||' output2.txt
check_output 'log: ||' output2.txt
check_output 'optional: ||' output2.txt
check_output 'optional_with_default: |The default value.|' output2.txt
check_output 'multiple: ||' output2.txt
check_output 'multiple_pos: ||' output2.txt

check_output 'meta_name: |test_languages_.*|' output2.txt
check_output 'meta_resources_dir: |..*|' output2.txt
check_output 'meta_cpus: |666|' output2.txt
check_output 'meta_memory_b: |100000000000000000|' output2.txt
check_output 'meta_memory_kb: |100000000000000|' output2.txt
check_output 'meta_memory_mb: |100000000000|' output2.txt
check_output 'meta_memory_gb: |100000000|' output2.txt
check_output 'meta_memory_tb: |100000|' output2.txt
check_output 'meta_memory_pb: |100|' output2.txt
check_output 'meta_memory_kib: |97656250000000|' output2.txt
check_output 'meta_memory_mib: |95367431641|' output2.txt
check_output 'meta_memory_gib: |93132258|' output2.txt
check_output 'meta_memory_tib: |90950|' output2.txt
check_output 'meta_memory_pib: |89|' output2.txt

check_output 'head of input: |this file is only for testing|' output2.txt
check_output 'head of resource1: |if you can read this,|' output2.txt


echo ">>> Checking whether output is correct with minimal parameters, but with 1024-base memory"
"$meta_executable" \
  "resource2.txt" \
  --real_number 123.456 \
  --whole_number=789 \
  -s "a \\ b \$ c \` d \" e ' f \n g # h @ i { j } k \"\"\" l ''' m todo_add_back_DOLLAR_VIASH_TEMP n : o ; p" \
  ---cpus 666 \
  ---memory 100PiB \
  > output2.txt

check_output 'meta_memory_b: |112589990684262400|' output2.txt
check_output 'meta_memory_kb: |112589990684263|' output2.txt
check_output 'meta_memory_mb: |112589990685|' output2.txt
check_output 'meta_memory_gb: |112589991|' output2.txt
check_output 'meta_memory_tb: |112590|' output2.txt
check_output 'meta_memory_pb: |113|' output2.txt
check_output 'meta_memory_kib: |109951162777600|' output2.txt
check_output 'meta_memory_mib: |107374182400|' output2.txt
check_output 'meta_memory_gib: |104857600|' output2.txt
check_output 'meta_memory_tib: |102400|' output2.txt
check_output 'meta_memory_pib: |100|' output2.txt

# Test unsetting defaults using UNDEFINED (issue #375)
# With JSON-based parameter passing, this now works in all languages
echo ">>> Try to unset defaults"
"$meta_executable" \
  "resource2.txt" \
  --real_number 123.456 \
  --whole_number=789 \
  -s "my\$weird#string\"\"\"" \
  ---cpus UNDEFINED \
  ---memory UNDEFINED \
  > output4.txt

[[ ! -f output4.txt ]] && echo "Output file could not be found!" && exit 1
check_output 'meta_cpus: ||' output4.txt
check_output 'meta_memory_b: ||' output4.txt
check_output 'meta_memory_kb: ||' output4.txt
check_output 'meta_memory_mb: ||' output4.txt
check_output 'meta_memory_gb: ||' output4.txt
check_output 'meta_memory_tb: ||' output4.txt
check_output 'meta_memory_pb: ||' output4.txt

# Test backslash-quote escaping (issue #821)
# The sequence \' was previously breaking Python syntax
echo ">>> Test backslash-quote escaping"
"$meta_executable" \
  "resource2.txt" \
  --real_number 123.456 \
  --whole_number=789 \
  -s "test\\'value" \
  > output5.txt

[[ ! -f output5.txt ]] && echo "Output file could not be found!" && exit 1
check_output "s: |test\\\\'value|" output5.txt

echo ">>> Test finished successfully"
