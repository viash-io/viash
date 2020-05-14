#!/bin/bash
set -x
./testbash help --real_number 10.5 --whole_number=10 -s "a string with spaces" --truth --optional foo --optional_with_default bar --passthrough='you shall#not$pass'

./testbash help --real_number 10.5 --whole_number=10 -s "a string with spaces" --truth --output here.txt --log here.txt --optional foo --optional_with_default bar --passthrough='you shall#not$pass'
