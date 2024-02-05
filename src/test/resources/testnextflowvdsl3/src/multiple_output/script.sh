#!/bin/bash

mkdir -p $par_output

if [ ! -z "$par_input" ]; then
  IFS=":"
  for var in $par_input; do
    unset IFS
    base=$(basename $var .txt)
    output="$par_output/${base}_copy.txt"
    cp "$var" "$output"
  done
fi
