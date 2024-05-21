#!/usr/bin/env bash

# VIASH START
par_input="code.sh"
par_real_number="123.987654"
par_whole_number="17"
par_s="test string"
par_truth="true"
par_falsehood="false"
par_reality=""
par_output="output.txt"
par_multiple_output="output*.txt"
par_log="log.txt"
par_optional="help"
par_optional_with_default="me"
meta_functionality_name="testbash"
meta_resources_dir="."
# VIASH END

set -e

function log {
  if [ -z "$par_log" ]; then
    echo $@
  else
    echo $@ >> $par_log
  fi
}
function output {
  if [ -z "$par_output" ]; then
    echo $@
  else
    echo $@ >> $par_output
  fi
}

log "INFO: Parsed input arguments."

if [ -z "$par_output" ]; then
  log "INFO: Printing output to console"
else
  log "INFO: Writing output to file"
fi

output "input: |$par_input|"
output "real_number: |$par_real_number|"
output "whole_number: |$par_whole_number|"
output "long_number: |$par_long_number|"
output "s: |$par_s|"
output "truth: |$par_truth|"
output "falsehood: |$par_falsehood|"
output "reality: |$par_reality|"
output "output: |$par_output|"
output "log: |$par_log|"
output "optional: |$par_optional|"
output "optional_with_default: |$par_optional_with_default|"
output "multiple: |$par_multiple|"
output "multiple_pos: |$par_multiple_pos|"
output "multiple_output: |$par_multiple_output|"
output "meta_resources_dir: |$meta_resources_dir|"

multiple_output_i=0
if [ ! -z "$par_multiple_output" ]; then
  multiple_output_file=$(echo $par_multiple_output | sed "s/\*/$multiple_output_i/")
  cp "$par_input" "$multiple_output_file"
  multiple_output_i=$((multiple_output_i+1))

  multiple_output_file=$(echo $par_multiple_output | sed "s/\*/$multiple_output_i/")
  cp "$par_input" "$multiple_output_file"
  multiple_output_i=$((multiple_output_i+1))
fi

