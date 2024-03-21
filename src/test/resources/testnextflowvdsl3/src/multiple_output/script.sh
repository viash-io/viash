#!/bin/bash

## VIASH START
par_input='resources/lines3.txt;resources/lines5.txt'
par_output='output_*.txt'
## VIASH END

output_i=0

if [ ! -z "$par_input" ]; then
  IFS=";"
  for var in $par_input; do
    unset IFS
    output=$(echo "$par_output" | sed "s/\*/$output_i/g")
    cp "$var" "$output"
    output_i=$((output_i+1))
  done
fi
