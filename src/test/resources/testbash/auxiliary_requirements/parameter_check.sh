#!/usr/bin/env bash

# VIASH START
par_input="code.sh"
par_real_number="123.987654"
par_whole_number="17"
par_string="test string"
par_reality=""
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
output "whole_number: |$par_whole_number|"
output "string: |$par_string|"
output "reality: |$par_reality|"
