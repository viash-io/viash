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
par_multiple_output="output_*.txt"
par_log="log.txt"
par_optional="help"
par_optional_with_default="me"
meta_resources_dir="."
# VIASH END

par_out_1=`echo "$par_multiple_output" | sed 's/\*/1/g'`
par_out_2=`echo "$par_multiple_output" | sed 's/\*/2/g'`
par_out_3=`echo "$par_multiple_output" | sed 's/\*/3/g'`

echo "input: |$par_input|" > $par_out_1
echo "real_number: |$par_real_number|" > $par_out_2
echo "whole_number: |$par_whole_number|" > $par_out_3
