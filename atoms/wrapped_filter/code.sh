#!/bin/bash

# PORTASH START
par_input="input.txt"
par_output="output.txt"
par_filter="AString"
PASSTHROUGH="-b" # passthrough
# PORTASH END

echo "## Reading from $par_input"
echo "## Filtering for $par_filter"
echo "## Writing to $par_output"
echo "## Additional parameters? $CMDARGS"
echo ">> Running cat $CMDARGS $par_input | grep $par_filter > $par_output"
cat $PASSTHROUGH "$par_input" | grep "$par_filter" > "$par_output"
