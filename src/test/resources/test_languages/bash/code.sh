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
INPUT=`head -1 "$par_input"`
output "head of input: |$INPUT|"
RESOURCE=`head -1 "$meta_resources_dir/resource1.txt"`
output "head of resource1: |$RESOURCE|"
output "multiple: |$par_multiple|"
output "multiple_pos: |$par_multiple_pos|"

output "meta_name: |$meta_name|"
output "meta_functionality_name: |$meta_functionality_name|"
output "meta_resources_dir: |$meta_resources_dir|"
output "meta_cpus: |$meta_cpus|"
output "meta_memory_b: |$meta_memory_b|"
output "meta_memory_kb: |$meta_memory_kb|"
output "meta_memory_mb: |$meta_memory_mb|"
output "meta_memory_gb: |$meta_memory_gb|"
output "meta_memory_tb: |$meta_memory_tb|"
output "meta_memory_pb: |$meta_memory_pb|"
output "meta_memory_kib: |$meta_memory_kib|"
output "meta_memory_mib: |$meta_memory_mib|"
output "meta_memory_gib: |$meta_memory_gib|"
output "meta_memory_tib: |$meta_memory_tib|"
output "meta_memory_pib: |$meta_memory_pib|"