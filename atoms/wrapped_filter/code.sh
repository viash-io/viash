#!/bin/bash

# PORTASH START
par_input="input.txt"
par_output="output.txt"
par_filter="AString"
# PORTASH END

echo "## Reading from $par_input"
echo "## Filtering for $par_filter"
echo "## Writing to $par_output"
echo "## Additional parameters? $CMDARGS"

cat $CMDARGS "$par_input" | grep "$par_filter" > "$par_output"
