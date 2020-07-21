#!/bin/bash

# VIASH START
par_string="myword"
par_real_number="123.987654"
par_whole_number="17"
par_truth="true"
par_output="output.txt"
# VIASH END

cat > "$par_output" << HERE
string: $par_string
real_number: $par_real_number
whole_number: $par_whole_number
truth: $par_truth
output: $par_output
HERE
