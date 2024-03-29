#!/usr/bin/env bash

# VIASH START
par_input="code_multiple_output.sh"
par_real_number="123.987654"
par_whole_number="17"
par_s="test string"
par_truth="true"
par_falsehood="false"
par_reality=""
par_output="output.txt"
par_log="log.txt"
par_optional="help"
par_optional_with_default="me"
meta_resources_dir="."
# VIASH END

set -e

function log {
  if [ -z "$par_log" ]; then
    echo "$@"
  else
    echo "$@" >> $par_log
  fi
}
function output {
  if [ -z "$par_output" ]; then
    echo "$@"
  else
    echo "$@" >> $par_output

    if [ ! -z "$par_output_pos" ]; then
      IFS=":"
      for var in $par_output_pos; do
        unset IFS
        echo "$@" >> $var
      done
    fi

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
output "s: |$par_s|"
output "truth: |$par_truth|"
output "falsehood: |$par_falsehood|"
output "reality: |$par_reality|"
output "output: |$par_output|"
output "log: |$par_log|"
output "optional: |$par_optional|"
output "optional_with_default: |$par_optional_with_default|"
output "meta_resources_dir: |$meta_resources_dir|"
INPUT=`head -1 "$par_input"`
output "head of input: |$INPUT|"
RESOURCE=`head -1 "$meta_resources_dir/resource1.txt"`
output "head of resource1: |$RESOURCE|"
output "output_pos: |$par_output_pos|"
