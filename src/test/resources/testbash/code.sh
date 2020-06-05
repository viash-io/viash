#!/usr/bin/env bash

# VIASH START
par_input="code.sh"
par_real_number="123.987654"
par_whole_number="17"
par_s="test string"
par_truth="true"
par_output="output.txt"
par_log="log.txt"
par_optional="help"
par_optional_with_default="me"
par_passthrough=test
par_passthroughbool=true
PASSTHROUGH="--passthrough test"
resources_dir="."
# VIASH END

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

output "input: \"$par_input\""
output "real_number: \"$par_real_number\""
output "whole_number: \"$par_whole_number\""
output "s: \"$par_s\""
output "truth: \"$par_truth\""
output "output: \"$par_output\""
output "log: \"$par_log\""
output "optional: \"$par_optional\""
output "optional_with_default: \"$par_optional_with_default\""
output "passthrough: \"$par_passthrough\""
output "passthroughbool: \"$par_passthroughbool\""
output "resources_dir: \"$resources_dir\""
output "head of input: \"`head -1 $par_input`\""
output "head of resource1: \"`head -1 $resources_dir/resource1.txt`\""
if [ ! -z "$par_data" ]; then
  output "data: \"$par_data\""
fi
output "PASSTHROUGH: \"$PASSTHROUGH\""
