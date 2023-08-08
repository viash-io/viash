#!/usr/bin/env bash
set -ex

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
grep -q 'input: |resource1.txt|' output.txt
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
grep -q 'meta_functionality_name: |test_languages_.*|' output.txt
grep -q 'meta_resources_dir: |..*|' output.txt
grep -q 'meta_cpus: |2|' output.txt
grep -q 'meta_memory_b: |2147483648|' output.txt
grep -q 'meta_memory_kb: |2097152|' output.txt
grep -q 'meta_memory_mb: |2048|' output.txt
grep -q 'meta_memory_gb: |2|' output.txt
grep -q 'meta_memory_tb: |1|' output.txt
grep -q 'meta_memory_pb: |1|' output.txt

grep -q 'head of input: |if you can read this,|' output.txt
grep -q 'head of resource1: |if you can read this,|' output.txt

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
grep -q 'input: |resource2.txt|' output2.txt
grep -q 'real_number: |123.456|' output2.txt
grep -q 'whole_number: |789|' output2.txt
grep -q 'long_number: ||' output2.txt
grep -q "s: |a \\\\ b \\\$ c \` d \" e ' f \\\\n g # h @ i { j } k \"\"\" l ''' m todo_add_back_DOLLAR_VIASH_TEMP n : o ; p|" output2.txt
grep -q 'truth: |false|' output2.txt
grep -q 'falsehood: |true|' output2.txt
grep -q 'reality: ||' output2.txt
grep -q 'output: ||' output2.txt
grep -q 'log: ||' output2.txt
grep -q 'optional: ||' output2.txt
grep -q 'optional_with_default: |The default value.|' output2.txt
grep -q 'multiple: ||' output2.txt
grep -q 'multiple_pos: ||' output2.txt

grep -q 'meta_functionality_name: |test_languages_.*|' output2.txt
grep -q 'meta_resources_dir: |..*|' output2.txt
grep -q 'meta_cpus: |666|' output2.txt
grep -q 'meta_memory_b: |112589990684262400|' output2.txt
grep -q 'meta_memory_kb: |109951162777600|' output2.txt
grep -q 'meta_memory_mb: |107374182400|' output2.txt
grep -q 'meta_memory_gb: |104857600|' output2.txt
grep -q 'meta_memory_tb: |102400|' output2.txt
grep -q 'meta_memory_pb: |100|' output2.txt

grep -q 'head of input: |this file is only for testing|' output2.txt
grep -q 'head of resource1: |if you can read this,|' output2.txt


if [[ $meta_functionality_name == "bash" || $meta_functionality_name == "js" ]]; then
# This currently only works fully on bash and javascript

  echo ">>> Try to unset defaults"
  "$meta_executable" \
    "resource2.txt" \
    --real_number 123.456 \
    --whole_number=789 \
    -s "my\$weird#string\"\"\"" \
    ---cpus "" \
    ---memory "" \
    > output4.txt

  [[ ! -f output4.txt ]] && echo "Output file could not be found!" && exit 1
  grep -q 'meta_cpus: ||' output4.txt
  grep -q 'meta_memory_b: ||' output4.txt
  grep -q 'meta_memory_kb: ||' output4.txt
  grep -q 'meta_memory_mb: ||' output4.txt
  grep -q 'meta_memory_gb: ||' output4.txt
  grep -q 'meta_memory_tb: ||' output4.txt
  grep -q 'meta_memory_pb: ||' output4.txt

fi

echo ">>> Test finished successfully"
