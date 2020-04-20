#!/bin/bash

# PORTASH START
par_abc=1
par_in="testbash"
# PORTASH END

echo "## This is your abc parameter: $par_abc"
echo "## Now reading from file '$par_in'"

cat "$par_in"
