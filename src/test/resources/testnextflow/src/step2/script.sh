#!/bin/test


cp "$par_input1" "$par_output1"
cp "$par_input2" "$par_output2"

if [ ! -z "$par_optional" ]; then 
  cat "$par_optional" >> "$par_output1"
  cat "$par_optional" >> "$par_output2"
fi
