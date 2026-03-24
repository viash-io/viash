#!/bin/test

[ -f "$par_output" ] && rm "$par_output"

if [ ${#par_input[@]} -gt 0 ]; then
  for var in "${par_input[@]}"; do
    cat "$var" >> "$par_output"
  done
fi
