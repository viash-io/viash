#!/usr/bin/env bash
set -ex

echo ">>> Checking whether output is correct"
$meta_executable "NOTICE" --real_number 10.5 --whole_number=10 -s "a string with spaces" --truth \
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
grep -q 'truth: |True|' output.txt
grep -q 'output: |.*/output.txt|' output.txt
grep -q 'log: |.*/log.txt|' output.txt
grep -q 'optional: |foo|' output.txt
grep -q 'optional_with_default: |bar|' output.txt
grep -q 'multiple: |one,two|' output.txt
grep -q 'multiple_pos: |a,b,c,d,e,f|' output.txt
grep -q 'meta_resources_dir: |..*|' output.txt
grep -q 'meta_functionality_name: |testpython|' output.txt
grep -q 'meta_n_proc: |None|' output.txt
grep -q 'meta_memory_b: |None|' output.txt
grep -q 'meta_memory_kb: |None|' output.txt
grep -q 'meta_memory_mb: |None|' output.txt
grep -q 'meta_memory_gb: |None|' output.txt
grep -q 'meta_memory_tb: |None|' output.txt
grep -q 'meta_memory_pb: |None|' output.txt

[[ ! -f log.txt ]] && echo "Log file could not be found!" && exit 1
grep -q 'Parsed input arguments.' log.txt

echo ">>> Checking whether output is correct with minimal parameters"
$meta_executable \
  "resource2.txt" \
  --real_number 123.456 \
  --whole_number=789 \
  -s "a \\ b \$ c \` d \" e ' f \n g # h @ i { j } k \"\"\" l ''' m \$VIASH_TEMP n : o ; p" \
  ---n_proc 666 \
  ---memory 100PB \
  > output2.txt

[[ ! -f output2.txt ]] && echo "Output file could not be found!" && exit 1
grep -q 'input: |resource2.txt|' output2.txt
grep -q 'real_number: |123.456|' output2.txt
grep -q 'whole_number: |789|' output2.txt
grep -q 'long_number: |None|' output2.txt
grep -q "s: |a \\\\ b \\\$ c \` d \" e ' f \\\\n g # h @ i { j } k \"\"\" l ''' m \\\$VIASH_TEMP n : o ; p" output2.txt
grep -q 'truth: |False|' output2.txt
grep -q 'output: |None|' output2.txt
grep -q 'log: |None|' output2.txt
grep -q 'optional: |None|' output2.txt
grep -q 'optional_with_default: |The default value.|' output2.txt
grep -q 'multiple: |None|' output2.txt
grep -q 'multiple_pos: |None|' output2.txt
grep -q 'meta_resources_dir: |..*|' output2.txt
grep -q 'meta_functionality_name: |testpython|' output2.txt
grep -q 'meta_n_proc: |666|' output2.txt
grep -q 'meta_memory_b: |112589990684262400|' output2.txt
grep -q 'meta_memory_kb: |109951162777600|' output2.txt
grep -q 'meta_memory_mb: |107374182400|' output2.txt
grep -q 'meta_memory_gb: |104857600|' output2.txt
grep -q 'meta_memory_tb: |102400|' output2.txt
grep -q 'meta_memory_pb: |100|' output2.txt

echo ">>> Test finished successfully"
