#!/usr/bin/env bash

# VIASH START
par_input="code.sh"
par_real_number="123.987654"
par_whole_number="17"
par_reality=""
par_string="test string"
# VIASH END

set -e

function output {
  if [ -z "$par_output" ]; then
    echo $@
  else
    echo $@ >> $par_output
  fi
}

output "real_number: |$par_real_number|"
output "real_number_multiple: |$par_real_number_multiple|"
output "whole_number: |$par_whole_number|"
output "whole_number_multiple: |$par_whole_number_multiple|"
output "reality: |$par_reality|"
output "reality_multiple: |$par_reality_multiple|"
output "string: |$par_string|"
output "multiple: |$par_multiple|"
output "whole_number_choice: |$whole_number_choice|"
output "whole_number_choice_multiple: |$whole_number_choice_multiple|"
