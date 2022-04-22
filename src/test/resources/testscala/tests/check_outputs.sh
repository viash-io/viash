#!/usr/bin/env bash
set -ex

echo ">>> Checking whether output is correct"
./testscala "NOTICE" --real_number 10.5 --whole_number=10 -s "a string with spaces" --truth \
  --optional foo --optional_with_default bar \
  a b c d \
  --output ./output.txt --log ./log.txt \
  --multiple one --multiple=two \
  e f

[[ ! -f output.txt ]] && echo "Output file could not be found!" && exit 1
grep -q 'input: |Some(NOTICE)|' output.txt
grep -q 'real_number: |10.5|' output.txt
grep -q 'whole_number: |10|' output.txt
grep -q 's: |a string with spaces|' output.txt
grep -q 'truth: |true|' output.txt
grep -q 'output: |Some(.*/output.txt)|' output.txt
grep -q 'log: |Some(.*/log.txt)|' output.txt
grep -q 'optional: |Some(foo)|' output.txt
grep -q 'optional_with_default: |Some(bar)|' output.txt
grep -q 'multiple: |List(one, two)|' output.txt
grep -q 'multiple_pos: |List(a, b, c, d, e, f)|' output.txt
grep -q 'resources_dir: |..*|' output.txt
grep -q 'meta_resources_dir: |..*|' output.txt
grep -q 'meta_functionality_name: |testscala|' output.txt

[[ ! -f log.txt ]] && echo "Log file could not be found!" && exit 1
grep -q 'Parsed input arguments.' log.txt

echo ">>> Checking whether output is correct with minimal parameters"
./testscala "resource2.txt" --real_number 123.456 --whole_number=789 -s "my\$weird#string\"\"\"'''\`@" \
  > output2.txt

[[ ! -f output2.txt ]] && echo "Output file could not be found!" && exit 1
grep -q 'input: |Some(resource2.txt)|' output2.txt
grep -q 'real_number: |123.456|' output2.txt
grep -q 'whole_number: |789|' output2.txt
grep -q "s: |my\$weird#string\"\"\"'''\`@|" output2.txt
grep -q 'truth: |false|' output2.txt
grep -q 'output: |None|' output2.txt
grep -q 'log: |None|' output2.txt
grep -q 'optional: |None|' output2.txt
grep -q 'optional_with_default: |Some(The default value.)|' output2.txt
grep -q 'multiple: |List()|' output2.txt
grep -q 'multiple_pos: |List()|' output2.txt
grep -q 'resources_dir: |..*|' output2.txt
grep -q 'meta_resources_dir: |..*|' output2.txt
grep -q 'meta_functionality_name: |testscala|' output2.txt

echo ">>> Test finished successfully"
