#!/bin/bash

## VIASH START
par_input='resources/lines3.txt'
par_double='10.5'
par_output='output.txt'
## VIASH END

echo "Copying $par_input to $par_output"
cp "$par_input" "$par_output"

echo "Adding $par_double to $par_output"
echo "Double: $par_double" >> "$par_output"

exit 0
