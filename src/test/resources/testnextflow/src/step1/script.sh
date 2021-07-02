#!/bin/test

[ -f "$par_output" ] && rm "$par_output"

if [ ! -z "$par_input" ]; then
  IFS=":"
  for var in $par_input; do
    unset IFS
    cat "$var" >> "$par_output"
  done
fi
