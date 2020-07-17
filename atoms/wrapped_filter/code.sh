#!/bin/bash

# VIASH START
par_input="build.sbt"
par_output="output.txt"
par_filter="scala"
par_number_nonblank="true"
# VIASH END

echo "## Reading from $par_input"
echo "## Filtering for $par_filter"
echo "## Writing to $par_output"
echo ">> Running cat $extra_args $par_input | grep $par_filter > $par_output"

if [ $par_number_nonblank == "true" ]; then
  extra_args="$extra_args -b"
fi
cat $extra_args "$par_input" | grep "$par_filter" > "$par_output"
